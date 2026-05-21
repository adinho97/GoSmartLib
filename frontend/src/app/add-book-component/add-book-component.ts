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
  genres: Genre[] = [];

  // Geselecteerde genre + subgenre voor normale boeken
  selectedGenreId: number | null = null;
  selectedSubgenreId: number | null = null;

  // Didactiek
  isDidactic = false;
  selectedDidacticSubgenre = "";

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
    genre: "",
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
        const did = genres.find((g) => g.naam.toLowerCase() === "didactiek");
        if (did) {
          this.didacticSubgenres = did.subgenres.map((s) => s.naam).sort();
        }
        this.genres = genres.filter(
          (g) => g.naam.toLowerCase() !== "didactiek",
        );
      },
      error: () => {},
    });
  }

  get selectedGenre(): Genre | null {
    return this.genres.find((g) => g.id === this.selectedGenreId) ?? null;
  }

  get hasSubgenres(): boolean {
    return (this.selectedGenre?.subgenres.length ?? 0) > 0;
  }

  onGenreChange(): void {
    this.selectedSubgenreId = null;
  }

  toggleDidactic(state: boolean): void {
    this.isDidactic = state;
    this.selectedGenreId = null;
    this.selectedSubgenreId = null;
    this.selectedDidacticSubgenre = "";
  }

  /** Berekent de genre-string om op te slaan */
  private buildGenreString(): string {
    if (this.isDidactic) {
      return this.selectedDidacticSubgenre
        ? `Didactiek - ${this.selectedDidacticSubgenre}`
        : "Didactiek";
    }
    if (!this.selectedGenreId) return "";
    const genre = this.selectedGenre;
    if (!genre) return "";
    if (this.selectedSubgenreId) {
      const sub = genre.subgenres.find((s) => s.id === this.selectedSubgenreId);
      return sub ? `${genre.naam} - ${sub.naam}` : genre.naam;
    }
    return genre.naam;
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

      await this.bookService.addBook(
        { ...this.book, cover: coverData, genre: this.buildGenreString() },
        this.selectedSchoolId,
      );
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
      const book = await this.bookService.fetchBookByGoNumber(trimmed);
      this.book = {
        titel: book.titel || "",
        auteur: book.auteur || "",
        isbn: book.isbn || "",
        goNumber: book.goNumber || trimmed,
        cover: book.cover || "",
        beschrijving: book.beschrijving || "",
        genre: book.genre || "",
        uitgaveDatum: book.uitgaveDatum || "",
        paginas: book.paginas ?? null,
        taal: book.taal || "",
        uitgeverij: book.uitgeverij || "",
        leesniveau: book.leesniveau || "",
      };
      this.restoreGenreFromString(book.genre || "");
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

  private restoreGenreFromString(genreStr: string): void {
    this.selectedGenreId = null;
    this.selectedSubgenreId = null;
    this.isDidactic = false;
    this.selectedDidacticSubgenre = "";

    if (!genreStr) return;
    const lower = genreStr.toLowerCase();

    if (lower.startsWith("didactiek")) {
      this.isDidactic = true;
      const parts = genreStr.split(" - ");
      if (parts.length > 1)
        this.selectedDidacticSubgenre = parts.slice(1).join(" - ").trim();
      return;
    }

    // Zoek match in dynamische genres
    for (const genre of this.genres) {
      if (lower.startsWith(genre.naam.toLowerCase())) {
        this.selectedGenreId = genre.id;
        const remainder = genreStr
          .substring(genre.naam.length)
          .replace(/^\s*-\s*/, "")
          .trim();
        if (remainder) {
          const sub = genre.subgenres.find(
            (s) => s.naam.toLowerCase() === remainder.toLowerCase(),
          );
          if (sub) this.selectedSubgenreId = sub.id;
        }
        return;
      }
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
      genre: "",
      uitgaveDatum: "",
      paginas: null,
      taal: "",
      uitgeverij: "",
      leesniveau: "",
    };
    this.isDidactic = false;
    this.selectedGenreId = null;
    this.selectedSubgenreId = null;
    this.selectedDidacticSubgenre = "";
    this.aantalExemplaren = 1;
    this.selectedCoverFile = null;
    this.goNumberLookup = "";
    if (this.coverPreviewUrl) URL.revokeObjectURL(this.coverPreviewUrl);
    this.coverPreviewUrl = null;
    bookForm.resetForm();
    this.isSubmitted = false;
  }
}
