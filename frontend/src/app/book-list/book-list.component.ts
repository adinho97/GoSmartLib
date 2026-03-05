import { Component, OnInit } from "@angular/core";
import { BookService } from "../services/book.service";

type Boek = {
  id?: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  genre: string;
  uitgaveDatum: string;
  paginas: number | null;
  taal: string;
  uitgeverij: string;
};

@Component({
  selector: "app-book-list",
  templateUrl: "./book-list.component.html",
  styleUrls: ["./book-list.component.css"],
})
export class BookListComponent implements OnInit {
  boeken: Boek[] = [];
  isLoading = true;
  error = "";
  readonly isBibbeheerder = localStorage.getItem("role") === "bibbeheerder";
  readonly pageSize = 25;
  currentPage = 1;

  constructor(private bookService: BookService) {}

  async ngOnInit() {
    await this.loadBoeken();
  }

  async loadBoeken() {
    this.isLoading = true;
    this.error = "";

    try {
      const boeken = await this.bookService.getBoeken();
      this.boeken = boeken.sort((a: Boek, b: Boek) =>
        (a.titel || "").localeCompare(b.titel || "", "nl", {
          sensitivity: "base",
        }),
      );
      this.currentPage = 1;
    } catch {
      this.error = "Boeken laden mislukt. Probeer later opnieuw.";
    } finally {
      this.isLoading = false;
    }
  }

  get totalPages(): number {
    return Math.ceil(this.boeken.length / this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, index) => index + 1);
  }

  get pagedBoeken(): Boek[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.boeken.slice(start, start + this.pageSize);
  }

  gaNaarPagina(page: number) {
    if (page < 1 || page > this.totalPages) {
      return;
    }

    this.currentPage = page;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async verwijderBoek(event: MouseEvent, boek: Boek) {
    event.stopPropagation();
    event.preventDefault();

    if (!boek.id) {
      return;
    }

    const isConfirmed = window.confirm(
      `Weet je zeker dat je "${boek.titel}" wil verwijderen?`,
    );

    if (!isConfirmed) {
      return;
    }

    try {
      await this.bookService.deleteBoek(boek.id);
      this.boeken = this.boeken.filter((b) => b.id !== boek.id);

      if (this.currentPage > this.totalPages && this.totalPages > 0) {
        this.currentPage = this.totalPages;
      }
    } catch {
      this.error = "Verwijderen mislukt. Probeer later opnieuw.";
    }
  }
}
