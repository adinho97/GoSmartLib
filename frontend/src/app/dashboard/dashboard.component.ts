import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import {
  RecommendationService,
  RecommendedBook,
} from "../services/recommendation.service";
import { HttpClient } from "@angular/common/http";
import { UserPreferencesService } from "../services/user-preferences.service";
import { SchoolService } from "../services/school.service";
import { inferNameParts, composeFullName } from "../utils/name-utils";

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
  classReadingListBooks: RecommendedBook[] = [];
  highlightedBooks: RecommendedBook[] = [];
  myLoans: Loan[] = [];

  loansLoading = true;
  recommendationsLoading = true;
  highlightedLoading = false;
  wishlistCount = 0;

  private readonly RECOMMENDATION_LIMIT = 25;
  today = new Date().toISOString().split("T")[0];

  get firstLoan(): Loan | null {
    return this.myLoans[0] ?? null;
  }

  get currentFirstName(): string {
    const firstName = (localStorage.getItem("firstName") || "").trim();
    if (firstName) return firstName;

    const lastName = (localStorage.getItem("lastName") || "").trim();
    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const { firstName: inferredFirst } = inferNameParts(
      firstName || null,
      lastName || null,
      nameCandidates,
    );
    return inferredFirst || lastName || localStorage.getItem("userName") || "Leerling";
  }

  get currentUsername(): string {
    const firstName = (localStorage.getItem("firstName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();
    const composed = composeFullName(firstName, lastName);
    if (composed) return composed;

    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const { firstName: inferredFirst, lastName: inferredLast } = inferNameParts(
      firstName || null,
      lastName || null,
      nameCandidates,
    );
    return composeFullName(inferredFirst, inferredLast)
      || firstName || lastName
      || localStorage.getItem("userName")
      || localStorage.getItem("fullname")
      || "";
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
    private userPreferencesService: UserPreferencesService,
    private schoolService: SchoolService,
  ) {}

  ngOnInit(): void {
    this.userPreferencesService.preferences$.subscribe((prefs) => {
      this.fetchAllRecommendations({
        trending: prefs["recommendationExcludeRead_trending"] ?? true,
        genre: prefs["recommendationExcludeRead_genre"] ?? true,
        author: prefs["recommendationExcludeRead_author"] ?? true,
        newArrivals: prefs["recommendationExcludeRead_newArrivals"] ?? true,
      });
    });

    this.fetchMyLoans();
    this.fetchHighlightedBooks();
    this.fetchWishlistCount();
  }

  private async fetchHighlightedBooks() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    this.highlightedLoading = true;
    try {
      const ids = await this.bookService.getHighlightedBookIds(schoolId);
      const bookIds = ids || [];
      if (bookIds.length > 0) {
        const enriched = await this.bookService.enrichBooksWithDetails(
          bookIds.map((id: number) => ({ bookId: id })),
        );
        this.highlightedBooks = enriched.map((b: any) => ({
          bookId: b.bookId,
          titel: b.titel,
          auteur: b.auteur,
          cover: b.cover || "",
          genre: b.genre,
          paginas: b.paginas,
          taal: b.taal,
        }) as RecommendedBook);
      } else {
        this.highlightedBooks = [];
      }
    } catch (error) {
      console.error("Fout bij ophalen gemarkeerde boeken:", error);
    } finally {
      this.highlightedLoading = false;
    }
  }

  private async fetchWishlistCount() {
    try {
      const items = await this.bookService.getUserWishlist();
      this.wishlistCount = items?.length ?? 0;
    } catch {
      this.wishlistCount = 0;
    }
  }

  private async fetchAllRecommendations(excludeReadFlags: {
    trending: boolean;
    genre: boolean;
    author: boolean;
    newArrivals: boolean;
  }) {
    this.recommendationsLoading = true;
    try {
      const results = await Promise.all([
        this.recommendationService.getTrending(this.RECOMMENDATION_LIMIT, excludeReadFlags.trending),
        this.recommendationService.getByGenre(this.RECOMMENDATION_LIMIT, excludeReadFlags.genre),
        this.recommendationService.getByAuthor(this.RECOMMENDATION_LIMIT, excludeReadFlags.author),
        this.recommendationService.getNewArrivals(this.RECOMMENDATION_LIMIT, excludeReadFlags.newArrivals),
      ]);

      const bookSets = {
        trending: results[0],
        genre: results[1],
        author: results[2],
        newArrivals: results[3],
      };

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

  daysLeft(dueDate: string): number {
    const due = new Date(dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / 86400000);
  }

  isUrgent(dueDate: string): boolean {
    return this.daysLeft(dueDate) <= 14;
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  goToDetail(bookId: number) {
    this.router.navigate(["/detail", bookId]);
  }

  goToMijnLijsten(fragment?: string) {
    this.router.navigate(["/mijn-lijsten"], { fragment });
  }

  logout(): void {
    const accessToken = localStorage.getItem("smartschoolToken");
    if (accessToken) {
      this.http.post("/api/auth/logout", { accessToken }).subscribe({
        next: () => this.completeLogout(),
        error: () => this.completeLogout(),
      });
    } else {
      this.completeLogout();
    }
  }

  private completeLogout(): void {
    localStorage.clear();
    this.router.navigate(["/login"]);
  }

  formatDueDate(dueDate: string): string {
    return new Date(dueDate).toLocaleDateString("nl-BE", {
      day: "numeric",
      month: "long",
    });
  }
}
