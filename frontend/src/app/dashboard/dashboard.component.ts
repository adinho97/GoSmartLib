import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";

@Component({
    selector: "app-dashboard",
    templateUrl: "./dashboard.component.html",
    styleUrls: ["./dashboard.component.css"],
    standalone: false
})
export class DashboardComponent implements OnInit {
  featuredBooks: any[] = [];
  didacticBooks: any[] = [];
  loading = true;

  get canSeeDidactic(): boolean {
    const role = localStorage.getItem("role");
    return role === "leerkracht" || role === "bibbeheerder";
  }

  constructor(
    private router: Router,
    private bookService: BookService,
  ) {}

  async ngOnInit() {
    await this.fetchBooks();
  }

  async fetchBooks() {
    this.loading = true;
    try {
      const data = await this.bookService.getBoeken();

      this.featuredBooks = data
        .filter((b: any) => (b.genre || "").toLowerCase() !== "didactiek")
        .map((book: any) => this.mapBook(book));

      this.didacticBooks = data
        .filter((b: any) => (b.genre || "").toLowerCase() === "didactiek")
        .map((book: any) => this.mapBook(book));
    } catch (error) {
      console.error("Fout bij ophalen boeken:", error);
    } finally {
      this.loading = false;
    }
  }

  private mapBook(book: any) {
    return {
      id: book.id,
      titel: book.titel,
      auteur: book.auteur,
      genre: book.genre || "Algemeen",
      taal: book.taal ? book.taal.substring(0, 2).toUpperCase() : "??",
      paginas: book.paginas || "?",
      coverUrl: book.cover || null,
      desc: book.beschrijving || "Geen beschrijving beschikbaar.",
    };
  }

  seeDetail(book: any) {
    this.router.navigate(["/detail", book.id]);
  }
}
