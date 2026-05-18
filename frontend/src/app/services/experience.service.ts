import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import axios from "axios";

export interface LevelInfo {
  level: number;
  totalExperience: number;
  experienceForCurrentLevel: number;
  experienceRequiredForLevel: number;
  progressPercentage: number;
}

type BadgeCategory = "loan" | "review";

@Injectable({
  providedIn: "root",
})
export class ExperienceService {
  private readonly STORAGE_KEY = "userExperience";
  private readonly CLAIMED_BADGE_REWARDS_KEY = "claimedBadgeRewards";
  private readonly LOAN_XP_SYNCED_COUNT_KEY = "loanXpSyncedCount";
  private readonly REVIEW_XP_SYNCED_COUNT_KEY = "reviewXpSyncedCount";
  private readonly API_URL = "/api/user/experience";
  private readonly backendSyncDebounceMs = 350;
  private readonly BASE_EXPERIENCE = 40;
  private readonly LEVEL_MULTIPLIER = 1.25; // Keep progression gentle so early levels come much faster
  private backendSyncTimeoutId?: ReturnType<typeof setTimeout>;
  private initializedFromBackend = false;
  private activeInitPromise: Promise<void> | null = null;
  private hydratedForUserSub = "";

  private totalExperienceSubject = new BehaviorSubject<number>(
    this.loadExperience(),
  );
  public totalExperience$: Observable<number> =
    this.totalExperienceSubject.asObservable();

  private levelInfoSubject = new BehaviorSubject<LevelInfo>(
    this.calculateLevelInfo(),
  );
  public levelInfo$: Observable<LevelInfo> =
    this.levelInfoSubject.asObservable();

  private readonly REWARDS = {
    loanCreated: 50,
    reviewWritten: 10,
  };

  private readonly BADGE_THRESHOLD_REWARDS: Record<number, number> = {
    1: 20,
    5: 40,
    10: 70,
    20: 120,
    50: 220,
    100: 400,
  };

  constructor() {
    this.updateLevelInfo();
    this.initFromBackend();
  }

  async refreshForCurrentUser(): Promise<void> {
    const userSub = this.getUserSub();
    if (!userSub) {
      return;
    }

    if (this.backendSyncTimeoutId) {
      clearTimeout(this.backendSyncTimeoutId);
      this.backendSyncTimeoutId = undefined;
    }

    // Only skip if we have successfully hydrated for this specific userSub
    // and the service has been fully initialized for them.
    if (
      this.hydratedForUserSub === userSub &&
      this.initializedFromBackend &&
      userSub !== ""
    ) {
      return;
    }

    await this.initFromBackend();
  }

  addExperienceForBadge(threshold: number, category: BadgeCategory): number {
    const badgeKey = this.getBadgeRewardKey(threshold, category);
    if (this.hasClaimedBadgeReward(badgeKey)) {
      return 0;
    }

    const reward = this.getBadgeExperienceWorth(threshold, category);
    this.markBadgeRewardClaimed(badgeKey);
    this.addExperience(reward);
    return reward;
  }

  getBadgeExperienceWorth(threshold: number, category: BadgeCategory): number {
    const reviewReward =
      this.BADGE_THRESHOLD_REWARDS[threshold] ??
      Math.max(20, Math.floor(18 + threshold * 3.2));

    return category === "loan" ? reviewReward * 2 : reviewReward;
  }

  addExperienceForReview(): void {
    this.addExperience(this.REWARDS.reviewWritten);
  }

  addExperienceForLoaningBook(): void {
    this.addExperience(this.REWARDS.loanCreated);
    this.persistLoanXpSyncedCount(this.readLoanXpSyncedCount() + 1);
  }

  reconcileLoanExperienceFromHistory(loanCount: number): void {
    const normalizedLoanCount = Math.max(0, Math.floor(Number(loanCount) || 0));
    const previouslySyncedLoanCount = this.readLoanXpSyncedCount();
    const loanCountDelta = normalizedLoanCount - previouslySyncedLoanCount;

    if (loanCountDelta > 0) {
      this.addExperience(loanCountDelta * this.REWARDS.loanCreated);
    } else if (loanCountDelta < 0) {
      this.removeExperience(
        Math.abs(loanCountDelta) * this.REWARDS.loanCreated,
      );
    }

    this.persistLoanXpSyncedCount(normalizedLoanCount);

    for (const threshold of Object.keys(this.BADGE_THRESHOLD_REWARDS).map(
      Number,
    )) {
      if (normalizedLoanCount >= threshold) {
        this.addExperienceForBadge(threshold, "loan");
      }
    }
  }

  reconcileReviewExperienceFromHistory(reviewCount: number): void {
    const normalizedReviewCount = Math.max(
      0,
      Math.floor(Number(reviewCount) || 0),
    );
    const previouslySyncedReviewCount = this.readReviewXpSyncedCount();
    const delta = normalizedReviewCount - previouslySyncedReviewCount;

    if (delta > 0) {
      this.addExperience(delta * this.REWARDS.reviewWritten);
    } else if (delta < 0) {
      this.removeExperience(Math.abs(delta) * this.REWARDS.reviewWritten);
    }

    this.persistReviewXpSyncedCount(normalizedReviewCount);

    for (const threshold of Object.keys(this.BADGE_THRESHOLD_REWARDS).map(
      Number,
    )) {
      if (normalizedReviewCount >= threshold) {
        this.addExperienceForBadge(threshold, "review");
      }
    }
  }

  removeExperienceForReview(): void {
    this.removeExperience(this.REWARDS.reviewWritten);
  }

  addExperience(amount: number): void {
    const currentXP = this.totalExperienceSubject.value;
    const newXP = currentXP + amount;
    this.totalExperienceSubject.next(newXP);
    this.saveExperience(newXP);
    this.updateLevelInfo();
  }

  removeExperience(amount: number): void {
    const currentXP = this.totalExperienceSubject.value;
    const newXP = Math.max(0, currentXP - amount);
    this.totalExperienceSubject.next(newXP);
    this.saveExperience(newXP);
    this.updateLevelInfo();
  }

  resetExperience(): void {
    this.totalExperienceSubject.next(0);
    this.saveExperience(0);
    this.saveClaimedBadgeRewards(new Set());
    this.updateLevelInfo();
  }

  private calculateLevelInfo(): LevelInfo {
    const totalXP = this.totalExperienceSubject.value;
    let level = 1;
    let xpUsed = 0;

    while (true) {
      const requiredForNextLevel = this.getExperienceRequiredForLevel(level);
      if (xpUsed + requiredForNextLevel > totalXP) {
        break;
      }
      xpUsed += requiredForNextLevel;
      level++;
    }

    const requiredForCurrentLevel = this.getExperienceRequiredForLevel(level);
    const experienceForCurrentLevel = totalXP - xpUsed;
    const progressPercentage = Math.floor(
      (experienceForCurrentLevel / requiredForCurrentLevel) * 100,
    );

    return {
      level,
      totalExperience: totalXP,
      experienceForCurrentLevel,
      experienceRequiredForLevel: requiredForCurrentLevel,
      progressPercentage,
    };
  }

  private getExperienceRequiredForLevel(level: number): number {
    return Math.floor(
      this.BASE_EXPERIENCE * Math.pow(this.LEVEL_MULTIPLIER, level - 1),
    );
  }

  private loadExperience(): number {
    const stored = this.readScopedValueWithLegacyMigration(this.STORAGE_KEY);
    return stored ? parseInt(stored, 10) : 0;
  }

  private getBadgeRewardKey(
    threshold: number,
    category: BadgeCategory,
  ): string {
    return `${category}-${threshold}`;
  }

  private readClaimedBadgeRewards(): Set<string> {
    const stored = this.readScopedValueWithLegacyMigration(
      this.CLAIMED_BADGE_REWARDS_KEY,
    );
    return this.parseClaimedBadgeRewardsJson(stored);
  }

  private hasClaimedBadgeReward(badgeKey: string): boolean {
    return this.readClaimedBadgeRewards().has(badgeKey);
  }

  private markBadgeRewardClaimed(badgeKey: string): void {
    const claimedRewards = this.readClaimedBadgeRewards();
    claimedRewards.add(badgeKey);
    this.saveClaimedBadgeRewards(claimedRewards);
  }

  private saveExperience(xp: number): void {
    this.persistExperienceLocally(xp);
    this.scheduleBackendSync();
  }

  private updateLevelInfo(): void {
    this.levelInfoSubject.next(this.calculateLevelInfo());
  }

  private saveClaimedBadgeRewards(claimedRewards: Set<string>): void {
    this.persistClaimedBadgeRewardsLocally(claimedRewards);
    this.scheduleBackendSync();
  }

  private getUserSub(): string {
    return localStorage.getItem("sub") || localStorage.getItem("userId") || "";
  }

  private getUserScopedKey(baseKey: string): string {
    const userSub = this.getUserSub();
    return userSub ? `${baseKey}:${userSub}` : baseKey;
  }

  private getUserHeaders() {
    const token = localStorage.getItem("smartschoolToken");
    return {
      headers: {
        "X-User-Sub": this.getUserSub(),
        "Cache-Control": "no-cache",
        Pragma: "no-cache",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    };
  }

  private scheduleBackendSync(): void {
    if (!this.initializedFromBackend) {
      return;
    }

    if (!this.getUserSub()) {
      return;
    }

    if (this.backendSyncTimeoutId) {
      clearTimeout(this.backendSyncTimeoutId);
    }

    this.backendSyncTimeoutId = setTimeout(() => {
      this.syncToBackend();
    }, this.backendSyncDebounceMs);
  }

  private async initFromBackend(): Promise<void> {
    if (this.activeInitPromise) {
      return this.activeInitPromise;
    }

    const userSub = this.getUserSub();
    const role = localStorage.getItem("role");
    if (!userSub || role !== "leerling") {
      // If not authenticated or not a student, we don't attempt to fetch from backend.
      // Reset flags to ensure a fresh attempt if user logs in later.
      this.initializedFromBackend = false; // Explicitly set to false if we didn't fetch
      this.hydratedForUserSub = "";
      return;
    }

    this.activeInitPromise = (async () => {
      try {
        const response = await axios.get<{
          totalExperience: number;
          claimedBadgeRewardsJson: string;
        }>(this.API_URL, this.getUserHeaders());

        const totalExperience = Math.max(
          0,
          Number(response.data?.totalExperience || 0),
        );
        const claimedBadgeRewards = this.parseClaimedBadgeRewardsJson(
          response.data?.claimedBadgeRewardsJson,
        );

        this.totalExperienceSubject.next(totalExperience);
        this.updateLevelInfo();
        this.persistExperienceLocally(totalExperience);
        this.persistClaimedBadgeRewardsLocally(claimedBadgeRewards);
        this.hydratedForUserSub = userSub;
        this.initializedFromBackend = true;
      } catch (error: any) {
        if (error?.response?.status === 403) {
          this.hydratedForUserSub = "";
          this.initializedFromBackend = false;
          return;
        }
      } finally {
        this.activeInitPromise = null;
        this.initializedFromBackend = true;
      }
    })();

    return this.activeInitPromise;
  }

  private async syncToBackend(): Promise<void> {
    if (!this.getUserSub()) {
      return;
    }

    try {
      const claimed = this.readClaimedBadgeRewards();
      await axios.patch(
        this.API_URL,
        {
          totalExperience: this.totalExperienceSubject.value,
          claimedBadgeRewardsJson: JSON.stringify(Array.from(claimed)),
        },
        this.getUserHeaders(),
      );
    } catch {
      // Keep local cache and retry on next XP change.
    }
  }

  private parseClaimedBadgeRewardsJson(
    rawJson: string | undefined | null,
  ): Set<string> {
    if (!rawJson) {
      return new Set();
    }

    try {
      const parsed = JSON.parse(rawJson) as unknown;
      if (Array.isArray(parsed)) {
        return new Set(
          parsed.filter((value): value is string => typeof value === "string"),
        );
      }
    } catch {
      return new Set();
    }

    return new Set();
  }

  private readScopedValueWithLegacyMigration(baseKey: string): string | null {
    const scopedKey = this.getUserScopedKey(baseKey);
    const scopedValue = localStorage.getItem(scopedKey);
    if (scopedValue !== null) {
      return scopedValue;
    }

    if (scopedKey === baseKey) {
      return null;
    }

    const legacyValue = localStorage.getItem(baseKey);
    if (legacyValue !== null) {
      localStorage.setItem(scopedKey, legacyValue);
      localStorage.removeItem(baseKey);
      return legacyValue;
    }

    return null;
  }

  private persistExperienceLocally(totalExperience: number): void {
    localStorage.setItem(
      this.getUserScopedKey(this.STORAGE_KEY),
      totalExperience.toString(),
    );
  }

  private persistClaimedBadgeRewardsLocally(claimedRewards: Set<string>): void {
    localStorage.setItem(
      this.getUserScopedKey(this.CLAIMED_BADGE_REWARDS_KEY),
      JSON.stringify(Array.from(claimedRewards)),
    );
  }

  private readLoanXpSyncedCount(): number {
    const rawValue = this.readScopedValueWithLegacyMigration(
      this.LOAN_XP_SYNCED_COUNT_KEY,
    );
    const parsedValue = rawValue ? parseInt(rawValue, 10) : 0;
    return Number.isFinite(parsedValue) ? Math.max(0, parsedValue) : 0;
  }

  private persistLoanXpSyncedCount(loanCount: number): void {
    localStorage.setItem(
      this.getUserScopedKey(this.LOAN_XP_SYNCED_COUNT_KEY),
      Math.max(0, Math.floor(Number(loanCount) || 0)).toString(),
    );
  }

  private readReviewXpSyncedCount(): number {
    const rawValue = this.readScopedValueWithLegacyMigration(
      this.REVIEW_XP_SYNCED_COUNT_KEY,
    );
    const parsedValue = rawValue ? parseInt(rawValue, 10) : 0;
    return Number.isFinite(parsedValue) ? Math.max(0, parsedValue) : 0;
  }

  private persistReviewXpSyncedCount(reviewCount: number): void {
    localStorage.setItem(
      this.getUserScopedKey(this.REVIEW_XP_SYNCED_COUNT_KEY),
      Math.max(0, Math.floor(Number(reviewCount) || 0)).toString(),
    );
  }
}
