import { Component, OnInit } from "@angular/core";
import { NgForm } from "@angular/forms";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { AdminGenreService, Genre } from "../services/admin-genre.service";

export enum Language {
  Nederlands = "Nederlands",
  Engels = "Engels",
  Frans = "Frans",
  Duits = "Duits",
  Spaans = "Spaans",
  Italiaans = "Italiaans",
  Portugees = "Portugees",
  Latijn = "Latijn",
}
export const LEESNIVEAUS = ["A", "B", "C", "D"] as const;

@Component({
  selector: "app-add-book",
  templateUrl: "./add-book-component.html",
  styleUrls: ["./add-book-component.css"],
  standalone: false,
})
export class AddBookComponent implements OnInit {
  readonly languages = Object.values(Language);
  readonly leesniveaus = LEESNIVEAUS;
  didacticSubgenres: string[] = [];

  // Dynamische genres van de API (excl. Didactiek)
  allGenres: Genre[] = []; // Raw hierarchical genres from API
  availableNonDidacticGenres: string[] = []; // Flattened list of non-didactic genres

  // Didactiek
  isDidacticMode = false;
  selectedNonDidacticGenres: string[] = [];
  selectedDidacticSubgenres: string[] = [];

  selectedSchoolId: number | null = null;
  selectedCoverFile: File | null = null;
  coverPreviewUrl: string | null = null;
  goNumberLookup = "";
  isLookupLoading = false;
  isSaving = false;
  isSubmitted = false;
  submitMessage = "";
  submitState: "success" | "error" | "" = "";
  aantalExemplaren: number = 1;

  book = {
    titel: "",
    auteur: "",
    isbn: "",
    goNumber: "",
    cover: "",
    beschrijving: "",
    genres: [], // Changed from singular 'genre' to plural 'genres'
    uitgaveDatum: "",
    paginas: null as number | null,
    taal: "" as Language | "",
    uitgeverij: "",
    leesniveau: "",
  };

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
    private genreService: AdminGenreService,
  ) {}

  async ngOnInit() {
    await this.schoolService.selectUserDefaultSchool();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId();
    this.loadGenres();
  }

  loadGenres(): void {
    this.genreService.getAll().subscribe({
      next: (genres) => {
        this.allGenres = genres; // Store the hierarchical structure
        // Populate didacticSubgenres for the didactic mode
        const did = genres.find((g) => g.naam.toLowerCase() === "didactiek");
        if (did) {
          this.didacticSubgenres = did.subgenres.map((s) => s.naam).sort();
        }
        // Populate availableNonDidacticGenres for the non-didactic multi-select
        this.availableNonDidacticGenres = this.flattenNonDidacticGenres(genres);
        // No need to restore genres here for add-book, as it's a new book
      },
      error: () => {},
    });
  }

  private flattenNonDidacticGenres(genres: Genre[]): string[] {
    const flattened: string[] = [];
    for (const g of genres) {
      if (g.naam.toLowerCase() === "didactiek") continue; // Skip didactic parent
      flattened.push(g.naam); // Add top-level genre
      for (const sg of g.subgenres) {
        flattened.push(`${g.naam} - ${sg.naam}`); // Add subgenre as "Parent - Sub"
      }
    }
    return flattened.sort();
  }

  toggleDidactic(state: boolean): void {
    this.isDidacticMode = state;
    this.selectedNonDidacticGenres = [];
    this.selectedDidacticSubgenres = [];
  }

  /** Berekent de genre-string om op te slaan */
  private buildGenreArray(): string[] {
    if (this.isDidacticMode) {
      if (this.selectedDidacticSubgenres.length > 0) {
        return this.selectedDidacticSubgenres.map((sg) => `Didactiek - ${sg}`);
      }
      return ["Didactiek"]; // If didactic mode is on but no subgenres selected
    } else {
      return this.selectedNonDidacticGenres;
    }
  }

  private restoreGenresToSelection(genres: string[]): void {
    this.selectedNonDidacticGenres = [];
    this.selectedDidacticSubgenres = [];
    this.isDidacticMode = false;

    if (!genres || genres.length === 0) return;

    // For add-book, we typically don't restore genres from an existing book,
    // but this method is kept for consistency if a book object with genres is pre-filled.
    // The logic here would be similar to edit-book.component.ts
  }

  onCoverSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    if (this.coverPreviewUrl) URL.revokeObjectURL(this.coverPreviewUrl);
    this.selectedCoverFile = file;
    if (file) this.coverPreviewUrl = URL.createObjectURL(file);
  }

  private toBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(new Error("Kon cover niet lezen"));
      reader.readAsDataURL(file);
    });
  }

  async onSubmit(bookForm: NgForm) {
    this.submitState = "";
    this.submitMessage = "";
    this.isSubmitted = true;
    if (bookForm.invalid) return;
    if (this.selectedSchoolId === null) {
      this.submitState = "error";
      this.submitMessage = "Kies een school.";
      return;
    }
    this.isSaving = true;
    try {
      const coverData = this.selectedCoverFile
        ? await this.toBase64(this.selectedCoverFile)
        : this.book.cover;

      const payload = {
        ...this.book,
        cover: coverData,
        genres: this.buildGenreArray(),
        genre: undefined, // Remove old property if it leaked in
      } as any;

      await this.bookService.addBook(payload, this.selectedSchoolId);
      this.resetForm(bookForm);
      this.submitState = "success";
      this.submitMessage = "Boek succesvol toegevoegd aan de bibliotheek.";
    } catch {
      this.submitState = "error";
      this.submitMessage = "Fout bij opslaan. Controleer de verbinding.";
    } finally {
      this.isSaving = false;
    }
  }

  async loadBookFromGoNumber(): Promise<void> {
    const trimmed = this.goNumberLookup.trim().toUpperCase();
    if (!trimmed) {
      this.submitState = "error";
      this.submitMessage = "Voer een GO-nummer in.";
      return;
    }
    this.isLookupLoading = true;
    this.submitState = "";
    this.submitMessage = "";
    try {
      const book = (await this.bookService.fetchBookByGoNumber(trimmed)) as any;
      this.book = {
        titel: book.titel || "",
        auteur: book.auteur || "",
        isbn: book.isbn || "",
        goNumber: book.goNumber || trimmed,
        cover: book.cover || "",
        beschrijving: book.beschrijving || "",
        genres: book.genres || (book.genre ? [book.genre] : []),
        uitgaveDatum: book.uitgaveDatum || "",
        paginas: book.paginas ?? null,
        taal: book.taal || "",
        uitgeverij: book.uitgeverij || "",
        leesniveau: book.leesniveau || "",
      };
      this.restoreGenresToSelection(this.book.genres);
      if (this.coverPreviewUrl) URL.revokeObjectURL(this.coverPreviewUrl);
      this.coverPreviewUrl = this.book.cover || null;
      this.selectedCoverFile = null;
      this.submitState = "success";
      this.submitMessage = `Boekgegevens geladen voor ${trimmed}.`;
    } catch (error: any) {
      this.submitState = "error";
      this.submitMessage =
        error?.response?.status === 404
          ? "Geen boek gevonden met dit GO-nummer."
          : "Fout bij het ophalen van het boek.";
    } finally {
      this.isLookupLoading = false;
    }
  }

  private resetForm(bookForm: NgForm) {
    this.book = {
      titel: "",
      auteur: "",
      isbn: "",
      goNumber: "",
      cover: "",
      beschrijving: "",
      genres: [],
      uitgaveDatum: "",
      paginas: null as number | null,
      taal: "" as Language | "",
      uitgeverij: "",
      leesniveau: "",
    };
    this.isDidacticMode = false;
    this.selectedNonDidacticGenres = [];
    this.selectedDidacticSubgenres = [];
    this.aantalExemplaren = 1;
    this.selectedCoverFile = null;
    this.goNumberLookup = "";
    if (this.coverPreviewUrl) URL.revokeObjectURL(this.coverPreviewUrl);
    this.coverPreviewUrl = null;
    bookForm.resetForm();
    this.isSubmitted = false;
  }
}
