import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { Book } from "../models/book";
import { FormsModule } from "@angular/forms";

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

@Component({
  selector: "app-edit-book",
  standalone: true,
  imports: [FormsModule],
  templateUrl: "./edit-book.component.html",
  styleUrls: ["./edit-book.component.css"],
})
export class EditBookComponent implements OnInit {
  bookId!: number;
  readonly languages = Object.values(Language);
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

  // Geen ISBN meer hier, want het staat niet in je interface
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
  };

  isLoading = true;
  errorMessage = "";

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    if (idParam) {
      this.bookId = Number(idParam);
      this.loadBook();
    }
  }

  loadBook() {
    this.bookService.getBookById(this.bookId).subscribe({
      next: (data) => {
        this.book = data;
        this.initializeGenreStateFromBook();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = "Kon het boek niet laden.";
        this.isLoading = false;
      },
    });
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

  get hasKnownLanguage(): boolean {
    const currentLanguage = String(this.book.taal || "")
      .trim()
      .toLowerCase();
    if (!currentLanguage) return true;
    return this.languages.some(
      (language) => language.toLowerCase() === currentLanguage,
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
      this.router.navigate(["/books"]);
    } catch (err) {
      this.errorMessage = "Fout bij het opslaan van wijzigingen.";
    }
  }

  cancel() {
    this.router.navigate(["/books"]);
  }
}
