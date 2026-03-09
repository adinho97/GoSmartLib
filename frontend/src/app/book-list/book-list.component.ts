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
  readonly maxPageFilterLimit = 2000;
  boeken: Boek[] = [];
  isLoading = true;
  error = "";

  readonly userRole = localStorage.getItem("role");
  readonly isBibbeheerder = this.userRole === "bibbeheerder";
  readonly isLeerkrachtOfBeheerder = this.userRole === "leerkracht" || this.userRole === "bibbeheerder";

  readonly pageSize = 32;
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

  constructor(private bookService: BookService) {}

  async ngOnInit() {
    await this.loadBoeken();
  }

  async loadBoeken() {
    this.isLoading = true;
    this.error = "";
    try {
      const data = await this.bookService.getBoeken();
      this.boeken = data.sort((a: Boek, b: Boek) =>
        (a.titel || "").localeCompare(b.titel || "", "nl", { sensitivity: "base" })
      );
      this.currentPage = 1;
    } catch (err) {
      this.error = "Boeken laden mislukt. Probeer later opnieuw.";
    } finally {
      this.isLoading = false;
    }
  }

  get availableGenres(): string[] {
    const genres = this.boeken
      .map((boek) => (boek.genre || "").trim())
      .filter((genre) => genre.length > 0 && genre.toLowerCase() !== 'didactiek');
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
      const isDidactic = (boek.genre || "").toLowerCase() === 'didactiek';
      if (isDidactic && !this.isLeerkrachtOfBeheerder) return false;

      const titleOrAuthorMatches = !query || 
        (boek.titel || "").toLowerCase().includes(query) || 
        (boek.auteur || "").toLowerCase().includes(query);

      const genreMatches = !this.appliedGenre || (boek.genre || "").toLowerCase() === this.appliedGenre.toLowerCase();
      const taalMatches = !this.appliedTaal || (boek.taal || "").toLowerCase() === this.appliedTaal.toLowerCase();

      const boekDatum = this.parseDate(boek.uitgaveDatum);
      const fromDate = this.parseDate(this.appliedReleaseDateFrom);
      const toDate = this.parseDate(this.appliedReleaseDateTo);
      const dateMatches = (!fromDate || (boekDatum && boekDatum >= fromDate)) && (!toDate || (boekDatum && boekDatum <= toDate));

      const paginas = boek.paginas ?? 0;
      const paginaMatches = paginas >= this.appliedMinPages && paginas <= this.appliedMaxPages;

      return titleOrAuthorMatches && genreMatches && taalMatches && dateMatches && paginaMatches;
    });
  }

  get totalPages(): number { return Math.ceil(this.filteredBoeken.length / this.pageSize); }
  get pageNumbers(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i + 1); }
  get pagedBoeken(): Boek[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredBoeken.slice(start, start + this.pageSize);
  }

  applySearch() { this.searchQuery = this.searchInput.trim(); this.currentPage = 1; }
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
    this.searchInput = ""; this.searchQuery = ""; this.selectedGenre = ""; this.selectedTaal = "";
    this.releaseDateFrom = ""; this.releaseDateTo = ""; this.minPages = 0; this.maxPages = 1000;
    this.applyFilters();
  }

  onMinPagesChange(v: any) { this.minPages = Number(v); if (this.minPages > this.maxPages) this.maxPages = this.minPages; }
  onMaxPagesChange(v: any) { this.maxPages = Number(v); if (this.maxPages < this.minPages) this.minPages = this.maxPages; }
  gaNaarPagina(p: number) { this.currentPage = p; window.scrollTo({ top: 0, behavior: "smooth" }); }

  async verwijderBoek(event: MouseEvent, boek: Boek) {
    event.stopPropagation();
    if (!boek.id || !confirm(`Weet je zeker dat je "${boek.titel}" wilt verwijderen?`)) return;
    try {
      await this.bookService.deleteBoek(boek.id);
      this.boeken = this.boeken.filter(b => b.id !== boek.id);
    } catch { this.error = "Verwijderen mislukt."; }
  }

  private parseDate(v: string): Date | null { return v ? new Date(v) : null; }

  get selectedRangeLeft(): string { return `calc(8px + (${((this.minPages - 0) / 1000) * 100} * (100% - 16px) / 100))`; }
  get selectedRangeRight(): string { return `calc(8px + (${100 - ((this.maxPages - 0) / 1000) * 100} * (100% - 16px) / 100))`; }
}