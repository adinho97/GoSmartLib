import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import { RecommendationService, RecommendedBook } from "../services/recommendation.service";
import { HttpClient } from "@angular/common/http";
import { UserPreferencesService } from "../services/user-preferences.service";

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
  genreBooks: RecommendedBook[] = [];
  authorBooks: RecommendedBook[] = [];
  newArrivalsBooks: RecommendedBook[] = [];
  myLoans: Loan[] = [];
  loansLoading = true;
  recommendationsLoading = true;
  private readonly RECOMMENDATION_LIMIT = 25;

  today = new Date().toISOString().split("T")[0];

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
    private http: HttpClient,
    private userPreferencesService: UserPreferencesService
  ) {}

  async ngOnInit() {
    const prefs = this.userPreferencesService.getSnapshotForLegacyUse();

    await Promise.all([
      this.fetchRecommendations(prefs["recommendationExcludeRead_trending"] ?? true),
      this.fetchMyLoans(),
      this.fetchGenreRecommendations(prefs["recommendationExcludeRead_genre"] ?? true),
      this.fetchAuthorRecommendations(prefs["recommendationExcludeRead_author"] ?? true),
      this.fetchNewArrivalsRecommendations(
        prefs["recommendationExcludeRead_newArrivals"] ?? true
      ),
    ]);
  }

  private async fetchRecommendations(excludeRead: boolean = true) {
    this.recommendationsLoading = true;
    try {
      const trendingBooks = await this.recommendationService.getTrending(
        this.RECOMMENDATION_LIMIT,
        excludeRead
      );
      this.trendingBooks = await this.bookService.enrichBooksWithDetails(trendingBooks);
    } catch (error) {
      console.error("Fout bij ophalen trending aanbevelingen:", error);
      this.trendingBooks = [];
    } finally {
      this.recommendationsLoading = false;
    }
  }

  private async fetchGenreRecommendations(excludeRead: boolean = true) {
    try {
      const genreBooks = await this.recommendationService.getByGenre(
        this.RECOMMENDATION_LIMIT,
        excludeRead
      );
      this.genreBooks = await this.bookService.enrichBooksWithDetails(genreBooks);
    } catch (error) {
      console.error("Fout bij ophalen genre aanbevelingen:", error);
      this.genreBooks = [];
    }
  }

  private async fetchAuthorRecommendations(excludeRead: boolean = true) {
    try {
      const authorBooks = await this.recommendationService.getByAuthor(
        this.RECOMMENDATION_LIMIT,
        excludeRead
      );
      this.authorBooks = await this.bookService.enrichBooksWithDetails(authorBooks);
    } catch (error) {
      console.error("Fout bij ophalen auteur aanbevelingen:", error);
      this.authorBooks = [];
    }
  }

  private async fetchNewArrivalsRecommendations(excludeRead: boolean = true) {
    try {
      const newArrivalsBooks = await this.recommendationService.getNewArrivals(
        this.RECOMMENDATION_LIMIT,
        excludeRead
      );
      this.newArrivalsBooks = await this.bookService.enrichBooksWithDetails(newArrivalsBooks);
    } catch (error) {
      console.error("Fout bij ophalen nieuwe aankomsten:", error);
      this.newArrivalsBooks = [];
    }
  }

  async onRefreshRecommendations(section: 'trending' | 'genre' | 'author', excludeRead: boolean) {
    try {
      if (section === 'trending') {
        const trendingBooks = await this.recommendationService.getTrending(this.RECOMMENDATION_LIMIT, excludeRead);
        this.trendingBooks = await this.bookService.enrichBooksWithDetails(trendingBooks);
      } else if (section === 'genre') {
        const genreBooks = await this.recommendationService.getByGenre(this.RECOMMENDATION_LIMIT, excludeRead);
        this.genreBooks = await this.bookService.enrichBooksWithDetails(genreBooks);
      } else if (section === 'author') {
        const authorBooks = await this.recommendationService.getByAuthor(this.RECOMMENDATION_LIMIT, excludeRead);
        this.authorBooks = await this.bookService.enrichBooksWithDetails(authorBooks);
      }
    } catch (error) {
      console.error(`Fout bij verversen ${section} aanbevelingen:`, error);
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
