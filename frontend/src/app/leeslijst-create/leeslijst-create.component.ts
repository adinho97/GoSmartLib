import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { SchoolService, KlasListItem } from "../services/school.service";
import { BookService, PagedBooksResponse } from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";
import { Book } from "../models/book";

type Step = "boeken" | "bevestiging";

@Component({
  selector: "app-leeslijst-create",
  templateUrl: "./leeslijst-create.component.html",
  styleUrls: ["./leeslijst-create.component.css"],
  standalone: false,
})
export class LeeslijstCreateComponent implements OnInit {
  step: Step = "boeken";
  isLoading = false;
  isSaving = false;
  klassenLoading = false;

  // Klassen from database
  klassen: KlasListItem[] = [];
  selectedKlassenIds = new Set<number>();

  // Step 1: Boeken
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
    await this.loadKlassen(schoolId);
  }

  async loadKlassen(schoolId: number) {
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
    if (this.isBookSelectionEmpty) {
      this.uiToastService.error("Selecteer minstens één boek.");
      return;
    }
    this.step = "bevestiging";
  }

  async saveLeeslijst() {
    this.isSaving = true;
    try {
      // Add each selected book to the class reading list
      const bookIds = Array.from(this.selectedBookIds);
      for (const bookId of bookIds) {
        await this.bookService.toggleClassReadingListItem(bookId);
      }
      this.uiToastService.success("Leeslijst succesvol aangemaakt.");
      this.router.navigate(["/mijn-lijsten", { fragment: "klasleeslijst" }]);
    } catch (error) {
      this.uiToastService.error("Fout bij het opslaan van de leeslijst.");
    } finally {
      this.isSaving = false;
    }
  }

  goBack() {
    if (this.step === "boeken") {
      this.step = "boeken";
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
