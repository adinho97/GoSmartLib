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

  // Experience rewards for different activities
  private readonly REWARDS = {
    reviewWritten: 40,
  };

  // Dynamic XP scaling for badge milestones.
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

  /**
   * Add experience for badge unlock
   */
  addExperienceForBadge(
    threshold: number,
    category: "loan" | "review",
  ): number {
    const reward = this.getBadgeExperienceWorth(threshold, category);
    this.addExperience(reward);
    return reward;
  }

  /**
   * Get XP reward for a badge milestone.
   */
  getBadgeExperienceWorth(
    threshold: number,
    category: "loan" | "review",
  ): number {
    const reviewReward =
      this.BADGE_THRESHOLD_REWARDS[threshold] ??
      Math.max(20, Math.floor(18 + threshold * 3.2));

    // Loan badges are worth exactly double review badges.
    return category === "loan" ? reviewReward * 2 : reviewReward;
  }

  /**
   * Add experience for writing a review
   */
  addExperienceForReview(): void {
    this.addExperience(this.REWARDS.reviewWritten);
  }

  /**
   * Add custom amount of experience
   */
  addExperience(amount: number): void {
    const currentXP = this.totalExperienceSubject.value;
    const newXP = currentXP + amount;
    this.totalExperienceSubject.next(newXP);
    this.saveExperience(newXP);
    this.updateLevelInfo();
  }

  /**
   * Reset experience (for testing or account reset)
   */
  resetExperience(): void {
    this.totalExperienceSubject.next(0);
    this.saveExperience(0);
    this.updateLevelInfo();
  }

  /**
   * Calculate level info based on total experience
   */
  private calculateLevelInfo(): LevelInfo {
    const totalXP = this.totalExperienceSubject.value;
    let level = 1;
    let xpUsed = 0;

    // Calculate level by accumulating required XP per level
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

  /**
   * Get experience required to go from current level to next
   */
  private getExperienceRequiredForLevel(level: number): number {
    return Math.floor(
      this.BASE_EXPERIENCE * Math.pow(this.LEVEL_MULTIPLIER, level - 1),
    );
  }

  /**
   * Load experience from localStorage
   */
  private loadExperience(): number {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    return stored ? parseInt(stored, 10) : 0;
  }

  /**
   * Save experience to localStorage
   */
  private saveExperience(xp: number): void {
    localStorage.setItem(this.STORAGE_KEY, xp.toString());
  }

  /**
   * Update level info and notify subscribers
   */
  private updateLevelInfo(): void {
    this.levelInfoSubject.next(this.calculateLevelInfo());
  }
}
