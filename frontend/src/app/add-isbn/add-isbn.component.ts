import { Component } from "@angular/core";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

@Component({
    selector: "app-add-isbn",
    templateUrl: "./add-isbn.component.html",
    styleUrls: ["./add-isbn.component.css"],
    standalone: false
})
export class AddIsbnComponent {
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  isbn = "";
  isLoading = false;
  isImporting = false;
  isAlreadyInLibrary = false;
  hasCheckedLibraryStatus = false;
  errorMessage = "";
  successMessage = "";
  book: any = null;

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {
    void this.loadSchools();
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

  onSchoolChange(value: string) {
    this.selectedSchoolId = value ? Number(value) : null;
    if (this.selectedSchoolId !== null) {
      this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    }
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

    try {
      this.book = await this.bookService.fetchBookByIsbn(trimmed);
      this.isAlreadyInLibrary = await this.bookService.isBookInLibrary(
        trimmed,
        this.selectedSchoolId ?? undefined,
      );
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
      this.book = savedBook;
      this.isAlreadyInLibrary = true;
      this.hasCheckedLibraryStatus = true;
      this.successMessage = "Boek toegevoegd aan bibliotheek.";
    } catch (err: any) {
      if (err?.response?.status === 404) {
        this.errorMessage = "Boek niet gevonden om te importeren.";
      } else {
        this.errorMessage =
          "Er ging iets mis bij het toevoegen aan de bibliotheek.";
      }
    } finally {
      this.isImporting = false;
    }
  }
}
