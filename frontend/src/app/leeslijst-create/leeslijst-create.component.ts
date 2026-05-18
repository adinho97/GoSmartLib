import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Router } from "@angular/router";
import { SchoolService, KlasListItem } from "../services/school.service";
import { BookService } from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";
import { Book } from "../models/book";

interface User {
  sub: string;
  displayName: string;
}

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

  // Years (jaren) selection
  jaren: number[] = [1, 2, 3, 4, 5, 6];
  selectedJaren = new Set<number>();

  // Sharing logic
  isGlobal = false;
  userSearchQuery = "";
  foundUsers: User[] = [];
  selectedUsers: User[] = [];
  sharedWithUserSubs = new Set<string>();

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
  editingLeeslijstId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private schoolService: SchoolService,
    private bookService: BookService,
    private uiToastService: UiToastService,
    private router: Router,
  ) {}

  get isEditMode(): boolean {
    return this.editingLeeslijstId !== null;
  }

  async ngOnInit() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.uiToastService.error("Geen school geselecteerd.");
      this.router.navigate(["/dashboard"]);
      return;
    }

    const leeslijstIdParam = this.route.snapshot.paramMap.get("id");
    if (leeslijstIdParam) {
      this.editingLeeslijstId = parseInt(leeslijstIdParam, 10);
      await this.loadExistingLeeslijst(this.editingLeeslijstId);
    }
  }

  private async loadExistingLeeslijst(id: number) {
    this.isLoading = true;
    try {
      const existing = await this.bookService.getLeeslijst(id);
      this.leeslijstTitel = existing?.titel || "";
      this.leeslijstDescription = existing?.description || "";
      this.selectedBookIds = new Set<number>(
        Array.isArray(existing?.books)
          ? existing.books
              .map((book: any) => Number(book.bookId))
              .filter((bookId: number) => Number.isFinite(bookId))
          : [],
      );

      await this.loadKlassen();

      const klasIds = Array.isArray(existing?.klasIds)
        ? existing.klasIds
            .map((idValue: any) => Number(idValue))
            .filter((idValue: number) => Number.isFinite(idValue))
        : [];
      if (klasIds.length > 0) {
        this.selectedKlassenIds = new Set<number>(klasIds);
      } else if (Array.isArray(existing?.klasNames)) {
        const idsFromNames = this.klassen
          .filter((klas) => existing.klasNames.includes(klas.naam))
          .map((klas) => klas.id);
        this.selectedKlassenIds = new Set<number>(idsFromNames);
      }

      this.isGlobal = !!existing?.isGlobal;
      if (Array.isArray(existing?.sharedWithUsers)) {
        this.selectedUsers = existing.sharedWithUsers.map((u: any) => ({
          sub: u.sub,
          displayName: u.displayName || u.username,
        }));
        this.sharedWithUserSubs = new Set(this.selectedUsers.map((u) => u.sub));
      }
    } catch (error: any) {
      if (error?.response?.status === 403) {
        this.uiToastService.error(
          "Je kan enkel je eigen leeslijsten aanpassen.",
        );
      } else {
        this.uiToastService.error("Fout bij het laden van de leeslijst.");
      }
      this.router.navigate(["/mijn-lijsten", { fragment: "klasleeslijst" }]);
    } finally {
      this.isLoading = false;
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
    this.loadAllUsers();
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

  toggleYearSelection(year: number) {
    if (this.selectedJaren.has(year)) {
      this.selectedJaren.delete(year);
      // Remove all classes from this year
      this.klassen.forEach((klas) => {
        const klasYear = parseInt(klas.naam.charAt(0), 10);
        if (klasYear === year) {
          this.selectedKlassenIds.delete(klas.id);
        }
      });
    } else {
      this.selectedJaren.add(year);
      // Auto-select all classes from this year
      this.klassen.forEach((klas) => {
        const klasYear = parseInt(klas.naam.charAt(0), 10);
        if (klasYear === year) {
          this.selectedKlassenIds.add(klas.id);
        }
      });
    }
  }

  getKlasenForYear(year: number): KlasListItem[] {
    return this.klassen.filter((klas) => {
      const klasYear = parseInt(klas.naam.charAt(0), 10);
      return klasYear === year;
    });
  }

  async loadAllUsers() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    try {
      // Pass empty string to get all users
      this.foundUsers = await this.bookService.searchUsers("", schoolId);
    } catch (error) {
      console.error("Failed to load users", error);
    }
  }

  async onUserSearch() {
    const query = this.userSearchQuery.trim();

    // If query is empty, show all users
    if (query.length === 0) {
      await this.loadAllUsers();
      return;
    }

    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    try {
      this.foundUsers = await this.bookService.searchUsers(query, schoolId);
    } catch (error) {
      console.error("User search failed", error);
    }
  }

  toggleUserSelection(user: User) {
    if (this.sharedWithUserSubs.has(user.sub)) {
      this.sharedWithUserSubs.delete(user.sub);
      this.selectedUsers = this.selectedUsers.filter((u) => u.sub !== user.sub);
    } else {
      this.sharedWithUserSubs.add(user.sub);
      this.selectedUsers.push(user);
    }
  }

  onGlobalToggle() {
    // Logic handled in template, but can add side-effects here
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
    const hasTarget =
      this.isGlobal ||
      this.selectedKlassenIds.size > 0 ||
      this.sharedWithUserSubs.size > 0;

    if (!hasTarget) {
      this.uiToastService.error(
        "Selecteer een doelgroep (klas, gebruiker of globaal).",
      );
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
      if (this.isEditMode && this.editingLeeslijstId !== null) {
        await this.bookService.updateLeeslijst(
          this.editingLeeslijstId,
          this.leeslijstTitel,
          this.leeslijstDescription,
          Array.from(this.selectedBookIds),
          Array.from(this.selectedKlassenIds),
          this.isGlobal,
          Array.from(this.sharedWithUserSubs),
        );
        this.uiToastService.success("Leeslijst succesvol aangepast.");
      } else {
        await this.bookService.createLeeslijst(
          this.leeslijstTitel,
          this.leeslijstDescription,
          Array.from(this.selectedBookIds),
          Array.from(this.selectedKlassenIds),
          this.isGlobal,
          Array.from(this.sharedWithUserSubs),
        );
        this.uiToastService.success("Leeslijst succesvol aangemaakt.");
      }

      this.router.navigate(["/mijn-lijsten", { fragment: "klasleeslijst" }]);
    } catch (error: any) {
      if (error?.response?.status === 403) {
        this.uiToastService.error(
          "Je kan enkel je eigen leeslijsten aanpassen.",
        );
      } else {
        this.uiToastService.error("Fout bij het opslaan van de leeslijst.");
      }
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

  getSelectedUserNames(): string {
    return this.selectedUsers.map((u) => u.displayName).join(", ");
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredBooks.length / this.pageSize));
  }

  setPage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
  }
}
