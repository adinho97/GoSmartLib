import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";

@Component({
  selector: "app-dashboard",
  templateUrl: "./dashboard.component.html",
  styleUrls: ["./dashboard.component.css"],
})
export class DashboardComponent implements OnInit {
  featuredBooks: any[] = [];
  loading = true;

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
      this.featuredBooks = data.map((book: any) => ({
        id: book.id,
        titel: book.titel,
        auteur: book.auteur,
        genre: book.genre || "Algemeen",
        taal: book.taal ? book.taal.substring(0, 2).toUpperCase() : "??",
        paginas: book.paginas || "?",
        coverUrl: book.cover || null,
        desc: book.beschrijving || "Geen beschrijving beschikbaar.",
      }));
    } catch (error) {
      console.error("Fout bij ophalen boeken:", error);
    } finally {
      this.loading = false;
    }
  }

  seeDetail(book: any) {
    console.log("Navigeren naar ID:", book.id);
    this.router.navigate(["/detail", book.id]);
  }
}
