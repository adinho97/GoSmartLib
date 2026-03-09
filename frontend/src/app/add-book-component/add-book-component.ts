import { Component, OnInit } from "@angular/core";
import { NgForm } from "@angular/forms";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

export enum Taal {
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
  selector: "app-add-book",
  templateUrl: "./add-book-component.html",
  styleUrls: ["./add-book-component.css"],
})
export class AddBookComponent implements OnInit {
  readonly talen = Object.values(Taal);
  scholen: School[] = [];
  selectedSchoolId: number | null = null;
  selectedCoverFile: File | null = null;
  coverPreviewUrl: string | null = null;
  isSaving = false;
  isSubmitted = false;
  submitMessage = "";
  submitState: "success" | "error" | "" = "";

  book = {
    titel: "",
    auteur: "",
    cover: "",
    beschrijving: "",
    genre: "",
    uitgaveDatum: "",
    paginas: null as number | null,
    taal: "" as Taal | "",
    uitgeverij: "",
  };

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit() {
    await this.loadScholen();
  }

  async loadScholen() {
    try {
      this.scholen = await this.schoolService.getScholen();
      const storedSchoolId = this.schoolService.getSelectedSchoolId();
      const hasStoredSchool =
        storedSchoolId !== null &&
        this.scholen.some((school) => school.id === storedSchoolId);

      const fallbackSchoolId =
        this.scholen.length > 0 ? this.scholen[0].id : null;
      this.selectedSchoolId = hasStoredSchool
        ? storedSchoolId
        : fallbackSchoolId;

      if (this.selectedSchoolId !== null) {
        this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
      }
    } catch {
      this.scholen = [];
      this.selectedSchoolId = null;
    }
  }

  onCoverSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;

    if (this.coverPreviewUrl) {
      URL.revokeObjectURL(this.coverPreviewUrl);
      this.coverPreviewUrl = null;
    }

    this.selectedCoverFile = file;
    this.book.cover = file ? file.name : "";

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
    this.isSubmitted = true;

    if (bookForm.invalid) {
      bookForm.control.markAllAsTouched();
      this.submitState = "error";
      this.submitMessage =
        "Controleer het formulier: sommige velden zijn ongeldig.";
      return;
    }

    if (this.selectedSchoolId === null) {
      this.submitState = "error";
      this.submitMessage = "Kies een school.";
      return;
    }

    this.isSaving = true;
    this.submitMessage = "";
    this.submitState = "";

    try {
      const coverData = this.selectedCoverFile
        ? await this.toBase64(this.selectedCoverFile)
        : "";

      await this.bookService.addBoek(
        {
          ...this.book,
          cover: coverData,
        },
        this.selectedSchoolId,
      );

      this.book = {
        titel: "",
        auteur: "",
        cover: "",
        beschrijving: "",
        genre: "",
        uitgaveDatum: "",
        paginas: null,
        taal: "",
        uitgeverij: "",
      };
      this.selectedCoverFile = null;

      if (this.coverPreviewUrl) {
        URL.revokeObjectURL(this.coverPreviewUrl);
      }
      this.coverPreviewUrl = null;
      bookForm.resetForm();
      this.isSubmitted = false;
      this.submitState = "success";
      this.submitMessage = "Boek succesvol toegevoegd.";
    } catch {
      this.submitState = "error";
      this.submitMessage = "Opslaan mislukt. Probeer opnieuw.";
    } finally {
      this.isSaving = false;
    }
  }
}
