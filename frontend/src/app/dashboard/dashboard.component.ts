import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { LoanService, Loan } from "../services/loan.service";
import { RecommendationService, RecommendedBook } from "../services/recommendation.service";
import { HttpClient } from "@angular/common/http";

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
  myLoans: Loan[] = [];
  loansLoading = true;
  recommendationsLoading = true;

  today = new Date().toISOString().split("T")[0];

  get currentUsername(): string {
    return localStorage.getItem("username") || "";
  }

  get currentUserSub(): string {
    return localStorage.getItem("sub") || "";
  }

  constructor(
    private router: Router,
    private loanService: LoanService,
    private recommendationService: RecommendationService,
    private http: HttpClient
  ) {}

  async ngOnInit() {
    await Promise.all([
      this.fetchRecommendations(),
      this.fetchMyLoans(),
    ]);
  }

  private async fetchRecommendations() {
    this.recommendationsLoading = true;
    try {
      const [trending, byGenre, byAuthor] = await Promise.all([
        this.recommendationService.getTrending(5),
        this.recommendationService.getByGenre(5),
        this.recommendationService.getByAuthor(5),
      ]);
      this.trendingBooks = trending;
      this.genreBooks = byGenre;
      this.authorBooks = byAuthor;
    } catch (error) {
      console.error("Fout bij ophalen aanbevelingen:", error);
      this.trendingBooks = [];
      this.genreBooks = [];
      this.authorBooks = [];
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
