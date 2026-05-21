import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import { AdminGenreService, Genre } from "../services/admin-genre.service";
import { Book } from "../models/book";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";

type CopyView = {
  id: number;
  status: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST";
  condition: "GOOD" | "MODERATE" | "BAD";
  editStatus: "AVAILABLE" | "DAMAGED" | "LOST";
  editCondition: "GOOD" | "MODERATE" | "BAD";
  isUpdating: boolean;
};

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
  selector: "app-edit-book",
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: "./edit-book.component.html",
  styleUrls: ["./edit-book.component.css"],
})
export class EditBookComponent implements OnInit {
  bookId!: number;
  readonly languages = Object.values(Language);
  readonly leesniveaus = LEESNIVEAUS;
  didacticSubgenres: string[] = [];
  availableNonDidacticGenres: string[] = []; // Flattened list of non-didactic genres

  allGenres: Genre[] = []; // Raw hierarchical genres from API
  genresLoaded = false;

  // Geselecteerde waarden
  isDidacticMode = false;
  selectedNonDidacticGenres: string[] = [];
  selectedDidacticSubgenres: string[] = [];

  book: Book = {
    id: 0,
    titel: "",
    auteur: "",
    cover: "",
    beschrijving: "",
    genres: [], // Changed from singular 'genre' to plural 'genres'
    uitgaveDatum: "",
    paginas: 0,
    taal: "",
    uitgeverij: "",
    leesniveau: "",
  };

  isLoading = true;
  errorMessage = "";
  copySummary = { total: 0, available: 0 };
  copies: CopyView[] = [];
  isAddingCopy = false;
  copyMessage = "";
  copyError = "";

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
    private genreService: AdminGenreService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    if (idParam) {
      this.bookId = Number(idParam);
      this.loadCopyData();
      this.loadGenres();
      this.loadBook();
    }
  }

  // ── Genre helpers ──────────────────────────────────────────────────────────

  loadGenres(): void {
    this.genreService.getAll().subscribe({
      next: (genres) => {
        this.allGenres = genres;
        const didacticParent = genres.find(
          (g) => g.naam.toLowerCase() === "didactiek",
        );
        if (didacticParent) {
          this.didacticSubgenres = didacticParent.subgenres
            .map((s) => s.naam)
            .sort();
        }
        this.availableNonDidacticGenres = this.flattenNonDidacticGenres(genres);
        this.genresLoaded = true;
        if (!this.isLoading && this.book.genres) {
          this.restoreGenresToSelection(this.book.genres);
        }
      },
      error: () => {
        this.genresLoaded = true;
      },
    });
  }

  private flattenNonDidacticGenres(genres: Genre[]): string[] {
    const flattened: string[] = [];
    for (const g of genres) {
      if (g.naam.toLowerCase() === "didactiek") continue;
      flattened.push(g.naam);
      for (const sg of g.subgenres) {
        flattened.push(`${g.naam} - ${sg.naam}`);
      }
    }
    return flattened.sort();
  }

  toggleDidactic(state: boolean): void {
    this.isDidacticMode = state;
    this.selectedNonDidacticGenres = [];
    this.selectedDidacticSubgenres = [];
  }

  private buildGenreArray(): string[] {
    if (this.isDidacticMode) {
      if (this.selectedDidacticSubgenres.length > 0) {
        return this.selectedDidacticSubgenres.map((sg) => `Didactiek - ${sg}`);
      }
      return ["Didactiek"];
    } else {
      return this.selectedNonDidacticGenres;
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

  // ── Boek laden ─────────────────────────────────────────────────────────────

  loadBook() {
    this.bookService.getBookById(this.bookId).subscribe({
      next: (data: Book) => {
        this.book = data;
        this.isLoading = false;
        if (this.genresLoaded) {
          this.restoreGenresToSelection(data.genres || []);
        }
      },
      error: () => {
        this.errorMessage = "Kon het boek niet laden.";
        this.isLoading = false;
      },
    });
  }

  // ── Exemplaren ─────────────────────────────────────────────────────────────

  async loadCopySummary(): Promise<void> {
    try {
      this.copySummary = await this.loanService.getCopySummary(this.bookId);
    } catch {
      this.copySummary = { total: 0, available: 0 };
    }
  }

  async loadCopyData(): Promise<void> {
    await Promise.all([this.loadCopySummary(), this.loadCopies()]);
  }

  async loadCopies(): Promise<void> {
    try {
      const copies = await this.loanService.getCopiesForBook(this.bookId);
      this.copies = copies
        .sort((a, b) => a.id - b.id)
        .map((copy) => ({
          ...copy,
          editStatus: this.normalizeEditableStatus(copy.status),
          editCondition: copy.condition,
          isUpdating: false,
        }));
    } catch {
      this.copies = [];
    }
  }

  async addCopy(): Promise<void> {
    this.isAddingCopy = true;
    this.copyMessage = "";
    this.copyError = "";
    try {
      await this.loanService.addCopy(this.bookId);
      await this.loadCopyData();
      this.copyMessage = "Exemplaar toegevoegd.";
    } catch {
      this.copyError = "Toevoegen mislukt.";
    } finally {
      this.isAddingCopy = false;
    }
  }

  async removeCopy(copyId: number): Promise<void> {
    this.copyMessage = "";
    this.copyError = "";
    try {
      await this.loanService.deleteCopy(copyId);
      await this.loadCopyData();
      this.copyMessage = "Exemplaar verwijderd.";
    } catch (err: any) {
      this.copyError =
        err?.response?.status === 409
          ? "Dit exemplaar is nog uitgeleend en kan niet verwijderd worden."
          : err?.message || "Verwijderen mislukt.";
    }
  }

  async updateCopyState(copy: CopyView): Promise<void> {
    this.copyMessage = "";
    this.copyError = "";
    copy.isUpdating = true;
    try {
      await this.loanService.updateCopyState(copy.id, {
        status: copy.editStatus,
        condition: copy.editCondition,
      });
      await this.loadCopyData();
      this.copyMessage = "Exemplaar staat bijgewerkt.";
    } catch (err: any) {
      this.copyError =
        err?.response?.status === 409
          ? "Uitgeleende exemplaren kunnen niet aangepast worden."
          : "Bijwerken van exemplaarstaat mislukt.";
    } finally {
      copy.isUpdating = false;
    }
  }

  private normalizeEditableStatus(
    status: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST",
  ): "AVAILABLE" | "DAMAGED" | "LOST" {
    if (status === "DAMAGED") return "DAMAGED";
    if (status === "LOST") return "LOST";
    return "AVAILABLE";
  }

  getCopyConditionLabel(condition: CopyView["condition"]): string {
    if (condition === "MODERATE") return "Matig";
    if (condition === "BAD") return "Slecht";
    return "Goed";
  }

  get hasKnownLanguage(): boolean {
    const l = String(this.book.taal || "")
      .trim()
      .toLowerCase();
    if (!l) return true;
    return this.languages.some((lang) => lang.toLowerCase() === l);
  }

  get hasKnownLeesniveau(): boolean {
    const l = String(this.book.leesniveau || "")
      .trim()
      .toLowerCase();
    if (!l) return true;
    return this.leesniveaus.some((n) => n.toLowerCase() === l);
  }

  async onSubmit(): Promise<void> {
    try {
      this.book.genres = this.buildGenreArray();
      await this.bookService.updateBook(this.bookId, this.book);
      this.router.navigate(["/detail", this.bookId]);
    } catch {
      this.errorMessage = "Fout bij het opslaan van wijzigingen.";
    }
  }

  cancel() {
    this.router.navigate(["/detail", this.bookId]);
  }
}
