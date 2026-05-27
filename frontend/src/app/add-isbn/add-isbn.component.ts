import { Component } from "@angular/core";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { LoanService } from "../services/loan.service";

@Component({
  selector: "app-add-isbn",
  templateUrl: "./add-isbn.component.html",
  styleUrls: ["./add-isbn.component.css"],
  standalone: false,
})
export class AddIsbnComponent {
  selectedSchoolId: number | null = null;
  isbn = "";
  aantalExemplaren: number = 1;
  copyCondition: "GOOD" | "MODERATE" | "BAD" = "GOOD";
  readonly copyConditions = ["GOOD", "MODERATE", "BAD"] as const;
  isLoading = false;
  isImporting = false;
  isAlreadyInLibrary = false;
  hasCheckedLibraryStatus = false;
  errorMessage = "";
  successMessage = "";
  book: any = null;
  copiesTotalCount: number = 0;

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
    private loanService: LoanService,
  ) {
    void this.loadUserSchool();
  }

  private async loadUserSchool() {
    await this.schoolService.selectUserDefaultSchool();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId();
  }

  async searchBook() {
    const trimmed = this.isbn.trim();
    if (!trimmed) {
      this.errorMessage = "Voer een ISBN-nummer in.";
      return;
    }
    this.isLoading = true;
    this.errorMessage = "";
    this.successMessage = "";
    this.isAlreadyInLibrary = false;
    this.hasCheckedLibraryStatus = false;
    this.book = null;
    this.copiesTotalCount = 0;
    try {
      // First try to fetch from library (database)
      const libraryBook = await this.bookService.getBookByIsbnFromLibrary(
        trimmed,
        this.selectedSchoolId ?? undefined,
      );

      if (libraryBook) {
        // Book exists in library
        this.book = libraryBook;
        this.isAlreadyInLibrary = true;
        // Get copy count
        const summary = await this.loanService.getCopySummary(libraryBook.id);
        this.copiesTotalCount = summary.total;
        this.successMessage = `Reeds in de bibliotheek: ${this.copiesTotalCount} exemplaren.`;
      } else {
        // Book not in library, fetch from OpenLibrary
        this.book = await this.bookService.fetchBookByIsbn(trimmed);
        this.isAlreadyInLibrary = false;
      }
      this.hasCheckedLibraryStatus = true;
    } catch (err: any) {
      if (err?.response?.status === 400) {
        this.errorMessage = "Ongeldig ISBN-nummer.";
      } else if (err?.response?.status === 404) {
        this.errorMessage = "Geen boek gevonden voor dit ISBN-nummer.";
      } else {
        this.errorMessage = "Er ging iets mis bij het ophalen van het boek.";
      }
    } finally {
      this.isLoading = false;
    }
  }

  async addToLibrary() {
    if (this.isAlreadyInLibrary) {
      this.successMessage = "Reeds in de bibliotheek.";
      return;
    }
    const isbnToImport = (this.book?.isbn || this.isbn).trim();
    if (!isbnToImport) {
      this.errorMessage = "Geen ISBN beschikbaar om toe te voegen.";
      return;
    }
    this.isImporting = true;
    this.errorMessage = "";
    this.successMessage = "";
    try {
      const savedBook = await this.bookService.importBookByIsbn(
        isbnToImport,
        this.selectedSchoolId ?? undefined,
      );
      console.log("savedBook:", savedBook);

      if (savedBook?.id && this.aantalExemplaren > 0) {
        const promises = Array.from({ length: this.aantalExemplaren }, () =>
          this.loanService.addCopy(savedBook.id, this.copyCondition),
        );
        await Promise.all(promises);
      }

      this.book = savedBook;
      this.isAlreadyInLibrary = true;
      this.hasCheckedLibraryStatus = true;
      this.successMessage = `Boek toegevoegd met ${this.aantalExemplaren} exemplaar/exemplaren.`;
    } catch (err: any) {
      if (err?.response?.status === 400) {
        this.errorMessage = "Ongeldig ISBN-nummer.";
      } else if (err?.response?.status === 404) {
        this.errorMessage = "Boek niet gevonden om te importeren.";
      } else {
        this.errorMessage =
          "Er ging iets mis bij het toevoegen aan de bibliotheek.";
      }
    } finally {
      this.isImporting = false;
    }
  }

  async addCopiesToExisting() {
    if (!this.isAlreadyInLibrary || !this.book?.id) {
      this.errorMessage = "Kan geen exemplaren toevoegen.";
      return;
    }

    if (this.aantalExemplaren < 1) {
      this.errorMessage = "Voer een geldig aantal exemplaren in.";
      return;
    }

    this.isImporting = true;
    this.errorMessage = "";
    this.successMessage = "";

    try {
      const promises = Array.from({ length: this.aantalExemplaren }, () =>
        this.loanService.addCopy(this.book.id, this.copyCondition),
      );
      await Promise.all(promises);

      // Refresh copy count
      const summary = await this.loanService.getCopySummary(this.book.id);
      this.copiesTotalCount = summary.total;

      this.successMessage = `${this.aantalExemplaren} exemplaar(en) toegevoegd. Totaal: ${this.copiesTotalCount} exemplaren.`;
      this.aantalExemplaren = 1; // Reset to default
    } catch (err: any) {
      this.errorMessage =
        "Er ging iets mis bij het toevoegen van de exemplaren.";
      console.error("Error adding copies:", err);
    } finally {
      this.isImporting = false;
    }
  }
}
