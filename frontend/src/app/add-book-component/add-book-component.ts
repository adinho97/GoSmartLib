import { Component } from "@angular/core";
import { NgForm } from "@angular/forms";
import { BookService } from "../services/book.service";

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
export class AddBookComponent {
  readonly talen = Object.values(Taal);
  selectedCoverFile: File | null = null;
  coverPreviewUrl: string | null = null;
  isSaving = false;
  isSubmitted = false;
  submitMessage = "";
  submitState: "success" | "error" | "" = "";

  isDidactic = false;

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

  constructor(private bookService: BookService) {}

  toggleDidactic(state: boolean) {
    this.isDidactic = state;
    this.book.genre = state ? 'Didactiek' : '';
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
    this.isSubmitted = true;
    if (bookForm.invalid) return;

    this.isSaving = true;
    try {
      const coverData = this.selectedCoverFile ? await this.toBase64(this.selectedCoverFile) : "";
      
      // Aanroep naar de nieuwe addBoek methode in BookService
      await this.bookService.addBoek({ 
        ...this.book, 
        cover: coverData,
        taal: this.book.taal as string // Casten naar string voor de service
      });

      this.resetForm(bookForm);
      this.submitState = "success";
      this.submitMessage = "Boek succesvol toegevoegd aan de bibliotheek.";
    } catch (error) {
      this.submitState = "error";
      this.submitMessage = "Fout bij opslaan. Controleer de verbinding.";
    } finally {
      this.isSaving = false;
    }
  }

  private resetForm(bookForm: NgForm) {
    this.book = { titel: "", auteur: "", cover: "", beschrijving: "", genre: "", uitgaveDatum: "", paginas: null, taal: "", uitgeverij: "" };
    this.isDidactic = false;
    this.selectedCoverFile = null;
    if (this.coverPreviewUrl) URL.revokeObjectURL(this.coverPreviewUrl);
    this.coverPreviewUrl = null;
    bookForm.resetForm();
    this.isSubmitted = false;
  }
}