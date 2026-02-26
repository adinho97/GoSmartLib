import { Component } from "@angular/core";
import { NgForm } from "@angular/forms";
import { ItemService } from "../item.service";

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

  constructor(private itemService: ItemService) {}

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

    this.isSaving = true;
    this.submitMessage = "";
    this.submitState = "";

    try {
      const coverData = this.selectedCoverFile
        ? await this.toBase64(this.selectedCoverFile)
        : "";

      await this.itemService.addBoek({
        ...this.book,
        cover: coverData,
      });

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
