import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BookService } from "../services/book.service";
import { Book } from "../models/book";
import { Review } from "../models/review";
import { Location } from "@angular/common";

@Component({
  selector: "app-detail",
  templateUrl: "./detail.component.html",
  styleUrls: ["./detail.component.css"],
  standalone: false,
})
export class DetailComponent implements OnInit {
  book!: Book;
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
      this.loadReviews(this.currentBookId);
    }

    this.bookService.getBookById(id).subscribe((data) => {
      this.book = data;
    });
  }

  goBack() {
    this.location.back();
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
    } catch {
      this.reviewError = "Review opslaan mislukt. Probeer opnieuw.";
    }
  }

  get averageRating(): number {
    if (this.reviews.length === 0) {
      return 0;
    }

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
