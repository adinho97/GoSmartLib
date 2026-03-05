import { Component } from "@angular/core";
import { ItemService } from "../item.service";

@Component({
  selector: "app-add-isbn",
  templateUrl: "./add-isbn.component.html",
  styleUrls: ["./add-isbn.component.css"],
})
export class AddIsbnComponent {
  isbn = "";
  isLoading = false;
  errorMessage = "";
  book: any = null;

  constructor(private itemService: ItemService) {}

  async zoekBoek() {
    const trimmed = this.isbn.trim();
    if (!trimmed) {
      this.errorMessage = "Voer een ISBN-nummer in.";
      return;
    }

    this.isLoading = true;
    this.errorMessage = "";
    this.book = null;

    try {
      this.book = await this.itemService.fetchBoekByIsbn(trimmed);
    } catch (err: any) {
      if (err?.response?.status === 404) {
        this.errorMessage = "Geen boek gevonden voor dit ISBN-nummer.";
      } else {
        this.errorMessage = "Er ging iets mis bij het ophalen van het boek.";
      }
    } finally {
      this.isLoading = false;
    }
  }
}
