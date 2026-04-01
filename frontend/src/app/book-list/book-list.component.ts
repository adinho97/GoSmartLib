import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

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
  reviewCount?: number;
  averageRating?: number;
};

@Component({
  selector: "app-book-list",
  templateUrl: "./book-list.component.html",
  styleUrls: ["./book-list.component.css"],
  standalone: false,
})
export class BookListComponent implements OnInit {
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
  readonly userRole = localStorage.getItem("role");
  readonly isLibrarian = this.userRole === "bibbeheerder";
  readonly isTeacher = this.userRole === "leerkracht";
  readonly isTeacherOrLibrarian =
    this.userRole === "leerkracht" || this.userRole === "bibbeheerder";
  readonly pageSize = 16;
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  currentPage = 1;
  searchInput = "";
  searchQuery = "";

  selectedGenre = "";
  selectedLanguage = "";
  selectedMinAverageRating = "";
  selectedNonFictionSubgenre = "";
  selectedDidacticSubgenre = "";
  minPages = this.minPageFilterLimit;
  maxPages = this.maxPageFilterLimit;

  appliedGenre = "";
  appliedLanguage = "";
  appliedMinAverageRating = "";
  appliedNonFictionSubgenre = "";
  appliedDidacticSubgenre = "";
  appliedMinPages = this.minPageFilterLimit;
  appliedMaxPages = this.maxPageFilterLimit;

  minAvailablePages = this.minPageFilterLimit;
  maxAvailablePages = this.maxPageFilterLimit;
  wishlistedBookIds = new Set<number>();
  favoritedBookIds = new Set<number>();
  openMenuId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {
    // Close menu when clicking outside
    document.addEventListener("click", () => {
      this.closeKebabMenu();
    });
  }

  async ngOnInit() {
    this.initializeFiltersFromQueryParams();
    this.route.queryParamMap.subscribe((params) => {
      this.applyQueryGenreFilter(params.get("genre"));
    });
    await this.loadSchools();
    await this.loadBooks();
    await this.loadWishlistState();
    await this.loadFavoritesState();
    this.applyFilters();
  }

  private initializeFiltersFromQueryParams() {
    const genre = this.route.snapshot.queryParamMap.get("genre");
    this.applyQueryGenreFilter(genre);
  }

  private applyQueryGenreFilter(genre: string | null) {
    if (!genre) {
      // Clicking "Boekenlijst" removes the didactic quick-filter.
      this.selectedGenre = "";
      this.clearGenreSubgenres();
      this.applyFilters();
      return;
    }

    if (genre.toLowerCase() === "didactiek") {
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
    this.applyFilters();
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
      if (this.isTeacher) {
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

      const minAverage = Number(this.appliedMinAverageRating);
      const averageMatches =
        !this.appliedMinAverageRating ||
        ((book.reviewCount || 0) >= 10 &&
          (book.averageRating || 0) >= minAverage);

      const pageCount = book.paginas ?? 0;
      const pageMatches =
        pageCount >= this.appliedMinPages && pageCount <= this.appliedMaxPages;

      return (
        titleOrAuthorMatches &&
        genreMatches &&
        languageMatches &&
        averageMatches &&
        pageMatches
      );
    });
  }

  get totalPages(): number {
    return Math.ceil(this.filteredBooks.length / this.pageSize);
  }
  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
  get pagedBooks(): BookItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredBooks.slice(start, start + this.pageSize);
  }

  applySearch() {
    this.searchQuery = this.searchInput.trim();
    this.currentPage = 1;
  }
  applyFilters() {
    this.appliedGenre = this.selectedGenre;
    this.appliedLanguage = this.selectedLanguage;
    this.appliedMinAverageRating = this.selectedMinAverageRating;
    this.appliedNonFictionSubgenre = this.selectedNonFictionSubgenre;
    this.appliedDidacticSubgenre = this.selectedDidacticSubgenre;
    this.appliedMinPages = this.minPages;
    this.appliedMaxPages = this.maxPages;
    this.currentPage = 1;
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
    this.selectedMinAverageRating = "";
    this.selectedNonFictionSubgenre = "";
    this.selectedDidacticSubgenre = "";
    this.minPages = this.minPageFilterLimit;
    this.maxPages = this.maxPageFilterLimit;
    this.applyFilters();
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

  private async loadFavoritesState() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      this.favoritedBookIds = new Set(favorites.map((item) => item.bookId));
    } catch {
      this.favoritedBookIds = new Set<number>();
    }
  }

  async toggleWishlist(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    event.preventDefault();
    if (!bookId) return;

    try {
      if (this.wishlistedBookIds.has(bookId)) {
        await this.bookService.removeFromWishlist(bookId);
        this.wishlistedBookIds.delete(bookId);
        return;
      }

      await this.bookService.addToWishlist(bookId);
      this.wishlistedBookIds.add(bookId);
    } catch {
      this.error = "Verlanglijst bijwerken mislukt. Probeer later opnieuw.";
    }
  }

  isWishlisted(bookId?: number): boolean {
    return !!bookId && this.wishlistedBookIds.has(bookId);
  }

  async toggleFavorite(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    event.preventDefault();
    if (!bookId) return;

    try {
      if (this.favoritedBookIds.has(bookId)) {
        await this.bookService.removeFromFavorites(bookId);
        this.favoritedBookIds.delete(bookId);
        return;
      }

      await this.bookService.addToFavorites(bookId);
      this.favoritedBookIds.add(bookId);
    } catch {
      this.error = "Favorieten bijwerken mislukt. Probeer later opnieuw.";
    }
  }

  isFavorited(bookId?: number): boolean {
    return !!bookId && this.favoritedBookIds.has(bookId);
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
