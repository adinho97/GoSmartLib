import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";

export interface LevelInfo {
  level: number;
  totalExperience: number;
  experienceForCurrentLevel: number;
  experienceRequiredForLevel: number;
  progressPercentage: number;
}

@Injectable({
  providedIn: "root",
})
export class ExperienceService {
  private readonly STORAGE_KEY = "userExperience";
  private readonly CLAIMED_BADGE_REWARDS_KEY = "claimedBadgeRewards";
  private readonly BASE_EXPERIENCE = 100;
  private readonly LEVEL_MULTIPLIER = 1.5; // Each level requires 1.5x more XP

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
  }

  addExperienceForBadge(
    threshold: number,
    category: "loan" | "review",
  ): number {
    const badgeKey = this.getBadgeRewardKey(threshold, category);
    if (this.hasClaimedBadgeReward(badgeKey)) {
      return 0;
    }

    const reward = this.getBadgeExperienceWorth(threshold, category);
    this.markBadgeRewardClaimed(badgeKey);
    this.addExperience(reward);
    return reward;
  }

  getBadgeExperienceWorth(
    threshold: number,
    category: "loan" | "review",
  ): number {
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
    const stored = localStorage.getItem(this.STORAGE_KEY);
    return stored ? parseInt(stored, 10) : 0;
  }

  private getBadgeRewardKey(
    threshold: number,
    category: "loan" | "review",
  ): string {
    return `${category}-${threshold}`;
  }

  private readClaimedBadgeRewards(): Set<string> {
    const stored = localStorage.getItem(this.CLAIMED_BADGE_REWARDS_KEY);
    if (!stored) {
      return new Set();
    }

    try {
      const parsed = JSON.parse(stored) as unknown;
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

  private hasClaimedBadgeReward(badgeKey: string): boolean {
    return this.readClaimedBadgeRewards().has(badgeKey);
  }

  private markBadgeRewardClaimed(badgeKey: string): void {
    const claimedRewards = this.readClaimedBadgeRewards();
    claimedRewards.add(badgeKey);
    localStorage.setItem(
      this.CLAIMED_BADGE_REWARDS_KEY,
      JSON.stringify(Array.from(claimedRewards)),
    );
  }

  private saveExperience(xp: number): void {
    localStorage.setItem(this.STORAGE_KEY, xp.toString());
  }

  private updateLevelInfo(): void {
    this.levelInfoSubject.next(this.calculateLevelInfo());
  }
}
