import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { SchoolService, KlasListItem } from "../services/school.service";
import { BookService } from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";
import { Book } from "../models/book";

type Step = "titel" | "boeken" | "bevestiging";

@Component({
  selector: "app-leeslijst-create",
  templateUrl: "./leeslijst-create.component.html",
  styleUrls: ["./leeslijst-create.component.css"],
  standalone: false,
})
export class LeeslijstCreateComponent implements OnInit {
  step: Step = "titel";
  isLoading = false;
  isSaving = false;
  klassenLoading = false;

  // Title
  leeslijstTitel = "";
  leeslijstDescription = "";

  // Klassen from database
  klassen: KlasListItem[] = [];
  klasSearchQuery = "";
  selectedKlassenIds = new Set<number>();

  // Step 2: Boeken
  books: Book[] = [];
  selectedBookIds = new Set<number>();
  searchInput = "";
  searchQuery = "";
  currentPage = 1;
  pageSize = 10;
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

  selectedGenre = "";
  selectedLanguage = "";
  selectedLeesniveau = "";
  selectedMinAverageRating = "";
  selectedNonFictionSubgenre = "";
  selectedDidacticSubgenre = "";
  minPages = this.minPageFilterLimit;
  maxPages = this.maxPageFilterLimit;
  minAvailablePages = this.minPageFilterLimit;
  maxAvailablePages = this.maxPageFilterLimit;

  constructor(
    private schoolService: SchoolService,
    private bookService: BookService,
    private uiToastService: UiToastService,
    private router: Router,
  ) {}

  async ngOnInit() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.uiToastService.error("Geen school geselecteerd.");
      this.router.navigate(["/dashboard"]);
      return;
    }
  }

  goToBooks() {
    if (!this.leeslijstTitel || this.leeslijstTitel.trim() === "") {
      this.uiToastService.error("Voer een titel in voor de leeslijst.");
      return;
    }
    this.step = "boeken";
    this.loadKlassen();
    this.loadBooks();
  }

  async loadKlassen() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    this.klassenLoading = true;
    try {
      this.klassen = await this.schoolService.getKlassenBySchool(schoolId);
    } catch (error) {
      this.uiToastService.error("Fout bij het laden van klassen.");
      this.klassen = [];
    } finally {
      this.klassenLoading = false;
    }
  }

  toggleKlasSelection(id: number) {
    if (this.selectedKlassenIds.has(id)) {
      this.selectedKlassenIds.delete(id);
    } else {
      this.selectedKlassenIds.add(id);
    }
  }

  async loadBooks() {
    this.isLoading = true;
    try {
      const schoolId = this.schoolService.getSelectedSchoolId() ?? undefined;
      this.books = (await this.bookService.getBooks(schoolId)) as Book[];
      const pageValues = this.books
        .map((book) => book.paginas)
        .filter((paginas): paginas is number => typeof paginas === "number");

      if (pageValues.length > 0) {
        this.minAvailablePages = Math.max(
          this.minPageFilterLimit,
          Math.min(...pageValues),
        );
        this.maxAvailablePages = Math.max(
          this.minAvailablePages,
          Math.min(this.maxPageFilterLimit, Math.max(...pageValues)),
        );
      } else {
        this.minAvailablePages = this.minPageFilterLimit;
        this.maxAvailablePages = this.maxPageFilterLimit;
      }

      this.minPages = this.minAvailablePages;
      this.maxPages = this.maxAvailablePages;
      this.currentPage = 1;
    } catch (error) {
      this.uiToastService.error("Fout bij het laden van boeken.");
    } finally {
      this.isLoading = false;
    }
  }

  onSearch() {
    this.currentPage = 1;
    this.searchQuery = this.searchInput.trim();
  }

  onGenreChange() {
    if (this.selectedGenre !== "Non-fictie algemeen") {
      this.selectedNonFictionSubgenre = "";
    }
    if (this.selectedGenre !== "Didactiek") {
      this.selectedDidacticSubgenre = "";
    }
  }

  onMinPagesChange(value: string) {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;
    this.minPages = Math.min(parsed, this.maxPages);
  }

  onMaxPagesChange(value: string) {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;
    this.maxPages = Math.max(parsed, this.minPages);
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
    this.minPages = this.minAvailablePages;
    this.maxPages = this.maxAvailablePages;
    this.currentPage = 1;
  }

  applyFilters() {
    this.searchQuery = this.searchInput.trim();
    this.currentPage = 1;
  }

  get filteredKlassen(): KlasListItem[] {
    const query = this.klasSearchQuery.trim().toLowerCase();
    if (!query) {
      return this.klassen;
    }
    return this.klassen.filter((klas) =>
      (klas.naam || "").toLowerCase().includes(query),
    );
  }

  get filteredBooks(): Book[] {
    const query = this.searchQuery.trim().toLowerCase();
    return this.books.filter((book) => {
      const matchesQuery =
        !query ||
        (book.titel || "").toLowerCase().includes(query) ||
        (book.auteur || "").toLowerCase().includes(query);

      const matchesGenre =
        !this.selectedGenre ||
        (book.genre || "").toLowerCase() === this.selectedGenre.toLowerCase();

      const matchesLanguage =
        !this.selectedLanguage ||
        (book.taal || "").toLowerCase() === this.selectedLanguage.toLowerCase();

      const matchesLeesniveau =
        !this.selectedLeesniveau ||
        (book.leesniveau || "").toLowerCase() ===
          this.selectedLeesniveau.toLowerCase();

      const averageRating = Number((book as any).averageRating ?? 0);
      const minRating = Number(this.selectedMinAverageRating || 0);
      const matchesMinRating =
        !this.selectedMinAverageRating || averageRating >= minRating;

      const pages = typeof book.paginas === "number" ? book.paginas : null;
      const matchesPages =
        pages === null || (pages >= this.minPages && pages <= this.maxPages);

      const subgenre = ((book as any).subgenre || "").toLowerCase();
      const matchesNonFictionSubgenre =
        !this.selectedNonFictionSubgenre ||
        subgenre === this.selectedNonFictionSubgenre.toLowerCase();
      const matchesDidacticSubgenre =
        !this.selectedDidacticSubgenre ||
        subgenre === this.selectedDidacticSubgenre.toLowerCase();

      return (
        matchesQuery &&
        matchesGenre &&
        matchesLanguage &&
        matchesLeesniveau &&
        matchesMinRating &&
        matchesPages &&
        matchesNonFictionSubgenre &&
        matchesDidacticSubgenre
      );
    });
  }

  get pagedBooks(): Book[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredBooks.slice(start, start + this.pageSize);
  }

  toggleBookSelection(id: number) {
    if (this.selectedBookIds.has(id)) {
      this.selectedBookIds.delete(id);
    } else {
      this.selectedBookIds.add(id);
    }
  }

  get isBookSelectionEmpty(): boolean {
    return this.selectedBookIds.size === 0;
  }

  goToConfirmation() {
    if (this.selectedKlassenIds.size === 0) {
      this.uiToastService.error("Selecteer minstens één klas.");
      return;
    }
    if (this.isBookSelectionEmpty) {
      this.uiToastService.error("Selecteer minstens één boek.");
      return;
    }
    this.step = "bevestiging";
  }

  async saveLeeslijst() {
    this.isSaving = true;
    try {
      await this.bookService.createLeeslijst(
        this.leeslijstTitel,
        this.leeslijstDescription,
        Array.from(this.selectedBookIds),
        Array.from(this.selectedKlassenIds),
      );

      this.uiToastService.success("Leeslijst succesvol aangemaakt.");
      this.router.navigate(["/mijn-lijsten", { fragment: "klasleeslijst" }]);
    } catch (error) {
      this.uiToastService.error("Fout bij het opslaan van de leeslijst.");
      console.error(error);
    } finally {
      this.isSaving = false;
    }
  }

  goBack() {
    if (this.step === "boeken") {
      this.step = "titel";
    } else if (this.step === "bevestiging") {
      this.step = "boeken";
    }
  }

  getSelectedKlassenNames(): string {
    return this.klassen
      .filter((k) => this.selectedKlassenIds.has(k.id))
      .map((k) => k.naam)
      .join(", ");
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredBooks.length / this.pageSize));
  }

  setPage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.loadBooks();
  }
}
