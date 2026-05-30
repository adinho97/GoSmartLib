import { Component, OnInit } from "@angular/core";
import {
  BookService,
  ReportedReview,
  ReportedReviewStatus,
} from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";
import { Router } from "@angular/router";
import { normalizeReviewAuthorName } from "../utils/name-utils";
import axios from "axios";

export interface GroupedReport {
  reviewId: number;
  book: { id: number; titel: string };
  review: {
    id: number;
    rating: number;
    comment: string;
    reviewerUserName: string;
    anonymous: boolean;
  };
  status: ReportedReviewStatus;
  reports: {
    id: number;
    reporterUserName: string;
    reason: string;
    reportedAt: string;
  }[];
  latestReportedAt: string;
}

@Component({
  selector: "app-reported-reviews",
  templateUrl: "./reported-reviews.component.html",
  styleUrls: ["./reported-reviews.component.css"],
  standalone: false,
})
export class ReportedReviewsComponent implements OnInit {
  reportedReviews: GroupedReport[] = [];
  loading = true;
  error = "";

  // Dialog control
  deleteReviewDialogOpen = false;
  resolveReportDialogOpen = false;

  // Pending actions data
  pendingReportId: number | null = null;
  pendingBookId: number | null = null;
  pendingReviewId: number | null = null;

  constructor(
    private bookService: BookService,
    private uiToastService: UiToastService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadReportedReviews();
  }

  async loadReportedReviews(): Promise<void> {
    this.loading = true;
    this.error = "";
    try {
      const reports = await this.bookService.getReportedReviews();
      const groups = new Map<number, GroupedReport>();

      for (const r of reports) {
        const reviewId = r.review.id;
        if (!groups.has(reviewId)) {
          groups.set(reviewId, {
            reviewId: reviewId,
            book: r.book,
            review: r.review,
            status: r.status,
            reports: [],
            latestReportedAt: r.reportedAt,
          });
        }
        const group = groups.get(reviewId)!;
        group.reports.push({
          id: r.id,
          reporterUserName: r.reporterUserName,
          reason: r.reason,
          reportedAt: r.reportedAt,
        });
        if (new Date(r.reportedAt) > new Date(group.latestReportedAt)) {
          group.latestReportedAt = r.reportedAt;
        }
      }

      this.reportedReviews = Array.from(groups.values()).sort(
        (a, b) =>
          new Date(b.latestReportedAt).getTime() -
          new Date(a.latestReportedAt).getTime(),
      );
    } catch (err: unknown) {
      console.error("Failed to load reported reviews:", err);
      this.error = "Fout bij het laden van de meldingen.";
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        this.error = err.response.data.message;
      }
    } finally {
      this.loading = false;
    }
  }

  getFormattedDate(dateString: string): string {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat("nl-NL", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  }

  getAuthorDisplayName(review: ReportedReview["review"]): string {
    if (review.anonymous) return "Anoniem";
    return normalizeReviewAuthorName(review.reviewerUserName || "Onbekend");
  }

  getStatusLabel(status: ReportedReviewStatus): string {
    switch (status) {
      case ReportedReviewStatus.PENDING:
        return "Nieuw";
      case ReportedReviewStatus.RESOLVED_KEPT:
        return "Behouden";
      case ReportedReviewStatus.RESOLVED_DELETED:
        return "Verwijderd";
      default:
        return status;
    }
  }

  goToBookDetail(bookId: number): void {
    this.router.navigate(["/detail", bookId]);
  }

  // --- Verwijder Logica ---

  openDeleteDialog(group: GroupedReport): void {
    this.pendingReportId = group.reports[0]?.id || null;
    this.pendingBookId = group.book.id;
    this.pendingReviewId = group.reviewId;
    this.deleteReviewDialogOpen = true;
  }

  closeDeleteDialog(): void {
    this.deleteReviewDialogOpen = false;
    this.pendingReportId = null;
    this.pendingBookId = null;
    this.pendingReviewId = null;
  }

  async confirmDeleteReview(): Promise<void> {
    if (
      this.pendingReportId === null ||
      this.pendingBookId === null ||
      this.pendingReviewId === null
    )
      return;

    try {
      await this.bookService.resolveReportedReview(
        this.pendingReportId,
        "inappropriate",
      );
      this.uiToastService.success("Review verwijderd en melding afgehandeld.");
      await this.loadReportedReviews();
    } catch (err) {
      this.uiToastService.error("Fout bij het verwijderen van de review.");
    } finally {
      this.closeDeleteDialog();
    }
  }

  // --- Afhandel Logica (Behouden) ---

  openResolveDialog(reportId: number): void {
    this.pendingReportId = reportId;
    this.resolveReportDialogOpen = true;
  }

  closeResolveDialog(): void {
    this.resolveReportDialogOpen = false;
    this.pendingReportId = null;
  }

  async confirmResolveReport(): Promise<void> {
    if (this.pendingReportId === null) return;

    try {
      await this.bookService.resolveReportedReview(
        this.pendingReportId,
        "appropriate",
      );
      this.uiToastService.success(
        "De melding is afgehandeld en de review is behouden.",
      );
      await this.loadReportedReviews();
    } catch (err) {
      this.uiToastService.error("Fout bij het afhandelen van de melding.");
    } finally {
      this.closeResolveDialog();
    }
  }
}
