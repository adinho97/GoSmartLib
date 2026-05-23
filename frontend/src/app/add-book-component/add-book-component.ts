import { Component, OnInit } from "@angular/core";
import { NgForm } from "@angular/forms";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { AdminGenreService, Genre } from "../services/admin-genre.service";
import { SettingsService } from "../services/settings.service";

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

// Didactiek blijft hard-coded en apart
const DIDACTIC_SUBGENRES = [
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

@Component({
  selector: "app-add-book",
  templateUrl: "./add-book-component.html",
  styleUrls: ["./add-book-component.css"],
  standalone: false,
})
export class AddBookComponent implements OnInit {
  readonly languages = Object.values(Language);
  leesniveaus: string[] = [];
  didacticSubgenres = [...DIDACTIC_SUBGENRES];

  // Dynamische genres van de API (excl. Didactiek)
  genres: Genre[] = [];
  allGenres: Genre[] = []; // Raw hierarchical genres from API
  selectedGenreId: number | null = null;
  selectedSubgenreId: number | null = null;
  isDidactic = false;
  selectedDidacticSubgenre = "";
  availableNonDidacticGenres: string[] = []; // Flattened list of non-didactic genres
  showGenreDropdown = false;
  showDidacticDropdown = false;

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
    genres: [] as string[],
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
    private settingsService: SettingsService,
  ) {}

  async ngOnInit() {
    await this.schoolService.selectUserDefaultSchool();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId();
    this.loadGenres();
    this.loadReadingLevels();
  }

  loadReadingLevels(): void {
    if (!this.selectedSchoolId) return;
    this.settingsService.getSettings(this.selectedSchoolId).subscribe({
      next: (settings) => {
        // Filter alleen actieve niveaus en gebruik de door de school ingestelde codes
        this.leesniveaus = settings.levels
          .filter((l) => l.active)
          .map((l) => l.code);

        const defaultLvl = settings.levels.find((l) => l.active && l.isDefault);
        if (defaultLvl && !this.book.leesniveau)
          this.book.leesniveau = defaultLvl.code;
      },
    });
  }

  loadGenres(): void {
    this.genreService.getAll().subscribe({
      next: (genres) => {
        this.genres = genres;
        this.allGenres = genres; // Store the hierarchical structure
        // Populate didacticSubgenres for the didactic mode
        const did = genres.find((g) => g.naam.toLowerCase() === "didactiek");
        if (did) {
          this.didacticSubgenres = did.subgenres.map((s: any) => s.naam).sort();
        }
        // Populate availableNonDidacticGenres for the non-didactic multi-select
        this.availableNonDidacticGenres = this.flattenNonDidacticGenres(genres);
        // No need to restore genres here for add-book, as it's a new book
      },
      error: () => {},
    });
  }

  get selectedGenre(): Genre | null {
    return (
      this.genres.find((g: Genre) => g.id === this.selectedGenreId) ?? null
    );
  }

  get hasSubgenres(): boolean {
    return (this.selectedGenre?.subgenres.length ?? 0) > 0;
  }

  onGenreChange(): void {
    this.selectedSubgenreId = null;
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

  isSelected(genre: string): boolean {
    return this.selectedNonDidacticGenres.includes(genre);
  }

  toggleGenre(genre: string): void {
    const index = this.selectedNonDidacticGenres.indexOf(genre);
    if (index >= 0) {
      this.selectedNonDidacticGenres.splice(index, 1);
    } else {
      this.selectedNonDidacticGenres.push(genre);
    }
  }

  isDidacticSubgenreSelected(sub: string): boolean {
    return this.selectedDidacticSubgenres.includes(sub);
  }

  toggleDidacticSubgenre(sub: string): void {
    const index = this.selectedDidacticSubgenres.indexOf(sub);
    if (index >= 0) {
      this.selectedDidacticSubgenres.splice(index, 1);
    } else {
      this.selectedDidacticSubgenres.push(sub);
    }
  }

  private restoreGenresToSelection(genres: string[]): void {
    this.selectedNonDidacticGenres = [];
    this.selectedDidacticSubgenres = [];
    this.isDidacticMode = false;

    if (!genres || genres.length === 0) return;

    const didacticGenre = genres.find((g) =>
      g.toLowerCase().startsWith("didactiek"),
    );
    if (didacticGenre) {
      this.isDidacticMode = true;
      this.selectedDidacticSubgenres = genres
        .filter((g) => g.toLowerCase().startsWith("didactiek"))
        .map((g) => {
          const parts = g.split(" - ");
          return parts.length > 1 ? parts.slice(1).join(" - ").trim() : "";
        })
        .filter((s) => s !== "");
    } else {
      this.selectedNonDidacticGenres = [...genres];
    }
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
        genre: book.genre || "",
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
            (s: any) => s.naam.toLowerCase() === remainder.toLowerCase(),
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
      genres: [],
      genre: "",
      uitgaveDatum: "",
      paginas: null,
      taal: "",
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
