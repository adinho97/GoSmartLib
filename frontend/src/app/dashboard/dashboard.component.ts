import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import { RecommendationService, RecommendedBook } from "../services/recommendation.service";
import { HttpClient } from "@angular/common/http";

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
  trendingBooks: RecommendedBook[] = [];
  didacticBooks: RecommendedBook[] = [];
  myLoans: Loan[] = [];
  loansLoading = true;
  recommendationsLoading = true;

  today = new Date().toISOString().split("T")[0];

  get canSeeDidactic(): boolean {
    const role = localStorage.getItem("role");
    return role === "leerkracht" || role === "bibbeheerder";
  }

  get currentUsername(): string {
    return localStorage.getItem("username") || "";
  }

  get currentUserSub(): string {
    return localStorage.getItem("sub") || "";
  }

  constructor(
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
    private recommendationService: RecommendationService,
    private http: HttpClient
  ) {}

  async ngOnInit() {
    await Promise.all([
      this.fetchRecommendations(),
      this.fetchMyLoans(),
      this.fetchDidacticBooks(),
    ]);
  }

  private async fetchRecommendations() {
    this.recommendationsLoading = true;
    try {
      const trendingBooks = await this.recommendationService.getTrending(5);
      
      // Enrich trending books with cover images from book service
      const allBooks = (await this.bookService.getBooks()) as BookResponse[];
      const bookMap = new Map(allBooks.map(b => [b.id, b]));
      
      this.trendingBooks = trendingBooks.map(rec => ({
        ...rec,
        cover: bookMap.get(rec.bookId)?.cover || rec.cover || null,
        taal: bookMap.get(rec.bookId)?.taal || rec.taal || null,
        paginas: bookMap.get(rec.bookId)?.paginas || rec.paginas || null,
      }));
    } catch (error) {
      console.error("Fout bij ophalen trending aanbevelingen:", error);
      this.trendingBooks = [];
    } finally {
      this.recommendationsLoading = false;
    }
  }

  private async fetchDidacticBooks() {
    try {
      const allBooks = (await this.bookService.getBooks()) as BookResponse[];
      const didacticGenreBooks = allBooks
        .filter((book) => this.isDidacticGenre(book.genre))
        .slice(0, 5);
      
      // Convert BookResponse to RecommendedBook format
      this.didacticBooks = didacticGenreBooks.map((book) => ({
        bookId: book.id || 0,
        titel: book.titel || "",
        auteur: book.auteur || "",
        genre: book.genre || "",
        score: 0,
        reason: "Didactische collectie",
        cover: book.cover || null,
        taal: book.taal || null,
        paginas: book.paginas || null,
      }));
    } catch (error) {
      console.error("Fout bij ophalen didactische boeken:", error);
      this.didacticBooks = [];
    }
  }

  async fetchMyLoans() {
    this.loansLoading = true;
    const userSub = this.currentUserSub;
    if (!userSub) {
      this.loansLoading = false;
      return;
    }
    try {
      this.myLoans = await this.loanService.getActiveLoans(userSub);
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
}
