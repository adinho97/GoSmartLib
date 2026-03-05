import { Component, OnInit } from "@angular/core";
import { BookService } from "../services/book.service";

type Boek = {
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
})
export class BookListComponent implements OnInit {
  readonly minPageFilterLimit = 0;
  readonly maxPageFilterLimit = 1000;
  boeken: Boek[] = [];
  isLoading = true;
  error = "";
  readonly isBibbeheerder = localStorage.getItem("role") === "bibbeheerder";
  readonly pageSize = 32;
  currentPage = 1;
  searchInput = "";
  searchQuery = "";
  selectedGenre = "";
  selectedTaal = "";
  releaseDateFrom = "";
  releaseDateTo = "";
  appliedGenre = "";
  appliedTaal = "";
  appliedReleaseDateFrom = "";
  appliedReleaseDateTo = "";
  minAvailablePages = this.minPageFilterLimit;
  maxAvailablePages = this.maxPageFilterLimit;
  minPages = this.minPageFilterLimit;
  maxPages = this.maxPageFilterLimit;
  appliedMinPages = this.minPageFilterLimit;
  appliedMaxPages = this.maxPageFilterLimit;

  constructor(private bookService: BookService) {}

  async ngOnInit() {
    await this.loadBoeken();
  }

  async loadBoeken() {
    this.isLoading = true;
    this.error = "";

    try {
      const boeken = await this.bookService.getBoeken();
      this.boeken = boeken.sort((a: Boek, b: Boek) =>
        (a.titel || "").localeCompare(b.titel || "", "nl", {
          sensitivity: "base",
        }),
      );
      this.updatePageBounds();
      this.currentPage = 1;
    } catch {
      this.error = "Boeken laden mislukt. Probeer later opnieuw.";
    } finally {
      this.isLoading = false;
    }
  }

  get availableGenres(): string[] {
    const genres = this.boeken
      .map((boek) => (boek.genre || "").trim())
      .filter((genre) => genre.length > 0);

    return Array.from(new Set(genres)).sort((a, b) =>
      a.localeCompare(b, "nl", { sensitivity: "base" }),
    );
  }

  get availableTalen(): string[] {
    const talen = this.boeken
      .map((boek) => (boek.taal || "").trim())
      .filter((taal) => taal.length > 0);

    return Array.from(new Set(talen)).sort((a, b) =>
      a.localeCompare(b, "nl", { sensitivity: "base" }),
    );
  }

  get filteredBoeken(): Boek[] {
    const normalizedQuery = this.searchQuery.trim().toLowerCase();

    return this.boeken.filter((boek) => {
      const titleOrAuthorMatches =
        !normalizedQuery ||
        (boek.titel || "").toLowerCase().includes(normalizedQuery) ||
        (boek.auteur || "").toLowerCase().includes(normalizedQuery);

      const genreMatches =
        !this.appliedGenre ||
        (boek.genre || "").toLowerCase() === this.appliedGenre.toLowerCase();

      const taalMatches =
        !this.appliedTaal ||
        (boek.taal || "").toLowerCase() === this.appliedTaal.toLowerCase();

      const boekDatum = this.parseDate(boek.uitgaveDatum);
      const fromDate = this.parseDate(this.appliedReleaseDateFrom);
      const toDate = this.parseDate(this.appliedReleaseDateTo);

      const dateFromMatches =
        !fromDate || (!!boekDatum && boekDatum >= fromDate);
      const dateToMatches = !toDate || (!!boekDatum && boekDatum <= toDate);

      const paginas = boek.paginas ?? 0;
      const paginaMatches =
        paginas >= this.appliedMinPages && paginas <= this.appliedMaxPages;

      return (
        titleOrAuthorMatches &&
        genreMatches &&
        taalMatches &&
        dateFromMatches &&
        dateToMatches &&
        paginaMatches
      );
    });
  }

  get totalPages(): number {
    return Math.ceil(this.filteredBoeken.length / this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, index) => index + 1);
  }

  get pagedBoeken(): Boek[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredBoeken.slice(start, start + this.pageSize);
  }

  get minPagesPercent(): number {
    const range = this.maxAvailablePages - this.minAvailablePages;

    if (range <= 0) {
      return 0;
    }

    return ((this.minPages - this.minAvailablePages) / range) * 100;
  }

  get maxPagesPercent(): number {
    const range = this.maxAvailablePages - this.minAvailablePages;

    if (range <= 0) {
      return 100;
    }

    return ((this.maxPages - this.minAvailablePages) / range) * 100;
  }

  get selectedRangeLeft(): string {
    if (this.minPages <= this.minAvailablePages) {
      return "0";
    }

    return `calc(8px + (${this.minPagesPercent} * (100% - 16px) / 100))`;
  }

  get selectedRangeRight(): string {
    if (this.maxPages >= this.maxAvailablePages) {
      return "0";
    }

    return `calc(8px + (${100 - this.maxPagesPercent} * (100% - 16px) / 100))`;
  }

  applySearch() {
    this.searchQuery = this.searchInput.trim();
    this.currentPage = 1;
  }

  applyFilters() {
    this.ensurePageRangeValidity();
    this.appliedGenre = this.selectedGenre;
    this.appliedTaal = this.selectedTaal;
    this.appliedReleaseDateFrom = this.releaseDateFrom;
    this.appliedReleaseDateTo = this.releaseDateTo;
    this.appliedMinPages = this.minPages;
    this.appliedMaxPages = this.maxPages;
    this.currentPage = 1;
  }

  onMinPagesChange(value: number | string) {
    this.minPages = Number(value);

    if (this.minPages > this.maxPages) {
      this.maxPages = this.minPages;
    }
  }

  onMaxPagesChange(value: number | string) {
    this.maxPages = Number(value);

    if (this.maxPages < this.minPages) {
      this.minPages = this.maxPages;
    }
  }

  clearFilters() {
    this.searchInput = "";
    this.searchQuery = "";
    this.selectedGenre = "";
    this.selectedTaal = "";
    this.releaseDateFrom = "";
    this.releaseDateTo = "";
    this.appliedGenre = "";
    this.appliedTaal = "";
    this.appliedReleaseDateFrom = "";
    this.appliedReleaseDateTo = "";
    this.minPages = this.minPageFilterLimit;
    this.maxPages = this.maxPageFilterLimit;
    this.appliedMinPages = this.minPageFilterLimit;
    this.appliedMaxPages = this.maxPageFilterLimit;
    this.currentPage = 1;
  }

  gaNaarPagina(page: number) {
    if (page < 1 || page > this.totalPages) {
      return;
    }

    this.currentPage = page;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async verwijderBoek(event: MouseEvent, boek: Boek) {
    event.stopPropagation();
    event.preventDefault();

    if (!boek.id) {
      return;
    }

    const isConfirmed = window.confirm(
      `Weet je zeker dat je "${boek.titel}" wil verwijderen?`,
    );

    if (!isConfirmed) {
      return;
    }

    try {
      await this.bookService.deleteBoek(boek.id);
      this.boeken = this.boeken.filter((b) => b.id !== boek.id);
      this.updatePageBounds();

      if (this.currentPage > this.totalPages && this.totalPages > 0) {
        this.currentPage = this.totalPages;
      }
    } catch {
      this.error = "Verwijderen mislukt. Probeer later opnieuw.";
    }
  }

  private updatePageBounds() {
    this.minAvailablePages = this.minPageFilterLimit;
    this.maxAvailablePages = this.maxPageFilterLimit;
    this.ensurePageRangeValidity();
  }

  private ensurePageRangeValidity() {
    this.minPages = Math.max(this.minPages, this.minPageFilterLimit);
    this.minPages = Math.min(this.minPages, this.maxPageFilterLimit);

    this.maxPages = Math.max(this.maxPages, this.minPageFilterLimit);
    this.maxPages = Math.min(this.maxPages, this.maxPageFilterLimit);

    if (this.minPages > this.maxPages) {
      this.maxPages = this.minPages;
    }
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
}
