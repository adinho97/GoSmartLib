import { Component, OnInit, OnDestroy } from "@angular/core";
import { ActivatedRoute, Router, NavigationEnd } from "@angular/router";
import { Subscription } from "rxjs";
import { filter } from "rxjs/operators";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import { Book } from "../models/book";
import { Review } from "../models/review";
import axios from "axios";

@Component({
  selector: "app-detail",
  templateUrl: "./detail.component.html",
  styleUrls: ["./detail.component.css"],
  standalone: false,
})
export class DetailComponent implements OnInit, OnDestroy {
  book!: Book;

  // Rol-gebaseerde logica
  readonly userRole = localStorage.getItem("role");
  readonly isLibrarian = this.userRole === "bibbeheerder";
  readonly isTeacher = this.userRole === "leerkracht";

  // Review-gerelateerde variabelen
  currentBookId: number | null = null;
  reviewRatings = [1, 2, 3, 4, 5];
  reviewDisplayStars = [0, 1, 2, 3, 4];
  reviews: Review[] = [];
  newReviewRating = 0;
  newReviewComment = "";
  newReviewAnonymous = false;
  publishReviewDialogOpen = false;
  pendingReviewComment = "";
  pendingReviewRating = 0;
  reviewError = "";
  reviewSuccess = "";
  editReviewId: number | null = null;
  editReviewRating = 0;
  editReviewComment = "";
  readonly maxCollapsedReviewChars = 220;
  private expandedReviewIds = new Set<number>();
  readonly smartschoolUserName =
    localStorage.getItem("userName") || "Gebruiker";

  // Lestip-gerelateerde variabelen (enkel voor leerkrachten)
  lestipText = "";
  lestipAuteurNaam = "";
  magLestipVerwijderen = false;
  newLestipText = "";
  lestipError = "";
  lestipSuccess = "";

  copySummary = { total: 0, available: 0 };

  private routerSub!: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get("id"));
    this.currentBookId = Number.isFinite(id) ? id : null;

    if (this.currentBookId !== null) {
      this.bookService.getBookById(this.currentBookId).subscribe((data) => {
        this.book = data;
      });

      // Laad de reviews
      this.loadReviews(this.currentBookId);

      if (this.isTeacher) {
        this.loadLestip(this.currentBookId);
      }

      this.loadCopySummary(this.currentBookId);
    }

    this.routerSub = this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => {
        if (this.currentBookId !== null) {
          this.loadCopySummary(this.currentBookId);
        }
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  goBack(): void {
    this.router.navigate(["/books"]);
  }

  async loadCopySummary(bookId: number) {
    try {
      this.copySummary = await this.loanService.getCopySummary(bookId);
    } catch {
      this.copySummary = { total: 0, available: 0 };
    }
  }

  getReviewDate(date: string | Date): string {
    let d: Date;
    if (typeof date === "string") {
      d = date.match(/Z|[+-]\d{2}:?\d{2}$/)
        ? new Date(date)
        : new Date(date + "Z");
    } else {
      d = date;
    }
    return new Intl.DateTimeFormat("nl-NL", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
      timeZone: "Europe/Amsterdam",
    }).format(d);
  }

  setReviewRating(rating: number): void {
    this.newReviewRating = rating;
    this.reviewError = "";
    this.reviewSuccess = "";
  }

  async submitReview(): Promise<void> {
    if (this.currentBookId === null) {
      this.reviewError = "Boek kon niet worden gevonden.";
      this.reviewSuccess = "";
      return;
    }
    const comment = this.newReviewComment.trim();
    if (this.newReviewRating < 1 || this.newReviewRating > 5) {
      this.reviewError = "Kies een score van 1 tot 5 sterren.";
      this.reviewSuccess = "";
      return;
    }
    if (!comment) {
      this.reviewError = "Voeg een korte comment toe.";
      this.reviewSuccess = "";
      return;
    }

    if (!this.newReviewAnonymous) {
      this.pendingReviewComment = comment;
      this.pendingReviewRating = this.newReviewRating;
      this.publishReviewDialogOpen = true;
      return;
    }

    await this.postReview(comment, true);
  }

  async confirmPostReviewWithName(): Promise<void> {
    this.publishReviewDialogOpen = false;
    await this.postReview(this.pendingReviewComment, false);
  }

  async confirmPostReviewAnonymously(): Promise<void> {
    this.newReviewAnonymous = true;
    this.publishReviewDialogOpen = false;
    await this.postReview(this.pendingReviewComment, true);
  }

  cancelPostReviewDialog(): void {
    this.publishReviewDialogOpen = false;
    this.pendingReviewComment = "";
    this.pendingReviewRating = 0;
  }

  private async postReview(comment: string, anonymous: boolean): Promise<void> {
    if (this.currentBookId === null) {
      return;
    }

    const ratingToSubmit = anonymous
      ? this.pendingReviewRating || this.newReviewRating
      : this.pendingReviewRating || this.newReviewRating;

    this.pendingReviewComment = "";
    this.pendingReviewRating = 0;

    try {
      const review = await this.bookService.addBookReview(this.currentBookId, {
        rating: ratingToSubmit,
        comment,
        reviewerName: this.smartschoolUserName,
        anonymous,
      });
      this.reviews = [review, ...this.reviews];
      this.newReviewRating = 0;
      this.newReviewComment = "";
      this.newReviewAnonymous = anonymous;
      this.reviewError = "";
      this.reviewSuccess = "Review opgeslagen.";
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const apiMessage = error.response?.data?.message;
        if (typeof apiMessage === "string" && apiMessage.trim()) {
          this.reviewError = apiMessage;
          this.reviewSuccess = "";
          return;
        }
      }
      this.reviewError = "Review opslaan mislukt. Probeer opnieuw.";
      this.reviewSuccess = "";
    }
  }

  async deleteReview(reviewId: number): Promise<void> {
    if (this.currentBookId === null) return;
    const review = this.reviews.find((r) => r.id === reviewId);
    if (!review || !this.canManageReview(review)) return;
    if (!confirm("Review verwijderen?")) return;
    try {
      await this.bookService.deleteBookReview(this.currentBookId, reviewId);
      this.reviews = this.reviews.filter((r) => r.id !== reviewId);
      this.reviewError = "";
      this.reviewSuccess = "Review verwijderd.";
      if (this.editReviewId === reviewId) {
        this.cancelReviewEdit();
      }
    } catch {
      this.reviewError = "Review verwijderen mislukt. Probeer opnieuw.";
      this.reviewSuccess = "";
    }
  }

  canManageReview(review: Review): boolean {
    if (this.isLibrarian) {
      return true;
    }

    return !!review.canManage;
  }

  startReviewEdit(review: Review): void {
    if (!this.canManageReview(review)) {
      return;
    }

    this.editReviewId = review.id;
    this.editReviewRating = review.rating;
    this.editReviewComment = review.comment;
    this.reviewError = "";
    this.reviewSuccess = "";
  }

  cancelReviewEdit(): void {
    this.editReviewId = null;
    this.editReviewRating = 0;
    this.editReviewComment = "";
    this.reviewError = "";
  }

  setEditReviewRating(rating: number): void {
    this.editReviewRating = rating;
    this.reviewError = "";
    this.reviewSuccess = "";
  }

  isReviewExpanded(reviewId: number): boolean {
    return this.expandedReviewIds.has(reviewId);
  }

  toggleReviewExpansion(reviewId: number): void {
    if (this.expandedReviewIds.has(reviewId)) {
      this.expandedReviewIds.delete(reviewId);
      return;
    }

    this.expandedReviewIds.add(reviewId);
  }

  isReviewTruncatable(comment: string | null | undefined): boolean {
    return (comment || "").trim().length > this.maxCollapsedReviewChars;
  }

  getReviewStarFillPercentage(review: Review, starIndex: number): number {
    const rating = Math.min(5, Math.max(0, review.rating || 0));
    const fillForStar = rating - starIndex;
    return Math.min(100, Math.max(0, fillForStar * 100));
  }

  async saveReviewEdit(reviewId: number): Promise<void> {
    if (this.currentBookId === null || this.editReviewId !== reviewId) {
      return;
    }

    const updatedComment = this.editReviewComment.trim();
    if (this.editReviewRating < 1 || this.editReviewRating > 5) {
      this.reviewError = "Kies een score van 1 tot 5 sterren.";
      this.reviewSuccess = "";
      return;
    }

    if (!updatedComment) {
      this.reviewError = "Voeg een korte comment toe.";
      this.reviewSuccess = "";
      return;
    }

    try {
      const updatedReview = await this.bookService.updateBookReview(
        this.currentBookId,
        reviewId,
        {
          rating: this.editReviewRating,
          comment: updatedComment,
        },
      );

      this.reviews = this.reviews.map((review) =>
        review.id === reviewId ? updatedReview : review,
      );
      this.cancelReviewEdit();
      this.reviewSuccess = "Review bijgewerkt.";
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const apiMessage = error.response?.data?.message;
        if (typeof apiMessage === "string" && apiMessage.trim()) {
          this.reviewError = apiMessage;
          this.reviewSuccess = "";
          return;
        }
      }

      this.reviewError = "Review bewerken mislukt. Probeer opnieuw.";
      this.reviewSuccess = "";
    }
  }

  get averageRating(): number {
    if (this.reviews.length === 0) return 0;
    return this.reviews.reduce((s, r) => s + r.rating, 0) / this.reviews.length;
  }

  private async loadReviews(bookId: number): Promise<void> {
    try {
      this.reviews = await this.bookService.getBookReviews(bookId);
      this.expandedReviewIds.clear();
      this.reviewError = "";
    } catch {
      this.reviews = [];
      this.expandedReviewIds.clear();
    }
  }

  get hasLestip(): boolean {
    return !!this.lestipText.trim();
  }

  async saveLestip(): Promise<void> {
    if (!this.isTeacher || this.currentBookId === null) {
      return;
    }

    const lestipToSave = this.newLestipText.trim();
    if (!lestipToSave) {
      this.lestipSuccess = "";
      this.lestipError = "Voeg eerst een lestip toe.";
      return;
    }

    if (this.hasLestip) {
      this.lestipSuccess = "";
      this.lestipError = "Een bestaande lestip kan niet aangepast worden.";
      return;
    }

    try {
      const savedLestipData = await this.bookService.updateBookLestip(
        this.currentBookId,
        lestipToSave,
      );
      this.lestipText = savedLestipData.lestip || "";
      this.newLestipText = "";
      this.lestipAuteurNaam = savedLestipData.auteurNaam || "";
      this.magLestipVerwijderen = !!savedLestipData.magVerwijderen;
      this.lestipError = "";
      this.lestipSuccess = "Lestip opgeslagen.";
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        if (status === 409) {
          this.lestipSuccess = "";
          this.lestipError =
            "Deze lestip bestaat al en kan niet meer aangepast worden.";
          return;
        }
      }
      this.lestipSuccess = "";
      this.lestipError = "Lestip opslaan mislukt. Probeer opnieuw.";
    }
  }

  async removeLestip(): Promise<void> {
    if (
      !this.isTeacher ||
      !this.magLestipVerwijderen ||
      this.currentBookId === null
    ) {
      return;
    }

    if (!confirm("Lestip verwijderen?")) {
      return;
    }

    try {
      await this.bookService.deleteBookLestip(this.currentBookId);
      this.lestipText = "";
      this.newLestipText = "";
      this.lestipAuteurNaam = "";
      this.magLestipVerwijderen = false;
      this.lestipError = "";
      this.lestipSuccess = "Lestip verwijderd.";
    } catch {
      this.lestipSuccess = "";
      this.lestipError = "Lestip verwijderen mislukt.";
    }
  }

  private async loadLestip(bookId: number): Promise<void> {
    try {
      const lestipData = await this.bookService.getBookLestipDetails(bookId);
      this.lestipText = lestipData.lestip || "";
      this.lestipAuteurNaam = lestipData.auteurNaam || "";
      this.magLestipVerwijderen = !!lestipData.magVerwijderen;
      this.newLestipText = "";
      this.lestipError = "";
    } catch {
      this.lestipText = "";
      this.lestipAuteurNaam = "";
      this.magLestipVerwijderen = false;
      this.lestipError = "Lestip laden mislukt.";
    }
  }
}
