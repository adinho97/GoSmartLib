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
import { SchoolService } from "../services/school.service";
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
    classReadingList: {
      title: "Klasleeslijst",
      description:
        "Dit zijn de boeken die voor jouw klas of leeromgeving extra in de kijker staan. Gebruik dit overzicht om snel relevant lesmateriaal te vinden.",
    },
    highlighted: {
      title: "In de kijker",
      description:
        "Boeken die door de bibliothecaris zijn gemarkeerd als aanbevolen of belangrijk voor de hele school.",
    },
    wishlist: {
      title: "Verlanglijst",
      description:
        "Bewaar hier boeken die je later wilt lezen of ontlenen. Je kunt ze vanuit dit blok ook beheren of meldingen aanpassen.",
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
    showReadingHistory: true,
    showBorrowed: true,
    showClassReadingList: true, // Renamed from showHighlighted
    showDeadline: true,
  };

  get userName(): string {
    const userNameKey = (localStorage.getItem("userName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();

    if (
      userNameKey &&
      lastName &&
      userNameKey.toLowerCase() !== lastName.toLowerCase()
    ) {
      return composeFullName(userNameKey, lastName);
    }

    const firstName = (
      localStorage.getItem("firstName") ||
      userNameKey ||
      ""
    ).trim();
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
      firstName,
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("username"),
      localStorage.getItem("name"),
      lastName,
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
  isSavingSettings = false;

  levelInfo: LevelInfo | null = null;

  wishlistBooks: ProfileBookCard[] = [];
  readingHistory: ProfileBookCard[] = [];
  readingHistoryLoading = false;
  borrowedBooks: ProfileBookCard[] = [];
  classReadingList: ProfileBookCard[] = []; // Renamed from readingList
  classReadingListLoading = false; // Renamed from readingListLoading
  highlightedBooks: ProfileBookCard[] = []; // New for highlighted books
  highlightedLoading = false; // New for highlighted books loading state
  wishlistLoading = false;
  notificationToggleErrors: Record<number, string> = {};
  readonly wishlistPageSize = 5;
  readonly readingHistoryPageSize = 5;
  currentWishlistPage = 1;
  currentReadingHistoryPage = 1;
  private wishlistChangedSub?: Subscription;
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
    private schoolService: SchoolService,
    private uiToastService: UiToastService,
  ) {}

  async ngOnInit() {
    // Subscribe to preferences to keep dashboard settings in sync reactively
    this.preferencesSub = this.userPreferencesService.preferences$.subscribe(
      (prefs) => {
        this.dashboardSettings = {
          showWishlist: prefs["dashboard_showWishlist"] !== false,
          showReadingHistory: prefs["dashboard_showReadingHistory"] !== false,
          showBorrowed: prefs["dashboard_showBorrowed"] !== false,
          showClassReadingList:
            prefs["dashboard_showClassReadingList"] !== false, // Updated preference key
          showHighlighted: prefs["dashboard_showHighlighted"] !== false, // New preference key
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
      this.loadReadingHistory(),
      this.loadActiveLoans(),
      this.fetchClassReadingList(), // Load Klasleeslijst
      this.loadHighlightedBooks(), // Load new highlighted books
    ]);

    if (this.showSections) {
      this.wishlistChangedSub = this.bookService.wishlistChanged$.subscribe(
        () => {
          this.loadWishlistBooks();
        },
      );
    }
  }

  private async fetchClassReadingList() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    this.classReadingListLoading = true;
    try {
      // Use BookService method for consistency and auth headers
      const res = await this.bookService.getClassReadingListItemIds(schoolId);
      const bookIds = res || [];

      if (bookIds.length === 0) {
        this.classReadingList = [];
        return;
      }

      const enriched = await this.bookService.enrichBooksWithDetails(
        bookIds.map((id) => ({ bookId: id })),
      );

      this.classReadingList = enriched.map((item: any) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
      }));
    } catch (error) {
      console.error("Failed to load class reading list", error);
    } finally {
      this.classReadingListLoading = false;
      this.cdr.detectChanges();
    }
  }

  private async loadHighlightedBooks() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    this.highlightedLoading = true;
    try {
      // Use BookService method for consistency and auth headers
      const res = await this.bookService.getHighlightedBookIds(schoolId);
      const bookIds = res || [];

      if (bookIds.length === 0) {
        this.highlightedBooks = [];
        return;
      }

      // Fetch details for these IDs
      const enriched = await this.bookService.enrichBooksWithDetails(
        bookIds.map((id) => ({ bookId: id })),
      );

      this.highlightedBooks = enriched.map((item: any) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
      }));
    } catch (error) {
      console.error("Failed to load highlighted books", error);
    } finally {
      this.highlightedLoading = false;
      this.cdr.detectChanges();
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
      const activeLoans = await this.loanService.getMyActiveLoans();
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

  get wishlistCount(): number {
    return this.wishlistBooks.length;
  }

  get classReadingListCount(): number {
    return this.classReadingList.length;
  }

  get highlightedBooksCount(): number {
    return this.highlightedBooks.length;
  }

  get activeSectionInfo() {
    if (!this.activeSectionInfoKey) {
      return null;
    }
    return this.sectionInfoContent[this.activeSectionInfoKey];
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

  goToPreviousReadingHistoryPage() {
    if (this.currentReadingHistoryPage > 1) {
      this.currentReadingHistoryPage--;
    }
  }

  goToNextReadingHistoryPage() {
    if (this.currentReadingHistoryPage < this.totalReadingHistoryPages) {
      this.currentReadingHistoryPage++;
    }
  }

  get visibleReadingHistoryPages(): (number | string)[] {
    const total = this.totalReadingHistoryPages;
    const current = this.currentReadingHistoryPage;
    const pages: (number | string)[] = [];

    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else {
      pages.push(1);
      if (current > 4) pages.push("...");

      const start = Math.max(2, current - 1);
      const end = Math.min(total - 1, current + 1);

      for (let i = start; i <= end; i++) pages.push(i);

      if (current < total - 3) pages.push("...");
      pages.push(total);
    }
    return pages;
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

  private navigateBackOrFallback(fallback: string): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate([fallback]);
    }
  }

  goBack() {
    this.navigateBackOrFallback("/dashboard");
  }

  toggleSettings() {
    this.settingsOpen = !this.settingsOpen;
  }

  async saveDashboardSettings() {
    this.isSavingSettings = true;
    const currentPrefs = this.userPreferencesService.getSnapshotForLegacyUse();

    // Snapshot the current UI state to prevent the reactive preferencesSub
    // from overwriting pending changes while we iterate through them.
    const settingsSnapshot = { ...this.dashboardSettings };

    try {
      const settingsToSave: {
        prop: string;
        key: PreferenceKey;
      }[] = [
        { prop: "showWishlist", key: "dashboard_showWishlist" },
        { prop: "showReadingHistory", key: "dashboard_showReadingHistory" },
        { prop: "showBorrowed", key: "dashboard_showBorrowed" },
        { prop: "showClassReadingList", key: "dashboard_showClassReadingList" },
        { prop: "showHighlighted", key: "dashboard_showHighlighted" },
        { prop: "showDeadline", key: "dashboard_showDeadline" },
      ];

      for (const item of settingsToSave) {
        const newValue = settingsSnapshot[item.prop];
        const oldValue = currentPrefs[item.key];

        if (newValue !== oldValue) {
          await this.userPreferencesService.savePreference(item.key, newValue);
        }
      }
    } finally {
      this.isSavingSettings = false;
      this.settingsOpen = false;
    }
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
