import { Component, HostListener, OnDestroy, OnInit } from "@angular/core";
import { BookService } from "../../services/book.service";
import { LoanService } from "../../services/loan.service";
import {
  BadgeNotificationService,
  BadgeUnlocked,
} from "../../services/badge-notification.service";
import { ExperienceService } from "../../services/experience.service";

type BadgeCategory = "loan" | "review";

type ProfileBadge = {
  id: string;
  title: string;
  category: BadgeCategory;
  threshold: number;
  current: number;
  unlocked: boolean;
};

type BadgeCounts = {
  loanCount: number;
  reviewCount: number;
};

@Component({
  selector: "app-badge-collection",
  templateUrl: "./badge-collection.component.html",
  styleUrls: ["./badge-collection.component.css"],
  standalone: true,
})
export class BadgeCollectionComponent implements OnInit, OnDestroy {
  badges: ProfileBadge[] = [];
  loading = true;

  private readonly badgeMilestones = [1, 5, 10, 20, 50, 100];
  private badgeRefreshIntervalId?: ReturnType<typeof setInterval>;
  private lastKnownCounts: BadgeCounts | null = null;
  private readonly badgeStateStorageKey = "profileBadgeCounts";

  constructor(
    private bookService: BookService,
    private loanService: LoanService,
    private badgeNotificationService: BadgeNotificationService,
    private experienceService: ExperienceService,
  ) {}

  async ngOnInit() {
    const previousCounts = this.readStoredCounts();
    const currentCounts = await this.loadCounts();
    this.lastKnownCounts = currentCounts;
    this.rebuildBadges(currentCounts, previousCounts);
    this.storeCounts(currentCounts);
    this.loading = false;
    this.startBadgeRefreshInterval();
  }

  @HostListener("document:visibilitychange")
  onVisibilityChange() {
    if (!document.hidden) {
      this.refreshBadges();
    }
  }

  @HostListener("window:focus")
  onWindowFocus() {
    this.refreshBadges();
  }

  private startBadgeRefreshInterval() {
    this.badgeRefreshIntervalId = setInterval(() => {
      this.refreshBadges();
    }, 1000);
  }

  private async loadCounts(): Promise<BadgeCounts> {
    const [loanCount, reviewCount] = await Promise.all([
      this.fetchLoanCount(),
      this.fetchReviewCount(),
    ]);

    return { loanCount, reviewCount };
  }

  async refreshBadges() {
    try {
      const currentCounts = await this.loadCounts();
      const previousCounts = this.lastKnownCounts;

      if (
        !previousCounts ||
        currentCounts.loanCount !== previousCounts.loanCount ||
        currentCounts.reviewCount !== previousCounts.reviewCount
      ) {
        this.rebuildBadges(currentCounts, previousCounts);
        this.lastKnownCounts = currentCounts;
        this.storeCounts(currentCounts);
      }
    } catch {
      // Ignore refresh failures.
    }
  }

  private async fetchLoanCount(): Promise<number> {
    try {
      const history = await this.loanService.getMyLoanHistory();
      return history.length;
    } catch {
      return this.lastKnownCounts?.loanCount ?? 0;
    }
  }

  private async fetchReviewCount(): Promise<number> {
    try {
      return await this.bookService.getMyReviewCount();
    } catch {
      return this.lastKnownCounts?.reviewCount ?? 0;
    }
  }

  private rebuildBadges(
    currentCounts: BadgeCounts,
    previousCounts: BadgeCounts | null,
  ) {
    const loanBadges: ProfileBadge[] = this.badgeMilestones.map(
      (threshold) => ({
        id: `loan-${threshold}`,
        title:
          threshold === 1 ? "Ontleen een boek" : `Ontleen ${threshold} boeken`,
        category: "loan",
        threshold,
        current: currentCounts.loanCount,
        unlocked: currentCounts.loanCount >= threshold,
      }),
    );

    const reviewBadges: ProfileBadge[] = this.badgeMilestones.map(
      (threshold) => ({
        id: `review-${threshold}`,
        title:
          threshold === 1 ? "Plaats een review" : `Plaats ${threshold} reviews`,
        category: "review",
        threshold,
        current: currentCounts.reviewCount,
        unlocked: currentCounts.reviewCount >= threshold,
      }),
    );

    this.badges = [...loanBadges, ...reviewBadges];

    if (previousCounts) {
      const newlyUnlockedBadges = this.badges.filter((badge) => {
        const previousCount =
          badge.category === "loan"
            ? previousCounts.loanCount
            : previousCounts.reviewCount;

        return previousCount < badge.threshold && badge.unlocked;
      });

      if (newlyUnlockedBadges.length > 0) {
        this.showBadgeToast(newlyUnlockedBadges);
      }
    }
  }

  private showBadgeToast(newlyUnlockedBadges: ProfileBadge[]) {
    const badge = newlyUnlockedBadges[0];
    const toastPayload: BadgeUnlocked = {
      title: badge.title,
      icon: this.getBadgeIcon(badge),
      category: badge.category,
    };

    this.badgeNotificationService.showBadgeNotification(toastPayload);

    // Award experience for each newly unlocked badge
    newlyUnlockedBadges.forEach(() => {
      this.experienceService.addExperienceForBadge();
    });
  }

  get unlockedBadgesCount(): number {
    return this.badges.filter((badge) => badge.unlocked).length;
  }

  get totalBadgesCount(): number {
    return this.badges.length;
  }

  private readStoredCounts(): BadgeCounts | null {
    const rawValue = localStorage.getItem(this.badgeStateStorageKey);

    if (!rawValue) {
      return null;
    }

    try {
      const parsedValue = JSON.parse(rawValue) as Partial<BadgeCounts>;
      if (
        typeof parsedValue.loanCount === "number" &&
        typeof parsedValue.reviewCount === "number"
      ) {
        return {
          loanCount: parsedValue.loanCount,
          reviewCount: parsedValue.reviewCount,
        };
      }
    } catch {
      return null;
    }

    return null;
  }

  private storeCounts(counts: BadgeCounts) {
    localStorage.setItem(this.badgeStateStorageKey, JSON.stringify(counts));
  }

  getBadgeProgressText(badge: ProfileBadge): string {
    if (badge.unlocked) {
      return `Behaald (${badge.current}/${badge.threshold})`;
    }

    return `Voortgang ${Math.min(badge.current, badge.threshold)}/${badge.threshold}`;
  }

  getBadgeCategoryLabel(category: BadgeCategory): string {
    return category === "loan" ? "Uitleen" : "Reviews";
  }

  getBadgeIcon(badge: ProfileBadge): string {
    if (badge.category === "loan") {
      if (badge.threshold >= 100) return "🏛️";
      if (badge.threshold >= 50) return "🏆";
      if (badge.threshold >= 20) return "📚";
      return "📘";
    }

    if (badge.threshold >= 100) return "👑";
    if (badge.threshold >= 50) return "🌟";
    if (badge.threshold >= 20) return "📝";
    return "✍️";
  }

  ngOnDestroy() {
    if (this.badgeRefreshIntervalId) {
      clearInterval(this.badgeRefreshIntervalId);
    }
  }
}
