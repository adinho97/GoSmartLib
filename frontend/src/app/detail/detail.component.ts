import { Component, OnInit, OnDestroy } from "@angular/core";
import { Location } from "@angular/common";
import { ActivatedRoute, Router, NavigationEnd } from "@angular/router";
import { DomSanitizer, SafeResourceUrl } from "@angular/platform-browser";
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
  isFavoritedBook = false;
  favoriteBusy = false;

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
  deleteReviewDialogOpen = false;
  pendingReviewComment = "";
  pendingReviewRating = 0;
  pendingDeleteReviewId: number | null = null;
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

  // Preview modal variabelen
  previewModalOpen = false;
  previewUrl = "";
  previewUrlSafe: SafeResourceUrl | null = null;
  previewLoading = false;
  previewAlertOpen = false;
  previewAlertTitle = "";
  previewAlertMessage = "";

  copySummary = { total: 0, available: 0 };

  private routerSub!: Subscription;

  constructor(
    private location: Location,
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get("id"));
    this.currentBookId = Number.isFinite(id) ? id : null;

    if (this.currentBookId !== null) {
      this.bookService.getBookById(this.currentBookId).subscribe((data) => {
        this.book = data;
      });

      this.loadWishlistState(this.currentBookId);
      this.loadFavoritesState(this.currentBookId);

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

  private async loadFavoritesState(bookId: number): Promise<void> {
    try {
      this.isFavoritedBook = await this.bookService.isFavorited(bookId);
    } catch {
      this.isFavoritedBook = false;
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

  async toggleFavorite(): Promise<void> {
    if (!this.currentBookId || this.favoriteBusy) return;

    this.favoriteBusy = true;
    try {
      if (this.isFavoritedBook) {
        await this.bookService.removeFromFavorites(this.currentBookId);
        this.isFavoritedBook = false;
      } else {
        await this.bookService.addToFavorites(this.currentBookId);
        this.isFavoritedBook = true;
      }
    } finally {
      this.favoriteBusy = false;
    }
  }

  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    this.router.navigate(["/dashboard"]);
  }

  async openPreview(): Promise<void> {
    if (!this.book) return;

    this.previewLoading = true;
    this.previewUrl = "";
    this.previewUrlSafe = null;

    try {
      const normalizedIsbn = this.normalizeIsbn(this.book.isbn || "");

      const resolvedPreviewUrl =
        await this.resolvePreviewUrlWithRetries(normalizedIsbn);

      if (!resolvedPreviewUrl) {
        this.showPreviewAlert(
          "Geen voorbeeld beschikbaar",
          "Voor dit boek is er momenteel geen leesbaar voorbeeld beschikbaar op Open Library.",
        );
        return;
      }

      this.previewUrl = resolvedPreviewUrl;
      this.previewUrlSafe =
        this.sanitizer.bypassSecurityTrustResourceUrl(resolvedPreviewUrl);
      this.previewModalOpen = true;
    } catch (error) {
      console.error("Error fetching preview:", error);
      this.showPreviewAlert(
        "Voorbeeld kon niet geladen worden",
        "Er ging iets mis bij het ophalen van het boekvoorbeeld. Probeer opnieuw.",
      );
    } finally {
      this.previewLoading = false;
    }
  }

  private async resolvePreviewUrlWithRetries(
    normalizedIsbn: string,
  ): Promise<string> {
    const maxAttempts = 4;

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      let resolvedPreviewUrl = "";

      // Prefer ISBN-specific lookup first.
      if (normalizedIsbn) {
        try {
          resolvedPreviewUrl = await this.getPreviewUrlFromBibKey(
            `ISBN:${normalizedIsbn}`,
          );
        } catch {
          resolvedPreviewUrl = "";
        }
      }

      // Then try ISBN-based search fallback.
      if (!resolvedPreviewUrl) {
        try {
          resolvedPreviewUrl =
            await this.getPreviewUrlFromIsbnSearch(normalizedIsbn);
        } catch {
          resolvedPreviewUrl = "";
        }
      }

      // If ISBN exists but failed, also try title/author search in same attempt.
      if (!resolvedPreviewUrl && normalizedIsbn) {
        try {
          resolvedPreviewUrl = await this.getPreviewUrlFromTitleAuthorSearch();
        } catch {
          resolvedPreviewUrl = "";
        }
      }

      // No ISBN present: still try title/author search path.
      if (!resolvedPreviewUrl && !normalizedIsbn) {
        try {
          resolvedPreviewUrl = await this.getPreviewUrlFromTitleAuthorSearch();
        } catch {
          resolvedPreviewUrl = "";
        }
      }

      if (resolvedPreviewUrl) {
        return resolvedPreviewUrl;
      }

      if (attempt < maxAttempts) {
        await this.delay(attempt * 350);
      }
    }

    return "";
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  private showPreviewAlert(title: string, message: string): void {
    this.previewAlertTitle = title;
    this.previewAlertMessage = message;
    this.previewAlertOpen = true;
  }

  closePreviewAlert(): void {
    this.previewAlertOpen = false;
    this.previewAlertTitle = "";
    this.previewAlertMessage = "";
  }

  private normalizeIsbn(isbn: string): string {
    return isbn.replace(/[^0-9Xx]/g, "").toUpperCase();
  }

  private async getPreviewUrlFromBibKey(bibKey: string): Promise<string> {
    const booksApiUrl =
      "https://openlibrary.org/api/books" +
      `?bibkeys=${encodeURIComponent(bibKey)}` +
      "&format=json&jscmd=viewapi";

    const response = await axios.get(booksApiUrl);
    const payload = response.data || {};
    const entry = payload[bibKey] as
      | { preview?: string; preview_url?: string }
      | undefined;

    if (!entry) {
      return "";
    }

    const previewState = (entry.preview || "").toLowerCase();
    const previewUrl = (entry.preview_url || "").trim();

    if (!previewUrl || previewState === "noview") {
      return "";
    }

    return this.normalizePreviewUrl(previewUrl);
  }

  private async getPreviewUrlFromIsbnSearch(
    normalizedIsbn: string,
  ): Promise<string> {
    if (!normalizedIsbn) {
      return "";
    }

    const params = new URLSearchParams();
    params.set("isbn", normalizedIsbn);
    params.set("limit", "15");

    const searchUrl = `https://openlibrary.org/search.json?${params.toString()}`;
    return this.findPreviewInSearchUrl(searchUrl);
  }

  private async getPreviewUrlFromTitleAuthorSearch(): Promise<string> {
    const title = (this.book.titel || "").trim();
    const author = (this.book.auteur || "").trim();

    if (!title && !author) {
      return "";
    }

    const queryVariants: URLSearchParams[] = [];

    if (title && author) {
      const exactParams = new URLSearchParams();
      exactParams.set("title", title);
      exactParams.set("author", author);
      exactParams.set("limit", "20");
      queryVariants.push(exactParams);

      const qParams = new URLSearchParams();
      qParams.set("q", `${title} ${author}`);
      qParams.set("limit", "20");
      queryVariants.push(qParams);
    }

    if (title) {
      const titleOnlyParams = new URLSearchParams();
      titleOnlyParams.set("title", title);
      titleOnlyParams.set("limit", "20");
      queryVariants.push(titleOnlyParams);
    }

    if (author) {
      const authorOnlyParams = new URLSearchParams();
      authorOnlyParams.set("author", author);
      authorOnlyParams.set("limit", "20");
      queryVariants.push(authorOnlyParams);
    }

    for (const params of queryVariants) {
      const searchUrl = `https://openlibrary.org/search.json?${params.toString()}`;
      const previewUrl = await this.findPreviewInSearchUrl(searchUrl);
      if (previewUrl) {
        return previewUrl;
      }
    }

    return "";
  }

  private async findPreviewInSearchUrl(searchUrl: string): Promise<string> {
    const response = await axios.get(searchUrl);
    const docs = Array.isArray(response.data?.docs) ? response.data.docs : [];

    for (const doc of docs) {
      const archiveId =
        typeof doc.ia === "string"
          ? doc.ia
          : Array.isArray(doc.ia)
            ? doc.ia.find((id: unknown) => typeof id === "string" && !!id)
            : "";

      if (typeof archiveId === "string" && archiveId.trim()) {
        return `https://archive.org/embed/${encodeURIComponent(archiveId)}`;
      }

      const editionKeys = new Set<string>();
      if (typeof doc.cover_edition_key === "string" && doc.cover_edition_key) {
        editionKeys.add(doc.cover_edition_key);
      }
      if (Array.isArray(doc.edition_key)) {
        for (const key of doc.edition_key) {
          if (typeof key === "string" && key) {
            editionKeys.add(key);
          }
        }
      }

      for (const editionKey of Array.from(editionKeys).slice(0, 3)) {
        const previewUrl = await this.getPreviewUrlFromBibKey(
          `OLID:${editionKey}`,
        );
        if (previewUrl) {
          return previewUrl;
        }
      }

      // Last fallback: use search metadata to open reader directly when available.
      const hasReadablePreview =
        !!doc.has_preview ||
        !!doc.has_fulltext ||
        ["borrowable", "public"].includes(String(doc.ebook_access || ""));

      if (hasReadablePreview) {
        const fallbackEditionKey =
          (typeof doc.cover_edition_key === "string" &&
            doc.cover_edition_key) ||
          (Array.isArray(doc.edition_key)
            ? (doc.edition_key.find(
                (key: unknown) => typeof key === "string" && !!key,
              ) as string | undefined)
            : undefined);

        if (fallbackEditionKey) {
          return this.normalizePreviewUrl(
            `https://openlibrary.org/read/${encodeURIComponent(fallbackEditionKey)}`,
          );
        }
      }
    }

    return "";
  }

  private async normalizePreviewUrl(url: string): Promise<string> {
    const trimmedUrl = (url || "").trim();
    if (!trimmedUrl) {
      return "";
    }

    // Keep archive embed URLs as-is.
    const archiveEmbedMatch = trimmedUrl.match(
      /archive\.org\/embed\/([^/?#]+)/i,
    );
    if (archiveEmbedMatch?.[1]) {
      return `https://archive.org/embed/${encodeURIComponent(archiveEmbedMatch[1])}`;
    }

    // Convert archive details URLs to embed URLs.
    const archiveDetailsMatch = trimmedUrl.match(
      /archive\.org\/details\/([^/?#]+)/i,
    );
    if (archiveDetailsMatch?.[1]) {
      return `https://archive.org/embed/${encodeURIComponent(archiveDetailsMatch[1])}`;
    }

    // For Open Library edition/read URLs, resolve archive id when possible.
    const editionMatch = trimmedUrl.match(
      /\/(?:books|read)\/(OL[0-9A-Z]+M)(?:\/|$)/i,
    );
    if (editionMatch?.[1]) {
      const archiveEmbedUrl = await this.resolveArchiveEmbedFromEdition(
        editionMatch[1].toUpperCase(),
      );
      if (archiveEmbedUrl) {
        return archiveEmbedUrl;
      }

      // Fallback to reader URL when archive id is unavailable.
      return `https://openlibrary.org/read/${editionMatch[1].toUpperCase()}`;
    }

    return trimmedUrl;
  }

  private async resolveArchiveEmbedFromEdition(
    editionKey: string,
  ): Promise<string> {
    try {
      const editionUrl = `https://openlibrary.org/books/${encodeURIComponent(editionKey)}.json`;
      const response = await axios.get(editionUrl);
      const data = response.data || {};

      const archiveId =
        (typeof data.ocaid === "string" && data.ocaid) ||
        (typeof data.ia === "string" && data.ia) ||
        (Array.isArray(data.ia)
          ? (data.ia.find((id: unknown) => typeof id === "string" && !!id) as
              | string
              | undefined)
          : undefined);

      if (archiveId) {
        return `https://archive.org/embed/${encodeURIComponent(archiveId)}`;
      }
    } catch {
      // Best effort only; caller will use fallback URL.
    }

    return "";
  }

  closePreviewModal(): void {
    this.previewModalOpen = false;
    this.previewUrl = "";
    this.previewUrlSafe = null;
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

  deleteReview(reviewId: number): void {
    if (this.currentBookId === null) return;
    const review = this.reviews.find((r) => r.id === reviewId);
    if (!review || !this.canManageReview(review)) return;

    this.pendingDeleteReviewId = reviewId;
    this.deleteReviewDialogOpen = true;
  }

  async confirmDeleteReview(): Promise<void> {
    if (this.currentBookId === null || this.pendingDeleteReviewId === null) {
      return;
    }

    const reviewId = this.pendingDeleteReviewId;
    const review = this.reviews.find((r) => r.id === reviewId);
    if (!review || !this.canManageReview(review)) {
      this.cancelDeleteReviewDialog();
      return;
    }

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
    } finally {
      this.cancelDeleteReviewDialog();
    }
  }

  cancelDeleteReviewDialog(): void {
    this.deleteReviewDialogOpen = false;
    this.pendingDeleteReviewId = null;
  }

  canManageReview(review: Review): boolean {
    if (this.isLibrarian) {
      return true;
    }

    return !!review.canManage;
  }

  getReviewAuthor(review: Review): string {
    if (review.reviewerUserName && review.reviewerUserName.trim()) {
      return review.reviewerUserName;
    }
    if (review.reviewerUserId != null) {
      return String(review.reviewerUserId);
    }
    return "Anoniem";
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
