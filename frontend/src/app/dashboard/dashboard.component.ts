import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import { HttpClient } from "@angular/common/http";

type DashboardBook = {
  id: number;
  titel: string;
  auteur: string;
  genre: string;
  taal: string;
  paginas: number | string;
  coverUrl: string | null;
};

type BookResponse = {
  id?: number;
  titel?: string;
  auteur?: string;
  genre?: string;
  taal?: string;
  paginas?: number | null;
  cover?: string | null;
  beschrijving?: string;
};

@Component({
  selector: "app-dashboard",
  templateUrl: "./dashboard.component.html",
  styleUrls: ["./dashboard.component.css"],
  standalone: false,
})
export class DashboardComponent implements OnInit {
  featuredBooks: DashboardBook[] = [];
  didacticBooks: DashboardBook[] = [];
  myLoans: Loan[] = [];
  loading = true;
  loansLoading = true;
  favoritedBookIds = new Set<number>();
  error = "";

  today = new Date().toISOString().split("T")[0];

  get canSeeDidactic(): boolean {
    const role = localStorage.getItem("role");
    return role === "leerkracht" || role === "bibbeheerder";
  }

  get currentUsername(): string {
    return localStorage.getItem("username") || "";
  }

  constructor(
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
    private http: HttpClient,
  ) {}

  async ngOnInit() {
    await Promise.all([
      this.fetchBooks(),
      this.fetchMyLoans(),
      this.loadFavoriteState(),
    ]);
  }

  async fetchBooks() {
    this.loading = true;
    try {
      const data = (await this.bookService.getBooks()) as BookResponse[];
      this.featuredBooks = data
        .filter((book) => !this.isDidacticGenre(book.genre))
        .map((book) => this.mapBook(book));
      this.didacticBooks = data
        .filter((book) => this.isDidacticGenre(book.genre))
        .map((book) => this.mapBook(book));
    } catch (error) {
      console.error("Fout bij ophalen boeken:", error);
    } finally {
      this.loading = false;
    }
  }

  async fetchMyLoans() {
    this.loansLoading = true;
    const username = this.currentUsername;
    if (!username) {
      this.loansLoading = false;
      return;
    }
    try {
      this.myLoans = await this.loanService.getActiveLoans(username);
    } catch {
      this.myLoans = [];
    } finally {
      this.loansLoading = false;
    }
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  private isDidacticGenre(genre: unknown): boolean {
    return String(genre || "")
      .toLowerCase()
      .startsWith("didactiek");
  }

  private formatGenreForDisplay(genre: unknown): string {
    const genreText = String(genre || "").trim();
    if (!genreText) return "Algemeen";
    const [baseGenre, subgenrePart] = genreText.split(" - ", 2);
    if (!subgenrePart) return genreText;
    const firstSubgenre = subgenrePart
      .split(",")
      .map((value) => value.trim())
      .find((value) => value.length > 0);
    return firstSubgenre ? `${baseGenre} - ${firstSubgenre}` : baseGenre;
  }

  private formatLanguageForDisplay(language: unknown): string {
    const languageText = String(language || "");
    if (!languageText) return "??";
    return languageText.toLowerCase() === "nederlands"
      ? "NL"
      : languageText.substring(0, 2).toUpperCase();
  }

  private mapBook(book: BookResponse): DashboardBook {
    return {
      id: book.id || 0,
      titel: book.titel || "",
      auteur: book.auteur || "",
      genre: this.formatGenreForDisplay(book.genre),
      taal: this.formatLanguageForDisplay(book.taal),
      paginas: book.paginas || "?",
      coverUrl: book.cover || null,
    };
  }

  seeDetail(book: DashboardBook) {
    this.router.navigate(["/detail", book.id]);
  }

  goToDetail(bookId: number) {
    this.router.navigate(["/detail", bookId]);
  }

  sendTestReminder() {
    const sub = localStorage.getItem("userId");
    if (!sub) return;
    this.http.post(`/api/users/${sub}/test-reminder`, {}).subscribe({
      next: () => alert("Testbericht verzonden!"),
      error: (err) => console.error("Fout bij verzenden:", err),
    });
  }

  private async loadFavoriteState() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      this.favoritedBookIds = new Set(
        favorites.map((item: any) => item.bookId),
      );
    } catch (err) {
      console.error("Fout bij laden favorieten:", err);
    }
  }

  async toggleFavorite(event: MouseEvent, bookId: number) {
    event.stopPropagation();
    event.preventDefault();
    try {
      if (this.isFavorited(bookId)) {
        await this.bookService.removeFromFavorites(bookId);
        this.favoritedBookIds.delete(bookId);
      } else {
        await this.bookService.addToFavorites(bookId);
        this.favoritedBookIds.add(bookId);
      }
    } catch (err) {
      this.error = "Favorieten bijwerken mislukt.";
    }
  }

  isFavorited(bookId: number): boolean {
    return this.favoritedBookIds.has(bookId);
  }
}
