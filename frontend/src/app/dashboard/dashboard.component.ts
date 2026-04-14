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

  ngOnInit(): void {
    const prefs = this.userPreferencesService.getSnapshotForLegacyUse();

    // Single consolidated call for all recommendations (instead of 5 separate calls)
    this.fetchAllRecommendations({
      trending: prefs["recommendationExcludeRead_trending"] ?? true,
      genre: prefs["recommendationExcludeRead_genre"] ?? true,
      author: prefs["recommendationExcludeRead_author"] ?? true,
      newArrivals: prefs["recommendationExcludeRead_newArrivals"] ?? true,
    });
    
    this.fetchMyLoans();
  }

  // Single consolidated fetch for all recommendations - uses cached API call for efficiency
  private async fetchAllRecommendations(excludeReadFlags: {
    trending: boolean;
    genre: boolean;
    author: boolean;
    newArrivals: boolean;
  }) {
    this.recommendationsLoading = true;
    try {
      // Load all strategies in parallel. The service caches these calls, so multiple calls
      // with same params hit cache. This respects per-strategy exclude-read preferences.
      const results = await Promise.all([
        this.recommendationService.getTrending(
          this.RECOMMENDATION_LIMIT,
          excludeReadFlags.trending
        ),
        this.recommendationService.getByGenre(
          this.RECOMMENDATION_LIMIT,
          excludeReadFlags.genre
        ),
        this.recommendationService.getByAuthor(
          this.RECOMMENDATION_LIMIT,
          excludeReadFlags.author
        ),
        this.recommendationService.getNewArrivals(
          this.RECOMMENDATION_LIMIT,
          excludeReadFlags.newArrivals
        ),
      ]);

      // Prepare book sets for batch enrichment
      const bookSets = {
        trending: results[0],
        genre: results[1],
        author: results[2],
        newArrivals: results[3],
      };

      // Enrich all at once with a shared book list (single getBooks call instead of 4)
      const enriched = await this.bookService.enrichMultipleBooksWithDetails(bookSets);

      this.trendingBooks = enriched["trending"];
      this.genreBooks = enriched["genre"];
      this.authorBooks = enriched["author"];
      this.newArrivalsBooks = enriched["newArrivals"];
    } catch (error) {
      console.error("Fout bij ophalen aanbevelingen:", error);
      this.trendingBooks = [];
      this.genreBooks = [];
      this.authorBooks = [];
      this.newArrivalsBooks = [];
    } finally {
      this.recommendationsLoading = false;
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

  logout(): void {
    const accessToken = localStorage.getItem("smartschoolToken");
    if (accessToken) {
      this.http.post("/api/auth/logout", { accessToken }).subscribe({
        next: () => {
          this.completeLogout();
        },
        error: (err) => {
          console.warn("Error revoking token, but proceeding with logout", err);
          this.completeLogout();
        },
      });
    } else {
      this.completeLogout();
    }
  }

  private completeLogout(): void {
    localStorage.clear();
    this.router.navigate(["/login"]);
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
