import { Component, OnInit, OnDestroy } from "@angular/core";
import { Location } from "@angular/common";
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
  isWishlistedBook = false;
  wishlistBusy = false;

  // Rol-gebaseerde logica
  readonly userRole = localStorage.getItem("role");
  readonly isLibrarian = this.userRole === "bibbeheerder";
  readonly isTeacher = this.userRole === "leerkracht";

  // Review-gerelateerde variabelen
  currentBookId: number | null = null;
  reviewRatings = [1, 2, 3, 4, 5];
  reviews: Review[] = [];
  newReviewRating = 0;
  newReviewComment = "";
  newReviewAnonymous = false;
  reviewError = "";
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
    private location: Location,
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

      this.loadWishlistState(this.currentBookId);

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

  private async loadWishlistState(bookId: number): Promise<void> {
    try {
      this.isWishlistedBook = await this.bookService.isWishlisted(bookId);
    } catch {
      this.isWishlistedBook = false;
    }
  }

  async toggleWishlist(): Promise<void> {
    if (!this.currentBookId || this.wishlistBusy) return;

    this.wishlistBusy = true;
    try {
      if (this.isWishlistedBook) {
        await this.bookService.removeFromWishlist(this.currentBookId);
        this.isWishlistedBook = false;
      } else {
        await this.bookService.addToWishlist(this.currentBookId);
        this.isWishlistedBook = true;
      }
    } finally {
      this.wishlistBusy = false;
    }
  }

  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    this.router.navigate(["/dashboard"]);
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
  }

  async submitReview(): Promise<void> {
    if (this.currentBookId === null) {
      this.reviewError = "Boek kon niet worden gevonden.";
      return;
    }
    const comment = this.newReviewComment.trim();
    if (this.newReviewRating < 1 || this.newReviewRating > 5) {
      this.reviewError = "Kies een score van 1 tot 5 sterren.";
      return;
    }
    if (!comment) {
      this.reviewError = "Voeg een korte comment toe.";
      return;
    }
    try {
      const review = await this.bookService.addBookReview(this.currentBookId, {
        rating: this.newReviewRating,
        comment,
        reviewerName: this.smartschoolUserName,
        anonymous: this.newReviewAnonymous,
      });
      this.reviews = [review, ...this.reviews];
      this.newReviewRating = 0;
      this.newReviewComment = "";
      this.newReviewAnonymous = false;
      this.reviewError = "";
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const apiMessage = error.response?.data?.message;
        if (typeof apiMessage === "string" && apiMessage.trim()) {
          this.reviewError = apiMessage;
          return;
        }
      }
      this.reviewError = "Review opslaan mislukt. Probeer opnieuw.";
    }
  }

  async deleteReview(reviewId: number): Promise<void> {
    if (!this.isLibrarian) return;
    if (this.currentBookId === null) return;
    if (!confirm("Review verwijderen?")) return;
    try {
      await this.bookService.deleteBookReview(this.currentBookId, reviewId);
      this.reviews = this.reviews.filter((r) => r.id !== reviewId);
    } catch {
      this.reviewError = "Review verwijderen mislukt. Probeer opnieuw.";
    }
  }

  get averageRating(): number {
    if (this.reviews.length === 0) return 0;
    return this.reviews.reduce((s, r) => s + r.rating, 0) / this.reviews.length;
  }

  private async loadReviews(bookId: number): Promise<void> {
    try {
      this.reviews = await this.bookService.getBookReviews(bookId);
    } catch {
      this.reviews = [];
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
