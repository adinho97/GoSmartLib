import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import { Book } from "../models/book";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";

type CopyView = {
  id: number;
  status: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST";
  condition: "GOOD" | "MODERATE" | "BAD";
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

export const LEESNIVEAUS = [
  "1ste-2de leerljaar",
  "3de-4de leerjaar",
  "5de-6de leerjaar",
  "1ste graad",
  "2de graad",
  "3de graad",
] as const;

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
  readonly genres = [
    "Didactiek",
    "Fictie algemeen",
    "Literaire roman",
    "Spanning / thriller",
    "Detective / misdaad",
    "Fantasy",
    "Sciencefiction",
    "Dystopie",
    "Historische roman",
    "Romantiek",
    "Coming-of-age",
    "Avontuur",
    "Oorlog & conflict",
    "Horror",
    "Humor",
    "Graphic novel / strip",
    "Poëzie",
    "Non-fictie algemeen",
  ];
  readonly nonFictionSubgenres = [
    "Biografie / autobiografie",
    "Wetenschap & technologie",
    "Filosofie",
    "Maatschappij & politiek",
    "Psychologie",
    "Geschiedenis",
    "Kunst & cultuur",
  ];
  readonly didacticSubgenres = [
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
  selectedGenre = "";
  selectedSubgenres: Set<string> = new Set();
  selectedDidacticSubgenre = "";

  book: Book = {
    id: 0,
    titel: "",
    auteur: "",
    cover: "",
    beschrijving: "",
    genre: "",
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
  isRemovingCopy = false;
  copyMessage = "";
  copyError = "";

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    if (idParam) {
      this.bookId = Number(idParam);
      this.loadBook();
      this.loadCopyData();
    }
  }

  loadBook() {
    this.bookService.getBookById(this.bookId).subscribe({
      next: (data) => {
        this.book = data;
        this.initializeGenreStateFromBook();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Kon het boek niet laden.";
        this.isLoading = false;
      },
    });
  }

  async loadCopySummary() {
    try {
      this.copySummary = await this.loanService.getCopySummary(this.bookId);
    } catch {
      this.copySummary = { total: 0, available: 0 };
    }
  }

  async loadCopyData() {
    await Promise.all([this.loadCopySummary(), this.loadCopies()]);
  }

  async loadCopies() {
    try {
      const copies = await this.loanService.getCopiesForBook(this.bookId);
      this.copies = copies
        .map((copy) => ({
          ...copy,
          condition: copy.condition ?? "GOOD",
        }))
        .sort((a, b) => a.id - b.id);
    } catch {
      this.copies = [];
    }
  }

  async addCopy() {
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

  async removeCopy() {
    if (this.copySummary.available === 0) {
      this.copyError = "Geen beschikbare exemplaren om te verwijderen.";
      return;
    }
    this.isRemovingCopy = true;
    this.copyMessage = "";
    this.copyError = "";
    try {
      await this.loanService.removeAvailableCopy(this.bookId);
      await this.loadCopyData();
      this.copyMessage = "Exemplaar verwijderd.";
    } catch (err: any) {
      this.copyError = err?.message || "Verwijderen mislukt.";
    } finally {
      this.isRemovingCopy = false;
    }
  }

  private initializeGenreStateFromBook() {
    const rawGenre = String(this.book.genre || "").trim();
    const lowerGenre = rawGenre.toLowerCase();
    this.selectedSubgenres.clear();
    this.selectedDidacticSubgenre = "";

    if (lowerGenre.startsWith("non-fictie algemeen")) {
      this.selectedGenre = "Non-fictie algemeen";
      if (rawGenre.length > "Non-fictie algemeen - ".length) {
        const subgenrePart = rawGenre.substring(
          "Non-fictie algemeen - ".length,
        );
        subgenrePart
          .split(",")
          .map((value) => value.trim())
          .filter((value) => value.length > 0)
          .forEach((value) => this.selectedSubgenres.add(value));
      }
      return;
    }

    if (lowerGenre.startsWith("didactiek")) {
      this.selectedGenre = "Didactiek";
      if (rawGenre.length > "Didactiek - ".length) {
        this.selectedDidacticSubgenre = rawGenre
          .substring("Didactiek - ".length)
          .trim();
      }
      return;
    }

    this.selectedGenre = this.genres.includes(rawGenre) ? rawGenre : "";
  }

  onGenreChange() {
    if (this.selectedGenre !== "Non-fictie algemeen") {
      this.selectedSubgenres.clear();
    }
    if (this.selectedGenre !== "Didactiek") {
      this.selectedDidacticSubgenre = "";
    }
  }

  toggleSubgenre(subgenre: string) {
    if (this.selectedSubgenres.has(subgenre)) {
      this.selectedSubgenres.delete(subgenre);
      return;
    }
    this.selectedSubgenres.add(subgenre);
  }

  isSubgenreSelected(subgenre: string): boolean {
    return this.selectedSubgenres.has(subgenre);
  }

  getCopyStatusLabel(status: CopyView["status"]): string {
    if (status === "AVAILABLE") return "Beschikbaar";
    if (status === "LOANED") return "Uitgeleend";
    if (status === "DAMAGED") return "Beschadigd";
    return "Verloren";
  }

  getCopyConditionLabel(condition: CopyView["condition"]): string {
    if (condition === "MODERATE") return "Matig";
    if (condition === "BAD") return "Slecht";
    return "Goed";
  }

  get hasKnownLanguage(): boolean {
    const currentLanguage = String(this.book.taal || "")
      .trim()
      .toLowerCase();
    if (!currentLanguage) return true;
    return this.languages.some(
      (language) => language.toLowerCase() === currentLanguage,
    );
  }

  get hasKnownLeesniveau(): boolean {
    const currentLeesniveau = String(this.book.leesniveau || "")
      .trim()
      .toLowerCase();
    if (!currentLeesniveau) return true;
    return this.leesniveaus.some(
      (leesniveau) => leesniveau.toLowerCase() === currentLeesniveau,
    );
  }

  async onSubmit() {
    try {
      let genreToSave = this.selectedGenre;
      if (
        this.selectedGenre === "Non-fictie algemeen" &&
        this.selectedSubgenres.size > 0
      ) {
        const subgenresArray = Array.from(this.selectedSubgenres).sort();
        genreToSave = `Non-fictie algemeen - ${subgenresArray.join(", ")}`;
      }
      if (this.selectedGenre === "Didactiek" && this.selectedDidacticSubgenre) {
        genreToSave = `Didactiek - ${this.selectedDidacticSubgenre}`;
      }

      await this.bookService.updateBook(this.bookId, {
        ...this.book,
        genre: genreToSave,
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
