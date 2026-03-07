import { Component } from "@angular/core";
import { BookService } from "../services/book.service";

@Component({
  selector: "app-add-isbn",
  templateUrl: "./add-isbn.component.html",
  styleUrls: ["./add-isbn.component.css"],
})
export class AddIsbnComponent {
  isbn = "";
  isLoading = false;
  isImporting = false;
  isAlreadyInLibrary = false;
  hasCheckedLibraryStatus = false;
  errorMessage = "";
  successMessage = "";
  book: any = null;

  constructor(private bookService: BookService) {}

  async zoekBoek() {
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

    try {
      this.book = await this.bookService.fetchBoekByIsbn(trimmed);
      this.isAlreadyInLibrary = await this.bookService.bestaatBoekInBibliotheek(trimmed);
      this.hasCheckedLibraryStatus = true;
      if (this.isAlreadyInLibrary) {
        this.successMessage = "Reeds in de bibliotheek.";
      }
    } catch (err: any) {
      if (err?.response?.status === 404) {
        this.errorMessage = "Geen boek gevonden voor dit ISBN-nummer.";
      } else {
        this.errorMessage = "Er ging iets mis bij het ophalen van het boek.";
      }
    } finally {
      this.isLoading = false;
    }
  }

  async voegToeAanBibliotheek() {
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
      const savedBook = await this.bookService.importBoekByIsbn(isbnToImport);
      this.book = savedBook;
      this.isAlreadyInLibrary = true;
      this.hasCheckedLibraryStatus = true;
      this.successMessage = "Boek toegevoegd aan bibliotheek.";
    } catch (err: any) {
      if (err?.response?.status === 404) {
        this.errorMessage = "Boek niet gevonden om te importeren.";
      } else {
        this.errorMessage = "Er ging iets mis bij het toevoegen aan de bibliotheek.";
      }
    } finally {
      this.isImporting = false;
    }
  }
}
