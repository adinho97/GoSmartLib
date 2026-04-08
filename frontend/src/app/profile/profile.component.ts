import {
  Component,
  HostListener,
  Input,
  ChangeDetectorRef,
  OnDestroy,
} from "@angular/core";
import { Location } from "@angular/common";
import { Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { Subscription } from "rxjs";
import { SmartschoolService } from "../services/smartschool.service";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";

type ProfileBookCard = {
  title: string;
  author?: string;
  cover: string;
  id: number;
  deadline?: Date;
  loanedDate?: Date;
  wishlistId?: number;
  notificationEnabled?: boolean;
  availableCopies?: number;
  totalCopies?: number;
  genre?: string;
  taal?: string;
  paginas?: number;
};

type BadgeCategory = "loan" | "review";

type ProfileBadge = {
  id: string;
  title: string;
  category: BadgeCategory;
  threshold: number;
  current: number;
  unlocked: boolean;
};

@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.css"],
  standalone: false,
})
export class ProfileComponent {
  @Input() embedded = false;
  @Input() showHero = true;
  @Input() showSections = true;

  role = localStorage.getItem("role") || "gebruiker";

  dashboardSettings = {
    showWishlist: true,
    showFavorites: true,
    showReadingHistory: true,
    showBorrowed: true,
    showHighlighted: true,
    showDeadline: true,
  };

  userName = localStorage.getItem("userName") || "Gebruiker";

  settingsOpen = false;

  wishlistBooks: ProfileBookCard[] = [];
  favoriteBooks: ProfileBookCard[] = [];
  readingHistory: ProfileBookCard[] = [];
  readingHistoryLoading = false;
  borrowedBooks: ProfileBookCard[] = [
    { title: "Book Four", deadline: new Date(), cover: "", id: 4 },
  ];
  readingList: ProfileBookCard[] = [
    { title: "Book Five", author: "Author C", cover: "", id: 5 },
  ];
  wishlistLoading = false;
  notificationToggleErrors: Record<number, string> = {};
  readonly wishlistPageSize = 5;
  readonly favoritePageSize = 5;
  readonly readingHistoryPageSize = 5;
  currentWishlistPage = 1;
  currentFavoritePage = 1;
  currentReadingHistoryPage = 1;
  reviewCount = 0;
  badges: ProfileBadge[] = [];
  badgeToastVisible = false;
  badgeToastTitle = "";
  badgeToastMessage = "";
  private readonly badgeMilestones = [1, 5, 10, 20, 50, 100];
  private unlockedBadgeIds = new Set<string>();
  private suppressBadgeToast = true;
  private badgeToastTimeoutId?: ReturnType<typeof setTimeout>;
  private badgeRefreshIntervalId?: ReturnType<typeof setInterval>;
  private visibilityChangeHandler?: () => void;
  private windowFocusHandler?: () => void;
  private wishlistChangedSub?: Subscription;
  private favoriteChangedSub?: Subscription;

  constructor(
    private location: Location,
    private router: Router,
    private smartschoolService: SmartschoolService,
    private http: HttpClient,
    private bookService: BookService,
    private loanService: LoanService,
    private cdr: ChangeDetectorRef,
  ) {}

  async ngOnInit() {
    const saved = localStorage.getItem("dashboardSettings");
    if (saved) {
      this.dashboardSettings = JSON.parse(saved);
    }

    await Promise.all([
      this.loadWishlistBooks(),
      this.loadFavoriteBooks(),
      this.loadReadingHistory(),
      this.loadReviewCount(),
    ]);
    this.rebuildBadges();
    this.suppressBadgeToast = false;
    this.cdr.detectChanges();

    if (this.showSections) {
      this.wishlistChangedSub = this.bookService.wishlistChanged$.subscribe(
        () => {
          this.loadWishlistBooks();
        },
      );
      this.favoriteChangedSub = this.bookService.favoriteChanged$.subscribe(
        () => {
          this.loadFavoriteBooks();
        },
      );
    }

    // Refresh badges periodically and on visibility change
    this.startBadgeRefreshInterval();
    this.setupVisibilityListener();
  }

  private setupVisibilityListener() {
    this.visibilityChangeHandler = () => {
      console.log("Visibility changed, document.hidden:", document.hidden);
      if (!document.hidden) {
        // Page became visible, refresh badges immediately
        console.log("Page is now visible, refreshing badges...");
        this.refreshBadges();
      }
    };

    this.windowFocusHandler = () => {
      // Window regained focus, refresh badges immediately
      console.log("Window regained focus, refreshing badges...");
      this.refreshBadges();
    };

    document.addEventListener("visibilitychange", this.visibilityChangeHandler);
    window.addEventListener("focus", this.windowFocusHandler);
    console.log("Visibility listeners set up");
  }

  private startBadgeRefreshInterval() {
    console.log("Starting badge refresh interval...");
    this.badgeRefreshIntervalId = setInterval(() => {
      this.refreshBadges();
    }, 1000); // Check every 1 second
  }

  async refreshBadges() {
    try {
      const newReviewCount = await this.bookService.getMyReviewCount();
      console.log(
        "Refreshing badges - reviewCount was:",
        this.reviewCount,
        "new:",
        newReviewCount,
      );
      // Only update if review count changed (that's what earns badges on the detail page)
      if (newReviewCount !== this.reviewCount) {
        this.reviewCount = newReviewCount;
        console.log("Review count changed! Rebuilding badges...");
        this.rebuildBadges();
        this.cdr.detectChanges();
      }
    } catch (error) {
      console.error("Error refreshing badges:", error);
    }
  }

  private async loadReadingHistory() {
    this.readingHistoryLoading = true;
    try {
      const history = await this.loanService.getMyLoanHistory();
      this.readingHistory = history.map((loan: any) => ({
        id: loan.bookId,
        title: loan.bookTitel,
        author: loan.bookAuteur || "",
        cover: loan.bookCover || "",
        genre: loan.bookGenre || "",
        taal: loan.bookTaal || "",
        paginas: loan.bookPaginas || 0,
        loanedDate: loan.loanedAt ? new Date(loan.loanedAt) : undefined,
      }));
      this.currentReadingHistoryPage = 1;
    } catch {
      this.readingHistory = [];
    } finally {
      this.readingHistoryLoading = false;
      this.rebuildBadges();
    }
  }

  private async loadReviewCount() {
    try {
      this.reviewCount = await this.bookService.getMyReviewCount();
    } catch {
      this.reviewCount = 0;
    } finally {
      this.rebuildBadges();
    }
  }

  private rebuildBadges() {
    const loanCount = this.readingHistory.length;
    const reviewCount = this.reviewCount;

    const loanBadges: ProfileBadge[] = this.badgeMilestones.map(
      (threshold) => ({
        id: `loan-${threshold}`,
        title:
          threshold === 1 ? "Ontleen een boek" : `Ontleen ${threshold} boeken`,
        category: "loan",
        threshold,
        current: loanCount,
        unlocked: loanCount >= threshold,
      }),
    );

    const reviewBadges: ProfileBadge[] = this.badgeMilestones.map(
      (threshold) => ({
        id: `review-${threshold}`,
        title:
          threshold === 1 ? "Plaats een review" : `Plaats ${threshold} reviews`,
        category: "review",
        threshold,
        current: reviewCount,
        unlocked: reviewCount >= threshold,
      }),
    );

    this.badges = [...loanBadges, ...reviewBadges];

    const nextUnlockedIds = new Set(
      this.badges.filter((badge) => badge.unlocked).map((badge) => badge.id),
    );

    console.log(
      "rebuildBadges - suppressBadgeToast:",
      this.suppressBadgeToast,
      "unlockedBadgeIds:",
      Array.from(this.unlockedBadgeIds),
    );

    if (!this.suppressBadgeToast) {
      const newlyUnlockedBadges = this.badges.filter(
        (badge) => badge.unlocked && !this.unlockedBadgeIds.has(badge.id),
      );
      console.log(
        "newlyUnlockedBadges:",
        newlyUnlockedBadges.map((b) => b.id),
      );
      if (newlyUnlockedBadges.length > 0) {
        this.showBadgeToast(newlyUnlockedBadges);
      }
    }

    this.unlockedBadgeIds = nextUnlockedIds;
  }

  private showBadgeToast(newlyUnlockedBadges: ProfileBadge[]) {
    const firstBadge = newlyUnlockedBadges[0];
    this.badgeToastTitle = `Nieuwe badge ontgrendeld: ${firstBadge.title}`;
    this.badgeToastMessage =
      newlyUnlockedBadges.length > 1
        ? `En nog ${newlyUnlockedBadges.length - 1} andere badge(s)!`
        : "Goed bezig, hou je streak vol.";
    console.log("🎉 Showing badge toast:", this.badgeToastTitle);
    this.badgeToastVisible = true;
    this.cdr.detectChanges(); // Trigger change detection immediately

    if (this.badgeToastTimeoutId) {
      clearTimeout(this.badgeToastTimeoutId);
    }
    this.badgeToastTimeoutId = setTimeout(() => {
      this.badgeToastVisible = false;
      this.cdr.detectChanges(); // Trigger change detection for fade out
    }, 4200);
  }

  dismissBadgeToast() {
    this.badgeToastVisible = false;
    if (this.badgeToastTimeoutId) {
      clearTimeout(this.badgeToastTimeoutId);
      this.badgeToastTimeoutId = undefined;
    }
  }

  get unlockedBadgesCount(): number {
    return this.badges.filter((badge) => badge.unlocked).length;
  }

  get totalBadgesCount(): number {
    return this.badges.length;
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
    this.wishlistChangedSub?.unsubscribe();
    this.favoriteChangedSub?.unsubscribe();
    if (this.badgeToastTimeoutId) {
      clearTimeout(this.badgeToastTimeoutId);
    }
    if (this.badgeRefreshIntervalId) {
      clearInterval(this.badgeRefreshIntervalId);
    }
    if (this.visibilityChangeHandler) {
      document.removeEventListener(
        "visibilitychange",
        this.visibilityChangeHandler,
      );
    }
    if (this.windowFocusHandler) {
      window.removeEventListener("focus", this.windowFocusHandler);
    }
  }

  private async loadWishlistBooks() {
    this.wishlistLoading = true;
    try {
      const wishlist = await this.bookService.getUserWishlist();
      this.wishlistBooks = wishlist.map((item: any) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
        genre: item.genre || "",
        taal: item.taal || "",
        paginas: item.paginas || 0,
        wishlistId: item.id,
        notificationEnabled: item.notificationEnabled ?? false,
        availableCopies: item.availableCopies ?? 0,
        totalCopies: item.totalCopies ?? 0,
      }));
      this.currentWishlistPage = 1;
    } catch {
      this.wishlistBooks = [];
    } finally {
      this.wishlistLoading = false;
    }
  }

  get totalWishlistPages(): number {
    return Math.max(
      1,
      Math.ceil(this.wishlistBooks.length / this.wishlistPageSize),
    );
  }

  get wishlistPageNumbers(): number[] {
    return Array.from({ length: this.totalWishlistPages }, (_, i) => i + 1);
  }

  get pagedWishlistBooks(): ProfileBookCard[] {
    const start = (this.currentWishlistPage - 1) * this.wishlistPageSize;
    return this.wishlistBooks.slice(start, start + this.wishlistPageSize);
  }

  goToWishlistPage(page: number) {
    this.currentWishlistPage = page;
  }

  get totalFavoritePages(): number {
    return Math.max(
      1,
      Math.ceil(this.favoriteBooks.length / this.favoritePageSize),
    );
  }

  get favoritePageNumbers(): number[] {
    return Array.from({ length: this.totalFavoritePages }, (_, i) => i + 1);
  }

  get pagedFavoriteBooks(): ProfileBookCard[] {
    const start = (this.currentFavoritePage - 1) * this.favoritePageSize;
    return this.favoriteBooks.slice(start, start + this.favoritePageSize);
  }

  get favoritesCount(): number {
    return this.favoriteBooks.length;
  }

  get wishlistCount(): number {
    return this.wishlistBooks.length;
  }

  get highlightedBooksCount(): number {
    return this.readingList.length;
  }

  goToFavoritePage(page: number) {
    this.currentFavoritePage = page;
  }

  get totalReadingHistoryPages(): number {
    return Math.max(
      1,
      Math.ceil(this.readingHistory.length / this.readingHistoryPageSize),
    );
  }

  get readingHistoryPageNumbers(): number[] {
    return Array.from(
      { length: this.totalReadingHistoryPages },
      (_, i) => i + 1,
    );
  }

  get pagedReadingHistory(): ProfileBookCard[] {
    const start =
      (this.currentReadingHistoryPage - 1) * this.readingHistoryPageSize;
    return this.readingHistory.slice(
      start,
      start + this.readingHistoryPageSize,
    );
  }

  goToReadingHistoryPage(page: number) {
    this.currentReadingHistoryPage = page;
  }

  async removeFromWishlist(event: MouseEvent | null, bookId: number) {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }

    try {
      await this.bookService.removeFromWishlist(bookId);
      this.wishlistBooks = this.wishlistBooks.filter(
        (book) => book.id !== bookId,
      );

      if (this.currentWishlistPage > this.totalWishlistPages) {
        this.currentWishlistPage = this.totalWishlistPages;
      }
    } catch {
      // Keep silent here to avoid noisy alerts on dashboard profile cards.
    }
  }

  async removeFromFavorites(event: MouseEvent | null, bookId: number) {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }

    try {
      await this.bookService.removeFromFavorites(bookId);
      this.favoriteBooks = this.favoriteBooks.filter(
        (book) => book.id !== bookId,
      );
    } catch {
      // Keep silent here as well.
    }
  }

  private async loadFavoriteBooks() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      this.favoriteBooks = favorites.map((item: any) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
        genre: item.genre || "",
        taal: item.taal || "",
        paginas: item.paginas || 0,
      }));
    } catch {
      this.favoriteBooks = [];
    } finally {
      this.currentFavoritePage = 1;
    }
  }

  getAvailableCopiesCount(book: ProfileBookCard): number {
    return book.availableCopies ?? 0;
  }

  getTotalCopiesCount(book: ProfileBookCard): number {
    return book.totalCopies ?? 0;
  }

  isBookUnavailable(book: ProfileBookCard): boolean {
    return (
      this.getAvailableCopiesCount(book) === 0 &&
      this.getTotalCopiesCount(book) > 0
    );
  }

  async toggleNotification(event: MouseEvent | null, book: ProfileBookCard) {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }

    if (!book.wishlistId) return;

    this.notificationToggleErrors[book.id] = "";

    const previousValue = book.notificationEnabled ?? false;
    const nextValue = !previousValue;
    book.notificationEnabled = nextValue;

    try {
      const updatedWishlist = await this.bookService.updateWishlistNotification(
        book.wishlistId,
        nextValue,
      );
      book.notificationEnabled =
        updatedWishlist.notificationEnabled ?? nextValue;
      this.notificationToggleErrors[book.id] = "";
    } catch (error) {
      book.notificationEnabled = previousValue;
      console.error("Failed to toggle notification:", error);
      const status = (error as any)?.response?.status;
      if (status === 400) {
        this.notificationToggleErrors[book.id] =
          "Kan niet aanzetten: boek is momenteel beschikbaar.";
      } else if (status === 401) {
        this.notificationToggleErrors[book.id] =
          "Niet ingelogd. Herlaad en probeer opnieuw.";
      } else {
        this.notificationToggleErrors[book.id] =
          "Melding aanpassen mislukt. Probeer opnieuw.";
      }
    }
  }

  // New event handlers for recommendation-card component
  async onWishlistRemove(bookId: number) {
    await this.removeFromWishlist(null, bookId);
  }

  async onFavoritesRemove(bookId: number) {
    await this.removeFromFavorites(null, bookId);
  }

  goBack() {
    this.location.back();
  }

  toggleSettings() {
    this.settingsOpen = !this.settingsOpen;
  }

  saveDashboardSettings() {
    localStorage.setItem(
      "dashboardSettings",
      JSON.stringify(this.dashboardSettings),
    );
    this.settingsOpen = false;
  }

  @HostListener("document:click", ["$event"])
  clickOutside(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest(".settings-dropdown")) {
      this.settingsOpen = false;
    }
  }

  goToDetail(bookId: number, event?: MouseEvent | null) {
    const target = event?.target as HTMLElement | null;
    if (target?.closest("button")) {
      return;
    }
    this.router.navigate(["/detail", bookId]);
  }

  scrollToSection(sectionId: string) {
    const section = document.getElementById(sectionId);
    if (!section) {
      return;
    }

    section.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  testSmartschoolMessage(): void {
    this.smartschoolService
      .sendMessage(
        "Testbericht van GoSmartLib",
        "Dit is een testbericht verstuurd vanuit je profielpagina.",
      )
      .subscribe({
        next: () =>
          alert("Bericht succesvol verzonden! Check je Smartschool berichten."),
        error: (err) => {
          console.error(err);
          alert("Er ging iets mis bij het versturen van het bericht.");
        },
      });
  }
}
