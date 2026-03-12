import { Component, OnInit } from "@angular/core";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

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
    standalone: false
})
export class BookListComponent implements OnInit {
  readonly minPageFilterLimit = 0;
  readonly maxPageFilterLimit = 5000;
  boeken: Boek[] = [];
  isLoading = true;
  error = "";
  readonly userRole = localStorage.getItem("role");
  readonly isBibbeheerder = this.userRole === "bibbeheerder";
  readonly isLeerkracht = this.userRole === "leerkracht";
  readonly isLeerkrachtOfBeheerder =
    this.userRole === "leerkracht" || this.userRole === "bibbeheerder";
  readonly pageSize = 32;
  scholen: School[] = [];
  selectedSchoolId: number | null = null;
  currentPage = 1;
  searchInput = "";
  searchQuery = "";

  selectedGenre = "";
  selectedTaal = "";
  releaseDateFrom = "";
  releaseDateTo = "";
  minPages = this.minPageFilterLimit;
  maxPages = this.maxPageFilterLimit;

  appliedGenre = "";
  appliedTaal = "";
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
    await this.loadScholen();
    await this.loadBoeken();
  }

  async loadScholen() {
    try {
      this.scholen = await this.schoolService.getScholen();
      const storedSchoolId = this.schoolService.getSelectedSchoolId();
      const hasStoredSchool =
        storedSchoolId !== null &&
        this.scholen.some((school) => school.id === storedSchoolId);

      const fallbackSchoolId =
        this.scholen.length > 0 ? this.scholen[0].id : null;
      this.selectedSchoolId = hasStoredSchool
        ? storedSchoolId
        : fallbackSchoolId;

      if (this.selectedSchoolId !== null) {
        this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
      }
    } catch {
      this.scholen = [];
      this.selectedSchoolId = null;
    }
  }

  async loadBoeken() {
    this.isLoading = true;
    this.error = "";
    try {
      const boeken = await this.bookService.getBoeken(
        this.selectedSchoolId ?? undefined,
      );
      this.boeken = boeken.sort((a: Boek, b: Boek) =>
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
    const genres = this.boeken
      .map((boek) => (boek.genre || "").trim())
      .filter(
        (genre) => genre.length > 0 && genre.toLowerCase() !== "didactiek",
      );
    return Array.from(new Set(genres)).sort((a, b) => a.localeCompare(b, "nl"));
  }

  get availableTalen(): string[] {
    const talen = this.boeken
      .map((boek) => (boek.taal || "").trim())
      .filter((taal) => taal.length > 0);
    return Array.from(new Set(talen)).sort((a, b) => a.localeCompare(b, "nl"));
  }

  get filteredBoeken(): Boek[] {
    const query = this.searchQuery.trim().toLowerCase();
    return this.boeken.filter((boek) => {
      const isDidactic = (boek.genre || "").toLowerCase() === "didactiek";
      if (isDidactic && !this.isLeerkrachtOfBeheerder) return false;

      const titleOrAuthorMatches =
        !query ||
        (boek.titel || "").toLowerCase().includes(query) ||
        (boek.auteur || "").toLowerCase().includes(query);

      const genreMatches =
        !this.appliedGenre ||
        (boek.genre || "").toLowerCase() === this.appliedGenre.toLowerCase();
      const taalMatches =
        !this.appliedTaal ||
        (boek.taal || "").toLowerCase() === this.appliedTaal.toLowerCase();

      const boekDatum = this.parseDate(boek.uitgaveDatum);
      const fromDate = this.parseDate(this.appliedReleaseDateFrom);
      const toDate = this.parseDate(this.appliedReleaseDateTo);
      const dateMatches =
        (!fromDate || (boekDatum && boekDatum >= fromDate)) &&
        (!toDate || (boekDatum && boekDatum <= toDate));

      const paginas = boek.paginas ?? 0;
      const paginaMatches =
        paginas >= this.appliedMinPages && paginas <= this.appliedMaxPages;

      return (
        titleOrAuthorMatches &&
        genreMatches &&
        taalMatches &&
        dateMatches &&
        paginaMatches
      );
    });
  }

  get totalPages(): number {
    return Math.ceil(this.filteredBoeken.length / this.pageSize);
  }
  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
  get pagedBoeken(): Boek[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredBoeken.slice(start, start + this.pageSize);
  }

  applySearch() {
    this.searchQuery = this.searchInput.trim();
    this.currentPage = 1;
  }
  applyFilters() {
    this.appliedGenre = this.selectedGenre;
    this.appliedTaal = this.selectedTaal;
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
    this.selectedTaal = "";
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
  gaNaarPagina(p: number) {
    this.currentPage = p;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async verwijderBoek(event: MouseEvent, boek: Boek) {
    event.stopPropagation();
    if (!boek.id || !confirm(`Verwijderen?`)) return;
    try {
      await this.bookService.deleteBoek(
        boek.id,
        this.selectedSchoolId ?? undefined,
      );
      this.boeken = this.boeken.filter((b) => b.id !== boek.id);
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

    await this.loadBoeken();
  }
}
