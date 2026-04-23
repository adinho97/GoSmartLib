import {
  ChangeDetectorRef,
  Component,
  HostListener,
  Input,
  OnDestroy,
  Output,
  EventEmitter,
} from "@angular/core";
import { Location } from "@angular/common";
import { Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { Subscription } from "rxjs";
import { SmartschoolService } from "../services/smartschool.service";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import {
  UserPreferencesService,
  PreferenceKey,
} from "../services/user-preferences.service";
import { ExperienceService, LevelInfo } from "../services/experience.service";
import { BadgeCollectionComponent } from "./badge-collection/badge-collection.component";
import { UiToastService } from "../services/ui-toast.service";
import { inferNameParts, composeFullName } from "../utils/name-utils";

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

  @Output() logoutRequested = new EventEmitter<void>();

  activeSectionInfoKey: keyof typeof this.sectionInfoContent | null = null;
  readonly sectionInfoContent = {
    borrowed: {
      title: "Geleende boeken",
      description:
        "Hier zie je alle boeken die je nu in uitleen hebt. Via deze lijst ga je snel naar details en volg je je deadlines op.",
    },
    highlighted: {
      title: "Klasleeslijst",
      description:
        "Dit zijn de boeken die voor jouw klas of leeromgeving extra in de kijker staan. Gebruik dit overzicht om snel relevant lesmateriaal te vinden.",
    },
    wishlist: {
      title: "Verlanglijst",
      description:
        "Bewaar hier boeken die je later wilt lezen of ontlenen. Je kunt ze vanuit dit blok ook beheren of meldingen aanpassen.",
    },
    favorites: {
      title: "Favoriete boeken",
      description:
        "Deze sectie bevat je persoonlijke favorieten. Handig om snel terug te keren naar boeken die je sterk aanbeveelt of vaker gebruikt.",
    },
    history: {
      title: "Ontleenhistoriek",
      description:
        "In de historiek zie je welke boeken je eerder ontleende. Dit helpt je om gelezen titels te herbekijken en leespatronen te volgen.",
    },
    badges: {
      title: "Badges",
      description:
        "Hier verzamel je badges op basis van je activiteit in de bibliotheek. Ze tonen je voortgang en belonen je lees- en gebruiksgedrag.",
    },
  } as const;

  role = localStorage.getItem("role") || "gebruiker";
  private readonly roleLikeValues = new Set([
    "leerling",
    "leerkracht",
    "bibbeheerder",
    "gebruiker",
  ]);

  dashboardSettings: Record<string, boolean> = {
    showWishlist: true,
    showFavorites: true,
    showReadingHistory: true,
    showBorrowed: true,
    showHighlighted: true,
    showDeadline: true,
  };

  get userName(): string {
    const firstName = (localStorage.getItem("firstName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();
    const composed = composeFullName(firstName, lastName);

    if (composed) {
      return composed;
    }

    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const { firstName: inferredFirst, lastName: inferredLast } = inferNameParts(
      firstName || null,
      lastName || null,
      nameCandidates,
    );
    const inferredComposed = composeFullName(inferredFirst, inferredLast);

    if (inferredComposed) {
      return inferredComposed;
    }

    const candidates = [
      firstName || lastName,
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("username"),
      localStorage.getItem("name"),
    ];

    for (const candidate of candidates) {
      const normalized = this.normalizeDisplayName(candidate);
      if (normalized) {
        return normalized;
      }
    }

    return "Gebruiker";
  }

  private normalizeDisplayName(raw: string | null): string {
    const value = (raw || "").trim();
    if (!value) {
      return "";
    }

    return this.roleLikeValues.has(value.toLowerCase()) ? "" : value;
  }

  settingsOpen = false;

  levelInfo: LevelInfo | null = null;

  wishlistBooks: ProfileBookCard[] = [];
  favoriteBooks: ProfileBookCard[] = [];
  readingHistory: ProfileBookCard[] = [];
  readingHistoryLoading = false;
  borrowedBooks: ProfileBookCard[] = [];
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
  private wishlistChangedSub?: Subscription;
  private favoriteChangedSub?: Subscription;
  private preferencesSub?: Subscription;
  private levelInfoSub?: Subscription;
  badgeToastTimeoutId: any;
  badgeRefreshIntervalId: any;
  visibilityChangeHandler: any;
  windowFocusHandler: any;

  constructor(
    private location: Location,
    private router: Router,
    private smartschoolService: SmartschoolService,
    private http: HttpClient,
    private bookService: BookService,
    private loanService: LoanService,
    private userPreferencesService: UserPreferencesService,
    private cdr: ChangeDetectorRef,
    private experienceService: ExperienceService,
    private uiToastService: UiToastService,
  ) {}

  async ngOnInit() {
    // Subscribe to preferences to keep dashboard settings in sync reactively
    this.preferencesSub = this.userPreferencesService.preferences$.subscribe(
      (prefs) => {
        this.dashboardSettings = {
          showWishlist: prefs["dashboard_showWishlist"] !== false,
          showFavorites: prefs["dashboard_showFavorites"] !== false,
          showReadingHistory: prefs["dashboard_showReadingHistory"] !== false,
          showBorrowed: prefs["dashboard_showBorrowed"] !== false,
          showHighlighted: prefs["dashboard_showHighlighted"] !== false,
          showDeadline: prefs["dashboard_showDeadline"] !== false,
        };
        this.cdr.detectChanges();
      },
    );

    // Subscribe to level info changes
    this.levelInfoSub = this.experienceService.levelInfo$.subscribe((info) => {
      this.levelInfo = info;
    });

    await Promise.all([
      this.loadWishlistBooks(),
      this.loadFavoriteBooks(),
      this.loadReadingHistory(),
      this.loadActiveLoans(),
    ]);

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
  }

  private async loadReadingHistory() {
    this.readingHistoryLoading = true;
    try {
      const history = await this.loanService.getMyLoanHistory();
      const enriched = await this.bookService.enrichBooksWithDetails(
        history.map((loan: any) => ({
          ...loan,
          bookId: loan.bookId,
          titel: loan.bookTitel,
          auteur: loan.bookAuteur || "",
        })),
      );

      this.readingHistory = enriched.map((item: any) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
        genre: item.genre || "",
        taal: item.taal || "",
        paginas: item.paginas || 0,
        loanedDate: item.loanedAt ? new Date(item.loanedAt) : undefined,
      }));
      this.currentReadingHistoryPage = 1;
    } catch {
      this.readingHistory = [];
    } finally {
      this.readingHistoryLoading = false;
    }
  }

  ngOnDestroy() {
    this.wishlistChangedSub?.unsubscribe();
    this.favoriteChangedSub?.unsubscribe();
    this.preferencesSub?.unsubscribe();
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
    this.levelInfoSub?.unsubscribe();
  }
  private async loadActiveLoans() {
    try {
      const activeLoans = await this.loanService.getActiveLoans(
        localStorage.getItem("sub") || "",
      );
      const enriched = await this.bookService.enrichBooksWithDetails(
        activeLoans.map((loan: any) => ({
          ...loan,
          bookId: loan.bookId,
          titel: loan.bookTitel,
        })),
      );

      this.borrowedBooks = enriched.map((item: any) => ({
        id: item.bookId,
        title: item.titel,
        cover: item.cover || "",
        deadline: item.dueDate ? new Date(item.dueDate) : undefined,
        eadline: item.dueDate ? new Date(item.dueDate) : undefined,
        author: item.auteur || "",
        genre: item.genre || "",
        taal: item.taal || "",
        paginas: item.paginas || 0,
      }));
    } catch (error) {
      console.error("Error loading active loans:", error);
      this.borrowedBooks = [];
    }
  }

  private async loadWishlistBooks() {
    this.wishlistLoading = true;
    try {
      const wishlist = await this.bookService.getUserWishlist();
      const enriched = await this.bookService.enrichBooksWithDetails(
        wishlist.map((item: any) => ({
          ...item,
          bookId: item.bookId,
          titel: item.titel,
          auteur: item.auteur,
        })),
      );

      this.wishlistBooks = enriched.map((item: any) => ({
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

  get activeSectionInfo() {
    if (!this.activeSectionInfoKey) {
      return null;
    }
    return this.sectionInfoContent[this.activeSectionInfoKey];
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
      this.uiToastService.success("Boek verwijderd van je verlanglijst.");

      if (this.currentWishlistPage > this.totalWishlistPages) {
        this.currentWishlistPage = this.totalWishlistPages;
      }
    } catch {
      // Keep silent here to avoid noisy alerts on dashboard profile cards.
      this.uiToastService.error("Verlanglijst bijwerken mislukt.");
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
      this.uiToastService.success("Boek verwijderd uit je favorieten.");
    } catch {
      // Keep silent here as well.
      this.uiToastService.error("Favorieten bijwerken mislukt.");
    }
  }

  private async loadFavoriteBooks() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      const enriched = await this.bookService.enrichBooksWithDetails(
        favorites.map((item: any) => ({
          ...item,
          bookId: item.bookId,
          titel: item.titel,
          auteur: item.auteur,
        })),
      );

      this.favoriteBooks = enriched.map((item: any) => ({
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
      this.uiToastService.success(
        nextValue
          ? "Melding ingeschakeld voor dit boek."
          : "Melding uitgeschakeld voor dit boek.",
      );
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
      this.uiToastService.error(this.notificationToggleErrors[book.id]);
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

  async saveDashboardSettings() {
    const currentPrefs = this.userPreferencesService.getSnapshotForLegacyUse();
    const settingsToSave: {
      prop: string;
      key: PreferenceKey;
    }[] = [
      { prop: "showWishlist", key: "dashboard_showWishlist" },
      { prop: "showFavorites", key: "dashboard_showFavorites" },
      { prop: "showReadingHistory", key: "dashboard_showReadingHistory" },
      { prop: "showBorrowed", key: "dashboard_showBorrowed" },
      { prop: "showHighlighted", key: "dashboard_showHighlighted" },
      { prop: "showDeadline", key: "dashboard_showDeadline" },
    ];

    for (const item of settingsToSave) {
      const newValue = this.dashboardSettings[item.prop];
      const oldValue = currentPrefs[item.key];

      // Only save if the value has actually changed to minimize network calls
      if (newValue !== oldValue) {
        await this.userPreferencesService.savePreference(item.key, newValue);
      }
    }
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

  openSectionInfo(key: keyof typeof this.sectionInfoContent) {
    this.activeSectionInfoKey = key;
  }

  closeSectionInfo() {
    this.activeSectionInfoKey = null;
  }

  logout(): void {
    this.logoutRequested.emit();
  }
}
