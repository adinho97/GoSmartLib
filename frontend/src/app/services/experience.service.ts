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
  private readonly API_URL = "/api/user/experience";
  private readonly backendSyncDebounceMs = 350;
  private readonly BASE_EXPERIENCE = 100;
  private readonly LEVEL_MULTIPLIER = 1.5; // Each level requires 1.5x more XP
  private backendSyncTimeoutId?: ReturnType<typeof setTimeout>;
  private initializedFromBackend = false;
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

    if (this.hydratedForUserSub === userSub && this.initializedFromBackend) {
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
    return {
      headers: {
        "X-User-Sub": this.getUserSub(),
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
    const userSub = this.getUserSub();
    if (!userSub) {
      this.initializedFromBackend = true;
      this.hydratedForUserSub = "";
      return;
    }

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
    } catch {
      // Keep local fallback data when backend is unreachable.
    } finally {
      this.initializedFromBackend = true;
    }
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
}
