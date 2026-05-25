import { Component, HostListener, OnDestroy, OnInit } from "@angular/core";
import { BookService } from "../../services/book.service";
import { LoanService } from "../../services/loan.service";
import {
  BadgeIconKey,
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
  selectedBadgeId: string | null = null;

  private readonly badgeMilestones = [1, 5, 10, 20, 50, 100];
  private readonly badgeRefreshIntervalMs = 60000;
  private readonly manualRefreshCooldownMs = 10000;
  private badgeRefreshIntervalId?: ReturnType<typeof setInterval>;
  private lastKnownCounts: BadgeCounts | null = null;
  private readonly badgeStateStorageKey = "profileBadgeCounts";
  private isRefreshing = false;
  private lastRefreshAt = 0;

  constructor(
    private bookService: BookService,
    private loanService: LoanService,
    private badgeNotificationService: BadgeNotificationService,
    private experienceService: ExperienceService,
  ) {}

  async ngOnInit() {
    const previousCounts = this.readStoredCounts();
    const currentCounts = await this.loadCounts();
    this.experienceService.reconcileLoanExperienceFromHistory(
      currentCounts.loanCount,
    );
    this.experienceService.reconcileReviewExperienceFromHistory(
      currentCounts.reviewCount,
    );
    this.lastKnownCounts = currentCounts;
    this.rebuildBadges(currentCounts, previousCounts);
    this.storeCounts(currentCounts);
    this.loading = false;
    this.startBadgeRefreshInterval();
  }

  @HostListener("document:visibilitychange")
  onVisibilityChange() {
    if (!document.hidden) {
      this.refreshBadges(true);
    }
  }

  @HostListener("window:focus")
  onWindowFocus() {
    this.refreshBadges(true);
  }

  @HostListener("document:click")
  onDocumentClick() {
    this.selectedBadgeId = null;
  }

  private startBadgeRefreshInterval() {
    this.badgeRefreshIntervalId = setInterval(() => {
      this.refreshBadges();
    }, this.badgeRefreshIntervalMs);
  }

  private async loadCounts(): Promise<BadgeCounts> {
    const [loanCount, reviewCount] = await Promise.all([
      this.fetchLoanCount(),
      this.fetchReviewCount(),
    ]);

    return { loanCount, reviewCount };
  }

  async refreshBadges(force = false) {
    const now = Date.now();

    if (this.isRefreshing) {
      return;
    }

    if (!force && now - this.lastRefreshAt < this.manualRefreshCooldownMs) {
      return;
    }

    this.isRefreshing = true;

    try {
      const currentCounts = await this.loadCounts();
      this.experienceService.reconcileLoanExperienceFromHistory(
        currentCounts.loanCount,
      );
      this.experienceService.reconcileReviewExperienceFromHistory(
        currentCounts.reviewCount,
      );
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
    } finally {
      this.lastRefreshAt = Date.now();
      this.isRefreshing = false;
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
    this.badges = [
      ...this.buildBadgesForCategory("loan", currentCounts.loanCount),
      ...this.buildBadgesForCategory("review", currentCounts.reviewCount),
    ];

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

  private buildBadgesForCategory(
    category: BadgeCategory,
    currentCount: number,
  ): ProfileBadge[] {
    return this.badgeMilestones.map((threshold) => ({
      id: `${category}-${threshold}`,
      title: this.getBadgeTitle(category, threshold),
      category,
      threshold,
      current: currentCount,
      unlocked: currentCount >= threshold,
    }));
  }

  private getBadgeTitle(category: BadgeCategory, threshold: number): string {
    if (category === "loan") {
      return threshold === 1
        ? "Ontleen een boek"
        : `Ontleen ${threshold} boeken`;
    }

    return threshold === 1
      ? "Plaats een review"
      : `Plaats ${threshold} reviews`;
  }

  private showBadgeToast(newlyUnlockedBadges: ProfileBadge[]) {
    const badge = newlyUnlockedBadges[0];
    const toastPayload: BadgeUnlocked = {
      title: badge.title,
      iconKey: this.getBadgeIconKey(badge),
      category: badge.category,
    };

    this.badgeNotificationService.showBadgeNotification(toastPayload);

    // Award dynamic experience for each newly unlocked badge.
    newlyUnlockedBadges.forEach((unlockedBadge) => {
      this.experienceService.addExperienceForBadge(
        unlockedBadge.threshold,
        unlockedBadge.category,
      );
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

  getBadgeIconKey(badge: ProfileBadge): BadgeIconKey {
    if (badge.category === "loan") {
      if (badge.threshold >= 100) return "leg";
      if (badge.threshold >= 50) return "bib";
      if (badge.threshold >= 20) return "klas";
      if (badge.threshold >= 10) return "nacht";
      if (badge.threshold >= 5) return "rij";
      return "boek";
    }

    if (badge.threshold >= 100) return "master";
    if (badge.threshold >= 50) return "lit";
    if (badge.threshold >= 20) return "schr";
    if (badge.threshold >= 10) return "crit";
    if (badge.threshold >= 5) return "recens";
    return "rev";
  }

  toggleBadgeMenu(badge: ProfileBadge, event: Event) {
    event.stopPropagation();
    this.selectedBadgeId = this.selectedBadgeId === badge.id ? null : badge.id;
  }

  stopMenuClick(event: Event) {
    event.stopPropagation();
  }

  getBadgeExperienceWorth(badge: ProfileBadge): number {
    return this.experienceService.getBadgeExperienceWorth(
      badge.threshold,
      badge.category,
    );
  }

  ngOnDestroy() {
    if (this.badgeRefreshIntervalId) {
      clearInterval(this.badgeRefreshIntervalId);
    }
  }
}
