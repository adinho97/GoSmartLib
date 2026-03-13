import { Component, OnInit } from "@angular/core";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

type BookItem = {
  id?: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  genre: string;
  uitgaveDatum: string;
  paginas: number | null;
  taal: string;
  uitgeverij: string;
};

@Component({
    selector: "app-book-list",
    templateUrl: "./book-list.component.html",
    styleUrls: ["./book-list.component.css"],
    standalone: false
})
export class BookListComponent implements OnInit {
  readonly minPageFilterLimit = 0;
  readonly maxPageFilterLimit = 5000;
  books: BookItem[] = [];
  isLoading = true;
  error = "";
  readonly userRole = localStorage.getItem("role");
  readonly isLibrarian = this.userRole === "bibbeheerder";
  readonly isTeacher = this.userRole === "leerkracht";
  readonly isTeacherOrLibrarian =
    this.userRole === "leerkracht" || this.userRole === "bibbeheerder";
  readonly pageSize = 32;
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  currentPage = 1;
  searchInput = "";
  searchQuery = "";

  selectedGenre = "";
  selectedLanguage = "";
  releaseDateFrom = "";
  releaseDateTo = "";
  minPages = this.minPageFilterLimit;
  maxPages = this.maxPageFilterLimit;

  appliedGenre = "";
  appliedLanguage = "";
  appliedReleaseDateFrom = "";
  appliedReleaseDateTo = "";
  appliedMinPages = this.minPageFilterLimit;
  appliedMaxPages = this.maxPageFilterLimit;

  minAvailablePages = this.minPageFilterLimit;
  maxAvailablePages = this.maxPageFilterLimit;

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit() {
    await this.loadSchools();
    await this.loadBooks();
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
      this.currentPage = 1;
    } catch (err) {
      this.error = "Boeken laden mislukt.";
    } finally {
      this.isLoading = false;
    }
  }

  get availableGenres(): string[] {
    const genres = this.books
      .map((book) => (book.genre || "").trim())
      .filter(
        (genre) => genre.length > 0 && genre.toLowerCase() !== "didactiek",
      );
    return Array.from(new Set(genres)).sort((a, b) => a.localeCompare(b, "nl"));
  }

  get availableLanguages(): string[] {
    const languages = this.books
      .map((book) => (book.taal || "").trim())
      .filter((language) => language.length > 0);
    return Array.from(new Set(languages)).sort((a, b) => a.localeCompare(b, "nl"));
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
        (book.genre || "").toLowerCase() === this.appliedGenre.toLowerCase();
      const languageMatches =
        !this.appliedLanguage ||
        (book.taal || "").toLowerCase() === this.appliedLanguage.toLowerCase();

      const bookDate = this.parseDate(book.uitgaveDatum);
      const fromDate = this.parseDate(this.appliedReleaseDateFrom);
      const toDate = this.parseDate(this.appliedReleaseDateTo);
      const dateMatches =
        (!fromDate || (bookDate && bookDate >= fromDate)) &&
        (!toDate || (bookDate && bookDate <= toDate));

      const pageCount = book.paginas ?? 0;
      const pageMatches =
        pageCount >= this.appliedMinPages && pageCount <= this.appliedMaxPages;

      return (
        titleOrAuthorMatches &&
        genreMatches &&
        languageMatches &&
        dateMatches &&
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
    this.appliedReleaseDateFrom = this.releaseDateFrom;
    this.appliedReleaseDateTo = this.releaseDateTo;
    this.appliedMinPages = this.minPages;
    this.appliedMaxPages = this.maxPages;
    this.currentPage = 1;
  }

  clearFilters() {
    this.searchInput = "";
    this.searchQuery = "";
    this.selectedGenre = "";
    this.selectedLanguage = "";
    this.releaseDateFrom = "";
    this.releaseDateTo = "";
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

  get selectedRangeLeft(): string {
    return (this.minPages / this.maxPageFilterLimit) * 100 + "%";
  }

  get selectedRangeRight(): string {
    return 100 - (this.maxPages / this.maxPageFilterLimit) * 100 + "%";
  }

  private parseDate(value: string): Date | null {
    if (!value) {
      return null;
    }

    const parsedDate = new Date(value);

    if (Number.isNaN(parsedDate.getTime())) {
      return null;
    }

    parsedDate.setHours(0, 0, 0, 0);
    return parsedDate;
  }

  async onSchoolChange(value: string) {
    this.selectedSchoolId = value ? Number(value) : null;

    if (this.selectedSchoolId !== null) {
      this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    }

    await this.loadBooks();
  }
}
