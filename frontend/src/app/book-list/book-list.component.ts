import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";
import { UiToastService } from "../services/ui-toast.service";

type PaginationItem = number | "...";

type BookItem = {
  id?: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  lestip?: string;
  genre: string;
  uitgaveDatum: string;
  paginas: number | null;
  taal: string;
  uitgeverij: string;
  leesniveau?: string | null;
  reviewCount?: number;
  averageRating?: number;
};

type BookListFilterState = {
  searchInput: string;
  searchQuery: string;
  selectedGenre: string;
  selectedLanguage: string;
  selectedLeesniveau: string;
  selectedMinAverageRating: string;
  selectedNonFictionSubgenre: string;
  selectedDidacticSubgenre: string;
  minPages: number;
  maxPages: number;
  appliedGenre: string;
  appliedLanguage: string;
  appliedLeesniveau: string;
  appliedMinAverageRating: string;
  appliedNonFictionSubgenre: string;
  appliedDidacticSubgenre: string;
  appliedMinPages: number;
  appliedMaxPages: number;
};

@Component({
  selector: "app-book-list",
  templateUrl: "./book-list.component.html",
  styleUrls: ["./book-list.component.css"],
  standalone: false,
})
export class BookListComponent implements OnInit {
  private readonly FILTER_STORAGE_KEY = "bookListFiltersV1";
  private hasAppliedQueryGenre = false;
  readonly ratingStars = [0, 1, 2, 3, 4];
  readonly minPageFilterLimit = 0;
  readonly maxPageFilterLimit = 1000;
  readonly genres = [
    "Didactiek",
    "Fictie algemeen",
    "Literaire roman",
    "Spanning / thriller",
    "Detective / misdaad",
    "Fantasy",
    "Sciencefiction",
    "Dystopie",
    "Historische roman",
    "Romantiek",
    "Coming-of-age",
    "Avontuur",
    "Oorlog & conflict",
    "Horror",
    "Humor",
    "Graphic novel / strip",
    "Poëzie",
    "Non-fictie algemeen",
  ];
  readonly languages = [
    "Nederlands",
    "Engels",
    "Frans",
    "Duits",
    "Spaans",
    "Italiaans",
    "Portugees",
    "Latijn",
  ];
  readonly leesniveaus = ["A", "B", "C", "D"];
  readonly nonFictionSubgenres = [
    "Biografie / autobiografie",
    "Wetenschap & technologie",
    "Filosofie",
    "Maatschappij & politiek",
    "Psychologie",
    "Geschiedenis",
    "Kunst & cultuur",
  ];
  readonly didacticSubgenres = [
    "Wiskunde",
    "Taal",
    "Geschiedenis",
    "Kleuteronderwijs",
    "Lager onderwijs",
    "Secundair onderwijs",
    "Volwasseneneducatie",
    "Geheugen",
    "Begrip",
    "Denkprocessen",
    "Samenwerking",
    "Interactie",
    "Dialoog",
    "Online leren",
    "E-learning platforms",
    "Educatieve apps",
    "Creativiteit",
    "Zelfexpressie",
    "Ervaringsgericht leren",
  ];
  books: BookItem[] = [];
  isLoading = true;
  error = "";
  readonly userRole = (localStorage.getItem("role") || "").toLowerCase().trim();
  readonly isLibrarian = this.userRole.includes("bibbeheerder");
  readonly isTeacher = this.userRole.includes("leerkracht");
  readonly isTeacherOrLibrarian =
    this.isTeacher || this.isLibrarian || this.userRole.includes("super_admin");

  get availableGenres(): string[] {
    if (this.isTeacherOrLibrarian) {
      return this.genres;
    }
    // Hidden didactic collection for students/regular users
    return this.genres.filter((g) => g.toLowerCase() !== "didactiek");
  }
  readonly pageSize = 16;
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  userOwnSchoolId: number | null = null;
  currentPage = 1;
  searchInput = "";
  searchQuery = "";

  selectedGenre = "";
  selectedLanguage = "";
  selectedLeesniveau = "";
  selectedMinAverageRating = "";
  selectedNonFictionSubgenre = "";
  selectedDidacticSubgenre = "";
  minPages = this.minPageFilterLimit;
  maxPages = this.maxPageFilterLimit;

  appliedGenre = "";
  appliedLanguage = "";
  appliedLeesniveau = "";
  appliedMinAverageRating = "";
  appliedNonFictionSubgenre = "";
  appliedDidacticSubgenre = "";
  appliedMinPages = this.minPageFilterLimit;
  appliedMaxPages = this.maxPageFilterLimit;

  minAvailablePages = this.minPageFilterLimit;
  maxAvailablePages = this.maxPageFilterLimit;
  wishlistedBookIds = new Set<number>();
  highlightedBookIds = new Set<number>(); // New: Track highlighted books
  classReadingListItemIds = new Set<number>(); // New: Track class reading list books
  openMenuId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private bookService: BookService,
    private schoolService: SchoolService,
    private uiToastService: UiToastService,
  ) {
    // Close menu when clicking outside
    document.addEventListener("click", () => {
      this.closeKebabMenu();
    });
  }

  get isViewingOtherSchool(): boolean {
    return (
      this.isTeacherOrLibrarian &&
      this.userOwnSchoolId !== null &&
      this.selectedSchoolId !== null &&
      this.selectedSchoolId !== this.userOwnSchoolId
    );
  }

  async ngOnInit() {
    this.userOwnSchoolId = this.schoolService.getUserOwnSchoolId();
    this.restoreFilterState();
    this.initializeFiltersFromQueryParams();
    this.route.queryParamMap.subscribe((params) => {
      this.applyQueryGenreFilter(params.get("genre"));
    });

    await Promise.all([
      this.loadSchools().then(() => this.loadBooks()), // Sequential dependency: books need school selection
      this.loadWishlistState(), // Independent
      this.loadHighlightedBookIds(), // New: Load highlighted books
      this.loadClassReadingListItemIds(), // New: Load class reading list items
    ]);

    this.applyFilters();
  }

  private initializeFiltersFromQueryParams() {
    const genre = this.route.snapshot.queryParamMap.get("genre");
    this.applyQueryGenreFilter(genre);
  }

  private applyQueryGenreFilter(genre: string | null) {
    if (!genre) {
      // Clicking "Boekenlijst" should remove didactic quick-filter only
      // when a query-driven genre was previously applied.
      if (this.hasAppliedQueryGenre) {
        this.selectedGenre = "";
        this.clearGenreSubgenres();
        this.applyFilters();
      }
      this.hasAppliedQueryGenre = false;
      return;
    }

    if (genre.toLowerCase() === "didactiek") {
      if (!this.isTeacherOrLibrarian) return;
      this.selectedGenre = "Didactiek";
      this.clearGenreSubgenres();
      this.applyFilters();
      return;
    }

    const matchedGenre = this.genres.find(
      (g) => g.toLowerCase() === genre.toLowerCase(),
    );
    this.selectedGenre = matchedGenre || "";
    this.clearGenreSubgenres();
    this.hasAppliedQueryGenre = true;
    this.applyFilters();
  }

  private persistFilterState(): void {
    const state: BookListFilterState = {
      searchInput: this.searchInput,
      searchQuery: this.searchQuery,
      selectedGenre: this.selectedGenre,
      selectedLanguage: this.selectedLanguage,
      selectedLeesniveau: this.selectedLeesniveau,
      selectedMinAverageRating: this.selectedMinAverageRating,
      selectedNonFictionSubgenre: this.selectedNonFictionSubgenre,
      selectedDidacticSubgenre: this.selectedDidacticSubgenre,
      minPages: this.minPages,
      maxPages: this.maxPages,
      appliedGenre: this.appliedGenre,
      appliedLanguage: this.appliedLanguage,
      appliedLeesniveau: this.appliedLeesniveau,
      appliedMinAverageRating: this.appliedMinAverageRating,
      appliedNonFictionSubgenre: this.appliedNonFictionSubgenre,
      appliedDidacticSubgenre: this.appliedDidacticSubgenre,
      appliedMinPages: this.appliedMinPages,
      appliedMaxPages: this.appliedMaxPages,
    };
    localStorage.setItem(this.FILTER_STORAGE_KEY, JSON.stringify(state));
  }

  private restoreFilterState(): void {
    const raw = localStorage.getItem(this.FILTER_STORAGE_KEY);
    if (!raw) {
      return;
    }

    try {
      const state = JSON.parse(raw) as Partial<BookListFilterState>;
      this.searchInput = state.searchInput ?? "";
      this.searchQuery = state.searchQuery ?? "";
      this.selectedGenre = state.selectedGenre ?? "";
      this.selectedLanguage = state.selectedLanguage ?? "";
      this.selectedLeesniveau = state.selectedLeesniveau ?? "";
      this.selectedMinAverageRating = state.selectedMinAverageRating ?? "";
      this.selectedNonFictionSubgenre = state.selectedNonFictionSubgenre ?? "";
      this.selectedDidacticSubgenre = state.selectedDidacticSubgenre ?? "";
      this.minPages =
        typeof state.minPages === "number"
          ? state.minPages
          : this.minPageFilterLimit;
      this.maxPages =
        typeof state.maxPages === "number"
          ? state.maxPages
          : this.maxPageFilterLimit;

      this.appliedGenre = state.appliedGenre ?? this.selectedGenre;
      this.appliedLanguage = state.appliedLanguage ?? this.selectedLanguage;
      this.appliedLeesniveau =
        state.appliedLeesniveau ?? this.selectedLeesniveau;
      this.appliedMinAverageRating =
        state.appliedMinAverageRating ?? this.selectedMinAverageRating;
      this.appliedNonFictionSubgenre =
        state.appliedNonFictionSubgenre ?? this.selectedNonFictionSubgenre;
      this.appliedDidacticSubgenre =
        state.appliedDidacticSubgenre ?? this.selectedDidacticSubgenre;
      this.appliedMinPages =
        typeof state.appliedMinPages === "number"
          ? state.appliedMinPages
          : this.minPages;
      this.appliedMaxPages =
        typeof state.appliedMaxPages === "number"
          ? state.appliedMaxPages
          : this.maxPages;
    } catch {
      localStorage.removeItem(this.FILTER_STORAGE_KEY);
      return;
    }

    if (!this.isTeacherOrLibrarian && this.selectedGenre === "Didactiek") {
      this.selectedGenre = "";
      this.selectedDidacticSubgenre = "";
      this.appliedGenre = "";
      this.appliedDidacticSubgenre = "";
    }
  }

  private clearGenreSubgenres() {
    this.selectedNonFictionSubgenre = "";
    this.selectedDidacticSubgenre = "";
  }

  async loadSchools() {
    try {
      this.schools = await this.schoolService.getSchools();
      const storedSchoolId = this.schoolService.getSelectedSchoolId();
      const hasStoredSchool =
        storedSchoolId !== null &&
        this.schools.some((school) => school.id === storedSchoolId);

      const fallbackSchoolId =
        this.schools.length > 0 ? this.schools[0].id : null;
      this.selectedSchoolId = hasStoredSchool
        ? storedSchoolId
        : fallbackSchoolId;

      if (this.selectedSchoolId !== null) {
        this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
      }
    } catch {
      this.schools = [];
      this.selectedSchoolId = null;
    }
  }

  async loadBooks() {
    this.isLoading = true;
    this.error = "";
    try {
      const books = await this.bookService.getBooks(
        this.selectedSchoolId ?? undefined,
      );
      this.books = books.sort((a: BookItem, b: BookItem) =>
        (a.titel || "").localeCompare(b.titel || "", "nl", {
          sensitivity: "base",
        }),
      );
      if (this.isTeacherOrLibrarian) {
        await this.loadLestipsForTeacher();
      }
      this.currentPage = 1;
    } catch (err) {
      this.error = "Boeken laden mislukt.";
    } finally {
      this.isLoading = false;
    }
  }

  get filteredBooks(): BookItem[] {
    const query = this.searchQuery.trim().toLowerCase();
    return this.books.filter((book) => {
      const isDidactic = (book.genre || "").toLowerCase() === "didactiek";
      if (isDidactic && !this.isTeacherOrLibrarian) return false;

      const titleOrAuthorMatches =
        !query ||
        (book.titel || "").toLowerCase().includes(query) ||
        (book.auteur || "").toLowerCase().includes(query);

      const genreMatches =
        !this.appliedGenre ||
        ((): boolean => {
          const bookGenre = (book.genre || "").toLowerCase();
          const appliedGenre = this.appliedGenre.toLowerCase();

          if (appliedGenre === "didactiek") {
            if (!bookGenre.startsWith("didactiek")) return false;
            if (this.appliedDidacticSubgenre) {
              return bookGenre.includes(
                this.appliedDidacticSubgenre.toLowerCase(),
              );
            }
            return true;
          }

          // For non-fiction, check genre prefix and apply subgenre filter if set
          if (appliedGenre === "non-fictie algemeen") {
            if (!bookGenre.includes("non-fictie algemeen")) return false;
            // If a subgenre is selected, check if book contains it
            if (this.appliedNonFictionSubgenre) {
              return bookGenre.includes(
                this.appliedNonFictionSubgenre.toLowerCase(),
              );
            }
            return true;
          }

          // Exact match for other genres
          return bookGenre === appliedGenre;
        })();
      const languageMatches =
        !this.appliedLanguage ||
        (book.taal || "").toLowerCase() === this.appliedLanguage.toLowerCase();
      const leesniveauMatches =
        !this.appliedLeesniveau ||
        (book.leesniveau || "").toLowerCase() ===
          this.appliedLeesniveau.toLowerCase();

      const minAverage = Number(this.appliedMinAverageRating);
      const averageMatches =
        !this.appliedMinAverageRating ||
        ((book.reviewCount || 0) >= 10 &&
          (book.averageRating || 0) >= minAverage);

      const pageCount = book.paginas ?? 0;
      let pageMatches = false;
      if (this.appliedMaxPages === this.maxPageFilterLimit) {
        // If the max page filter is at its maximum limit, include books with more pages than the limit
        pageMatches = pageCount >= this.appliedMinPages;
      } else {
        pageMatches =
          pageCount >= this.appliedMinPages &&
          pageCount <= this.appliedMaxPages;
      }

      return (
        titleOrAuthorMatches &&
        genreMatches &&
        languageMatches &&
        leesniveauMatches &&
        averageMatches &&
        pageMatches
      );
    });
  }

  get totalPages(): number {
    return Math.ceil(this.filteredBooks.length / this.pageSize);
  }

  get pageNumbers(): PaginationItem[] {
    return this.buildVisiblePages(this.totalPages, this.currentPage);
  }

  private buildVisiblePages(
    totalPages: number,
    currentPage: number,
  ): PaginationItem[] {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    const candidates = new Set<number>([
      1,
      2,
      totalPages - 1,
      totalPages,
      currentPage - 1,
      currentPage,
      currentPage + 1,
    ]);

    const pages = Array.from(candidates)
      .filter((page) => page >= 1 && page <= totalPages)
      .sort((left, right) => left - right);

    const result: PaginationItem[] = [];
    for (let index = 0; index < pages.length; index++) {
      const page = pages[index];
      if (index > 0) {
        const previousPage = pages[index - 1];
        if (page - previousPage > 1) {
          result.push("...");
        }
      }
      result.push(page);
    }

    return result;
  }
  get pagedBooks(): BookItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredBooks.slice(start, start + this.pageSize);
  }

  applySearch() {
    this.searchQuery = this.searchInput.trim();
    this.currentPage = 1;
    this.persistFilterState();
  }
  applyFilters() {
    this.appliedGenre = this.selectedGenre;
    this.appliedLanguage = this.selectedLanguage;
    this.appliedLeesniveau = this.selectedLeesniveau;
    this.appliedMinAverageRating = this.selectedMinAverageRating;
    this.appliedNonFictionSubgenre = this.selectedNonFictionSubgenre;
    this.appliedDidacticSubgenre = this.selectedDidacticSubgenre;
    this.appliedMinPages = this.minPages;
    this.appliedMaxPages = this.maxPages;
    this.currentPage = 1;
    this.persistFilterState();
  }

  onGenreChange() {
    // Clear subgenre filter when genre changes
    if (this.selectedGenre !== "Non-fictie algemeen") {
      this.selectedNonFictionSubgenre = "";
    }
    if (this.selectedGenre !== "Didactiek") {
      this.selectedDidacticSubgenre = "";
    }
  }

  clearFilters() {
    this.searchInput = "";
    this.searchQuery = "";
    this.selectedGenre = "";
    this.selectedLanguage = "";
    this.selectedLeesniveau = "";
    this.selectedMinAverageRating = "";
    this.selectedNonFictionSubgenre = "";
    this.selectedDidacticSubgenre = "";
    this.minPages = this.minPageFilterLimit;
    this.maxPages = this.maxPageFilterLimit;
    this.applyFilters();
    this.persistFilterState();
  }

  onMinPagesChange(v: any) {
    this.minPages = Number(v);
    if (this.minPages > this.maxPages) this.maxPages = this.minPages;
  }
  onMaxPagesChange(v: any) {
    this.maxPages = Number(v);
    if (this.maxPages < this.minPages) this.minPages = this.maxPages;
  }

  private getSliderFillPercent(value: number): number {
    const min = this.minAvailablePages;
    const max = this.maxAvailablePages;
    if (max <= min) return 0;
    const normalized = ((value - min) / (max - min)) * 100;
    return Math.min(100, Math.max(0, normalized));
  }

  get minPagesSliderFill(): number {
    return this.getSliderFillPercent(this.minPages);
  }

  get maxPagesSliderFill(): number {
    return this.getSliderFillPercent(this.maxPages);
  }

  goToPage(p: number) {
    this.currentPage = p;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async deleteBook(event: MouseEvent, book: BookItem) {
    event.stopPropagation();
    if (!book.id || !confirm(`Verwijderen?`)) return;
    try {
      await this.bookService.deleteBook(
        book.id,
        this.selectedSchoolId ?? undefined,
      );
      this.books = this.books.filter((b) => b.id !== book.id);
      if (this.currentPage > this.totalPages && this.totalPages > 0) {
        this.currentPage = this.totalPages;
      }
    } catch {
      this.error = "Verwijderen mislukt. Probeer later opnieuw.";
    }
  }

  getAverageRatingFillPercentage(book: BookItem): number {
    const average = book.averageRating || 0;
    const percentage = (average / 5) * 100;
    return Math.min(100, Math.max(0, percentage));
  }

  getStarFillPercentage(book: BookItem, starIndex: number): number {
    const average = Math.min(5, Math.max(0, book.averageRating || 0));
    const fillForStar = average - starIndex;
    return Math.min(100, Math.max(0, fillForStar * 100));
  }

  async onSchoolChange(value: string) {
    this.selectedSchoolId = value ? Number(value) : null;

    if (this.selectedSchoolId !== null) {
      this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    }

    await this.loadBooks();
  }

  private async loadWishlistState() {
    try {
      const wishlist = await this.bookService.getUserWishlist();
      this.wishlistedBookIds = new Set(wishlist.map((item) => item.bookId));
    } catch {
      this.wishlistedBookIds = new Set<number>();
    }
  }

  private async loadHighlightedBookIds() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;
    try {
      const ids = await this.bookService.getHighlightedBookIds(schoolId);
      this.highlightedBookIds = new Set(ids);
    } catch (error) {
      console.error("Failed to load highlighted book IDs:", error);
      this.highlightedBookIds = new Set();
    }
  }

  private async loadClassReadingListItemIds() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;
    try {
      const ids = await this.bookService.getClassReadingListItemIds(schoolId);
      this.classReadingListItemIds = new Set(ids);
    } catch (error) {
      console.error("Failed to load class reading list item IDs:", error);
      this.classReadingListItemIds = new Set();
    }
  }

  isBookHighlighted(bookId?: number): boolean {
    return !!bookId && this.highlightedBookIds.has(bookId);
  }

  isBookInClassReadingList(bookId?: number): boolean {
    return !!bookId && this.classReadingListItemIds.has(bookId);
  }

  async toggleWishlist(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    event.preventDefault();
    if (!bookId) return;

    try {
      if (this.wishlistedBookIds.has(bookId)) {
        await this.bookService.removeFromWishlist(bookId);
        this.wishlistedBookIds.delete(bookId);
        this.uiToastService.success("Boek verwijderd van je verlanglijst.");
        return;
      }

      await this.bookService.addToWishlist(bookId);
      this.wishlistedBookIds.add(bookId);
      this.uiToastService.success("Boek toegevoegd aan je verlanglijst.");
    } catch {
      this.error = "Verlanglijst bijwerken mislukt. Probeer later opnieuw.";
      this.uiToastService.error("Verlanglijst bijwerken mislukt.");
    }
  }

  async toggleHighlight(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    event.preventDefault();
    if (!bookId) return;

    try {
      const isNowHighlighted = await this.bookService.toggleHighlight(bookId);
      if (isNowHighlighted) {
        this.highlightedBookIds.add(bookId);
        this.uiToastService.success("Boek staat nu 'In de kijker'.");
      } else {
        this.highlightedBookIds.delete(bookId);
        this.uiToastService.success("Markering van boek verwijderd.");
      }
    } catch {
      this.uiToastService.error("Fout bij bijwerken markering.");
    }
  }

  async toggleClassReadingList(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    event.preventDefault();
    if (!bookId) return;

    try {
      const isNowInList =
        await this.bookService.toggleClassReadingListItem(bookId);
      if (isNowInList) this.classReadingListItemIds.add(bookId);
      else this.classReadingListItemIds.delete(bookId);
      this.uiToastService.success(
        isNowInList
          ? "Toegevoegd aan Klasleeslijst."
          : "Verwijderd uit Klasleeslijst.",
      );
    } catch {
      this.uiToastService.error("Fout bij bijwerken Klasleeslijst.");
    }
  }

  isWishlisted(bookId?: number): boolean {
    return !!bookId && this.wishlistedBookIds.has(bookId);
  }

  toggleKebabMenu(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    if (!bookId) return;
    this.openMenuId = this.openMenuId === bookId ? null : bookId;
  }

  closeKebabMenu() {
    this.openMenuId = null;
  }

  private async loadLestipsForTeacher(): Promise<void> {
    const lestipPromises = this.books.map(async (book) => {
      if (!book.id) {
        return;
      }

      try {
        const lestip = await this.bookService.getBookLestip(book.id);
        book.lestip = lestip;
      } catch {
        book.lestip = "";
      }
    });

    await Promise.all(lestipPromises);
  }
}
