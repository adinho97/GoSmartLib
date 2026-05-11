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

const DIDACTIC_SUBGENRES = [
  "Wiskunde", "Taal", "Geschiedenis", "Kleuteronderwijs",
  "Lager onderwijs", "Secundair onderwijs", "Volwasseneneducatie",
  "Geheugen", "Begrip", "Denkprocessen", "Samenwerking",
  "Interactie", "Dialoog", "Online leren", "E-learning platforms",
  "Educatieve apps", "Creativiteit", "Zelfexpressie", "Ervaringsgericht leren",
];

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
  readonly didacticSubgenres = DIDACTIC_SUBGENRES;

  // Dynamische genres van API
  genres: Genre[] = [];
  genresLoaded = false;

  // Geselecteerde waarden
  isDidactic = false;
  selectedGenreId: number | null = null;
  selectedSubgenreId: number | null = null;
  selectedDidacticSubgenre = "";

  book: Book = {
    id: 0, titel: "", auteur: "", cover: "", beschrijving: "",
    genre: "", uitgaveDatum: "", paginas: 0, taal: "", uitgeverij: "", leesniveau: "",
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
      // Laad genres én boek tegelijk, herstel selectie als beide klaar zijn
      this.genreService.getAll().subscribe({
        next: (genres) => {
          this.genres = genres;
          this.genresLoaded = true;
          if (!this.isLoading) {
            // boek was al geladen
            this.restoreGenreFromString(this.book.genre || "");
          }
        },
        error: () => { this.genresLoaded = true; },
      });
      this.loadBook();
    }
  }

  // ── Genre helpers ──────────────────────────────────────────────────────────

  get selectedGenre(): Genre | null {
    return this.genres.find(g => g.id === this.selectedGenreId) ?? null;
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
      const sub = genre.subgenres.find(s => s.id === this.selectedSubgenreId);
      return sub ? `${genre.naam} - ${sub.naam}` : genre.naam;
    }
    return genre.naam;
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
      if (parts.length > 1) this.selectedDidacticSubgenre = parts.slice(1).join(" - ").trim();
      return;
    }

    for (const genre of this.genres) {
      if (lower.startsWith(genre.naam.toLowerCase())) {
        this.selectedGenreId = genre.id;
        const remainder = genreStr.substring(genre.naam.length).replace(/^\s*-\s*/, "").trim();
        if (remainder) {
          const sub = genre.subgenres.find(s => s.naam.toLowerCase() === remainder.toLowerCase());
          if (sub) this.selectedSubgenreId = sub.id;
        }
        return;
      }
    }
  }

  // ── Boek laden ─────────────────────────────────────────────────────────────

  loadBook() {
    this.bookService.getBookById(this.bookId).subscribe({
      next: (data) => {
        this.book = data;
        this.isLoading = false;
        if (this.genresLoaded) {
          this.restoreGenreFromString(data.genre || "");
        }
      },
      error: () => {
        this.errorMessage = "Kon het boek niet laden.";
        this.isLoading = false;
      },
    });
  }

  // ── Exemplaren ─────────────────────────────────────────────────────────────

  async loadCopySummary() {
    try { this.copySummary = await this.loanService.getCopySummary(this.bookId); }
    catch { this.copySummary = { total: 0, available: 0 }; }
  }

  async loadCopyData() {
    await Promise.all([this.loadCopySummary(), this.loadCopies()]);
  }

  async loadCopies() {
    try {
      const copies = await this.loanService.getCopiesForBook(this.bookId);
      this.copies = copies.sort((a, b) => a.id - b.id).map((copy) => ({
        ...copy,
        editStatus: this.normalizeEditableStatus(copy.status),
        editCondition: copy.condition,
        isUpdating: false,
      }));
    } catch { this.copies = []; }
  }

  async addCopy() {
    this.isAddingCopy = true; this.copyMessage = ""; this.copyError = "";
    try {
      await this.loanService.addCopy(this.bookId);
      await this.loadCopyData();
      this.copyMessage = "Exemplaar toegevoegd.";
    } catch { this.copyError = "Toevoegen mislukt."; }
    finally { this.isAddingCopy = false; }
  }

  async removeCopy(copyId: number) {
    this.copyMessage = ""; this.copyError = "";
    try {
      await this.loanService.deleteCopy(copyId);
      await this.loadCopyData();
      this.copyMessage = "Exemplaar verwijderd.";
    } catch (err: any) {
      this.copyError = err?.response?.status === 409
        ? "Dit exemplaar is nog uitgeleend en kan niet verwijderd worden."
        : err?.message || "Verwijderen mislukt.";
    }
  }

  async updateCopyState(copy: CopyView) {
    this.copyMessage = ""; this.copyError = ""; copy.isUpdating = true;
    try {
      await this.loanService.updateCopyState(copy.id, { status: copy.editStatus, condition: copy.editCondition });
      await this.loadCopyData();
      this.copyMessage = "Exemplaar staat bijgewerkt.";
    } catch (err: any) {
      this.copyError = err?.response?.status === 409
        ? "Uitgeleende exemplaren kunnen niet aangepast worden."
        : "Bijwerken van exemplaarstaat mislukt.";
    } finally { copy.isUpdating = false; }
  }

  private normalizeEditableStatus(status: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST"): "AVAILABLE" | "DAMAGED" | "LOST" {
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
    const l = String(this.book.taal || "").trim().toLowerCase();
    if (!l) return true;
    return this.languages.some(lang => lang.toLowerCase() === l);
  }

  get hasKnownLeesniveau(): boolean {
    const l = String(this.book.leesniveau || "").trim().toLowerCase();
    if (!l) return true;
    return this.leesniveaus.some(n => n.toLowerCase() === l);
  }

  async onSubmit() {
    try {
      await this.bookService.updateBook(this.bookId, {
        ...this.book,
        genre: this.buildGenreString(),
      });
      this.router.navigate(["/detail", this.bookId]);
    } catch {
      this.errorMessage = "Fout bij het opslaan van wijzigingen.";
    }
  }

  cancel() {
    this.router.navigate(["/detail", this.bookId]);
  }
}