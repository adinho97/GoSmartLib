import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BookService } from "../services/book.service";
import { Book } from "../models/book";
import { Review } from "../models/review";
import { Location } from "@angular/common";
import axios from "axios";

@Component({
  selector: "app-detail",
  templateUrl: "./detail.component.html",
  styleUrls: ["./detail.component.css"],
  standalone: false,
})
export class DetailComponent implements OnInit {
  book!: Book;
  
  // Rol-gebaseerde logica
  readonly userRole = localStorage.getItem("role");
  readonly isLibrarian = this.userRole === "bibbeheerder";
  
  // Review-gerelateerde variabelen
  currentBookId: number | null = null;
  reviewRatings = [1, 2, 3, 4, 5];
  reviews: Review[] = [];
  newReviewRating = 0;
  newReviewComment = "";
  reviewError = "";

  constructor(
    private location: Location,
    private route: ActivatedRoute,
    private bookService: BookService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get("id"));
    this.currentBookId = Number.isFinite(id) ? id : null;

    if (this.currentBookId !== null) {
      // Laad het boek
      this.bookService.getBookById(this.currentBookId).subscribe((data) => {
        this.book = data;
      });
      
      // Laad de reviews
      this.loadReviews(this.currentBookId);
    }
    
    // Debugging logs (optioneel)
    console.log("Huidige rol:", this.userRole);
    console.log("Is beheerder:", this.isLibrarian);
  }

  goBack(): void {
    this.location.back();
  }

  // --- REVIEW METHODES ---

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
      });

      this.reviews = [review, ...this.reviews];
      this.newReviewRating = 0;
      this.newReviewComment = "";
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

    if (this.currentBookId === null) {
      this.reviewError = "Boek kon niet worden gevonden.";
      return;
    }

    if (!confirm("Review verwijderen?")) return;

    try {
      await this.bookService.deleteBookReview(this.currentBookId, reviewId);
      this.reviews = this.reviews.filter((review) => review.id !== reviewId);
    } catch {
      this.reviewError = "Review verwijderen mislukt. Probeer opnieuw.";
    }
  }

  get averageRating(): number {
    if (this.reviews.length === 0) return 0;
    const total = this.reviews.reduce((sum, review) => sum + review.rating, 0);
    return total / this.reviews.length;
  }

  private async loadReviews(bookId: number): Promise<void> {
    try {
      this.reviews = await this.bookService.getBookReviews(bookId);
    } catch {
      this.reviews = [];
    }
  }
}