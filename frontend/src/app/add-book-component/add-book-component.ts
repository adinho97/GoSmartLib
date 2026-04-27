import { Component, OnInit } from "@angular/core";
import { NgForm } from "@angular/forms";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

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
  readonly genres = [
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
  selectedSubgenres: Set<string> = new Set();
  selectedDidacticSubgenre = "";
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  selectedCoverFile: File | null = null;
  coverPreviewUrl: string | null = null;
  goNumberLookup = "";
  isLookupLoading = false;
  isSaving = false;
  isSubmitted = false;
  submitMessage = "";
  submitState: "success" | "error" | "" = "";
  isDidactic = false;
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
  ) {}

  async ngOnInit() {
    await this.loadSchools();
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

  toggleDidactic(state: boolean) {
    this.isDidactic = state;
    this.book.genre = state ? "Didactiek" : "";
    this.selectedSubgenres.clear();
    this.selectedDidacticSubgenre = "";
  }

  onGenreChange() {
    if (this.book.genre !== "Non-fictie algemeen") {
      this.selectedSubgenres.clear();
    }
  }

  toggleSubgenre(subgenre: string) {
    if (this.selectedSubgenres.has(subgenre)) {
      this.selectedSubgenres.delete(subgenre);
    } else {
      this.selectedSubgenres.add(subgenre);
    }
  }

  isSubgenreSelected(subgenre: string): boolean {
    return this.selectedSubgenres.has(subgenre);
  }

  onCoverSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    if (this.coverPreviewUrl) {
      URL.revokeObjectURL(this.coverPreviewUrl);
    }
    this.selectedCoverFile = file;
    if (file) {
      this.coverPreviewUrl = URL.createObjectURL(file);
    }
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

      // Format genre with subgenres if non-fiction or didactic
      let genreToSave = this.book.genre;
      if (
        this.book.genre === "Non-fictie algemeen" &&
        this.selectedSubgenres.size > 0
      ) {
        const subgenresArray = Array.from(this.selectedSubgenres).sort();
        genreToSave = `Non-fictie algemeen - ${subgenresArray.join(", ")}`;
      }
      if (this.book.genre === "Didactiek" && this.selectedDidacticSubgenre) {
        genreToSave = `Didactiek - ${this.selectedDidacticSubgenre}`;
      }

      await this.bookService.addBook(
        {
          ...this.book,
          cover: coverData,
          genre: genreToSave,
        },
        this.selectedSchoolId,
      );

      this.resetForm(bookForm);
      this.submitState = "success";
      this.submitMessage = `Boek succesvol toegevoegd aan de bibliotheek.`;
    } catch (error) {
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

      this.isDidactic = this.book.genre === "Didactiek";
      this.selectedSubgenres.clear();
      this.selectedDidacticSubgenre = "";

      if (this.coverPreviewUrl) {
        URL.revokeObjectURL(this.coverPreviewUrl);
      }
      this.coverPreviewUrl = this.book.cover || null;
      this.selectedCoverFile = null;

      this.submitState = "success";
      this.submitMessage = `Boekgegevens geladen voor ${trimmed}.`;
    } catch (error: any) {
      if (error?.response?.status === 404) {
        this.submitState = "error";
        this.submitMessage = "Geen boek gevonden met dit GO-nummer.";
      } else {
        this.submitState = "error";
        this.submitMessage = "Fout bij het ophalen van het boek.";
      }
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
      genre: "",
      uitgaveDatum: "",
      paginas: null,
      taal: "",
      uitgeverij: "",
      leesniveau: "",
    };
    this.isDidactic = false;
    this.aantalExemplaren = 1;
    this.selectedSubgenres.clear();
    this.selectedDidacticSubgenre = "";
    this.selectedCoverFile = null;
    this.goNumberLookup = "";
    if (this.coverPreviewUrl) URL.revokeObjectURL(this.coverPreviewUrl);
    this.coverPreviewUrl = null;
    bookForm.resetForm();
    this.isSubmitted = false;
  }
}
