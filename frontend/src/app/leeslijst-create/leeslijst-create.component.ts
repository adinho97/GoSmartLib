import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { SchoolService, KlasListItem } from "../services/school.service";
import { BookService, PagedBooksResponse } from "../services/book.service";
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
  selectedKlassenIds = new Set<number>();

  // Step 2: Boeken
  books: Book[] = [];
  selectedBookIds = new Set<number>();
  searchQuery = "";
  currentPage = 1;
  pageSize = 10;
  totalBooks = 0;

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
      const response: PagedBooksResponse = await this.bookService.getBooksPage(
        this.currentPage - 1,
        this.pageSize,
        this.searchQuery,
      );
      this.books = response.items;
      this.totalBooks = response.total;
    } catch (error) {
      this.uiToastService.error("Fout bij het laden van boeken.");
    } finally {
      this.isLoading = false;
    }
  }

  onSearch() {
    this.currentPage = 1;
    this.loadBooks();
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
    return Math.ceil(this.totalBooks / this.pageSize);
  }

  setPage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.loadBooks();
  }
}
