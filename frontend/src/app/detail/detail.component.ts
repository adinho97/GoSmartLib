import { Component, OnInit, OnDestroy } from "@angular/core";
import { Location } from "@angular/common";
import { ActivatedRoute, Router, NavigationEnd } from "@angular/router";
import { DomSanitizer, SafeResourceUrl } from "@angular/platform-browser";
import { Subscription } from "rxjs";
import { filter } from "rxjs/operators";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import {
  BadgeNotificationService,
  BadgeUnlocked,
} from "../services/badge-notification.service";
import { ExperienceService } from "../services/experience.service";
import { Book } from "../models/book";
import { SchoolService } from "../services/school.service";
import { UiToastService } from "../services/ui-toast.service";
import { Review } from "../models/review";
import {
  inferNameParts,
  composeFullName,
  normalizeReviewAuthorName,
} from "../utils/name-utils";
import axios from "axios";

@Component({
  selector: "app-detail",
  templateUrl: "./detail.component.html",
  styleUrls: ["./detail.component.css"],
  standalone: false,
})
export class DetailComponent implements OnInit, OnDestroy {
  book!: Book;
  private previewRequestNonce = 0;
  isWishlistedBook = false;
  wishlistBusy = false;
  isHighlighted = false; // This now refers to the NEW "highlighted" feature
  isInClassReadingList = false; // New property for Klasleeslijst

  // Role-based logic
  readonly userRole = (localStorage.getItem("role") || "").toLowerCase().trim();
  readonly isLibrarian = this.userRole.includes("bibbeheerder");
  readonly isTeacher = this.userRole.includes("leerkracht");
  readonly isTeacherOrLibrarian = this.isLibrarian || this.isTeacher;
  private readonly roleLikeValues = new Set([
    "leerling",
    "leerkracht",
    "bibbeheerder",
    "gebruiker",
  ]);

  // Review-related variables
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
  get smartschoolUserName(): string {
    const firstName = (localStorage.getItem("firstName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();
    const composed = composeFullName(firstName, lastName);

    if (composed) {
      return composed;
    }

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
    const inferredComposed = composeFullName(inferredFirst, inferredLast);

    if (inferredComposed) {
      return inferredComposed;
    }

    const candidates = [
      firstName || lastName,
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("username"),
      localStorage.getItem("name"),
    ];
    for (const candidate of candidates) {
      const normalized = this.normalizeDisplayName(candidate);
      if (normalized) return normalized;
    }
    return "Gebruiker";
  }

  // Teaching tip variables (teachers only)
  lestipText = "";
  lestipAuteurNaam = "";
  magLestipVerwijderen = false;
  newLestipText = "";
  lestipError = "";
  lestipSuccess = "";

  // Preview modal variables
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
    private badgeNotificationService: BadgeNotificationService,
    private experienceService: ExperienceService,
    private schoolService: SchoolService, // Inject SchoolService
    private uiToastService: UiToastService, // Inject UiToastService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get("id"));
    this.currentBookId = Number.isFinite(id) ? id : null;

    if (this.currentBookId !== null) {
      this.bookService.getBookById(this.currentBookId).subscribe((data) => {
        this.book = data;
      });

      this.loadWishlistState(this.currentBookId);

      // Load librarian-only states
      if (this.isLibrarian) {
        this.loadHighlightState(this.currentBookId);
        this.loadClassReadingListState(this.currentBookId);
      }

      // Load reviews
      this.loadReviews(this.currentBookId);

      if (this.isTeacherOrLibrarian) {
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

  private normalizeDisplayName(raw: string | null): string {
    const value = (raw || "").trim();
    if (!value) {
      return "";
    }
    return this.roleLikeValues.has(value.toLowerCase()) ? "" : value;
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

  // New method to load the Klasleeslijst status
  private async loadClassReadingListState(bookId: number): Promise<void> {
    if (!this.isLibrarian) return;
    try {
      this.isInClassReadingList =
        await this.bookService.isClassReadingListItem(bookId);
    } catch (error) {
      console.error("Failed to load class reading list state:", error);
      this.isInClassReadingList = false;
    }
  }
  private async loadHighlightState(bookId: number): Promise<void> {
    if (!this.isLibrarian) return; // Only librarians need to see/manage this state
    try {
      this.isHighlighted = await this.bookService.isHighlighted(bookId);
    } catch (error) {
      console.error("Failed to load highlight state:", error);
      this.isHighlighted = false;
    }
  }

  async toggleHighlight(): Promise<void> {
    if (!this.currentBookId || !this.isLibrarian) return; // Only librarians can toggle highlight

    try {
      const newStatus = await this.bookService.toggleHighlight(
        this.currentBookId,
      );
      this.isHighlighted = newStatus;
      this.uiToastService.success(
        this.isHighlighted
          ? "Boek gemarkeerd."
          : "Markering van boek verwijderd.",
      );
    } catch (error) {
      console.error("Failed to toggle highlight:", error);
      this.uiToastService.error("Fout bij bijwerken markering.");
    }
  }

  // New method to toggle Klasleeslijst status
  async toggleClassReadingListItem(): Promise<void> {
    if (!this.currentBookId || !this.isLibrarian) return; // Only librarians can toggle class reading list

    try {
      const newStatus = await this.bookService.toggleClassReadingListItem(
        this.currentBookId,
      );
      this.isInClassReadingList = newStatus;
      this.uiToastService.success(
        this.isInClassReadingList
          ? "Boek toegevoegd aan Klasleeslijst."
          : "Boek verwijderd uit Klasleeslijst.",
      );
    } catch (error) {
      console.error("Failed to toggle highlight:", error);
      this.uiToastService.error("Fout bij bijwerken Klasleeslijst.");
    }
  }
  goBack(): void {
    this.router.navigate(["/books"]);
  }

  goToEditBook(): void {
    this.router.navigate(["/edit", this.book.id]);
  }

  async openPreview(): Promise<void> {
    if (!this.book) return;

    const requestNonce = ++this.previewRequestNonce;
    const requestedBookId = this.book.id;
    const requestedIsbn = this.normalizeIsbn(this.book.isbn || "");

    this.previewLoading = true;
    this.previewUrl = "";
    this.previewUrlSafe = null;

    try {
      let resolvedPreviewUrl = "";

      // First try the dedicated Books API for this ISBN.
      if (requestedIsbn) {
        resolvedPreviewUrl = await this.getPreviewUrlFromBibKey(
          `ISBN:${requestedIsbn}`,
        );
      }

      // Fallback to search and resolve best readable candidate.
      if (!resolvedPreviewUrl) {
        resolvedPreviewUrl = await this.getPreviewUrlFromSearch(requestedIsbn);
      }

      const samePreviewRequest = this.previewRequestNonce === requestNonce;
      const sameBook = this.book?.id === requestedBookId;
      if (!samePreviewRequest || !sameBook) {
        return;
      }

      if (!resolvedPreviewUrl) {
        this.showPreviewAlert(
          "Geen voorbeeld beschikbaar",
          "Er is momenteel geen leesbaar voorbeeld beschikbaar op Open Library voor dit boek.",
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
        "Er ging iets mis bij het laden van het boekvoorbeeld. Probeer het opnieuw.",
      );
    } finally {
      this.previewLoading = false;
    }
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

    // Explicitly set Authorization to undefined to prevent global interceptors from sending tokens to OpenLibrary
    const response = await axios.get(booksApiUrl, {
      headers: { Authorization: undefined },
    });
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

  private async getPreviewUrlFromSearch(
    normalizedIsbn: string,
  ): Promise<string> {
    const params = new URLSearchParams();

    if (normalizedIsbn) {
      params.set("isbn", normalizedIsbn);
    } else {
      params.set("title", this.book.titel || "");
      params.set("author", this.book.auteur || "");
    }
    params.set("limit", "5");

    const searchUrl = `https://openlibrary.org/search.json?${params.toString()}`;
    const response = await axios.get(searchUrl, {
      headers: { Authorization: undefined },
    });
    const docs = Array.isArray(response.data?.docs) ? response.data.docs : [];

    for (const doc of docs) {
      if (!this.isPreviewCandidateMatch(doc, normalizedIsbn)) {
        continue;
      }

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

  private isPreviewCandidateMatch(
    doc: Record<string, unknown>,
    normalizedIsbn: string,
  ): boolean {
    if (normalizedIsbn) {
      const docIsbnValue = doc["isbn"];
      const docIsbns = Array.isArray(docIsbnValue)
        ? docIsbnValue
            .filter((isbn): isbn is string => typeof isbn === "string")
            .map((isbn) => this.normalizeIsbn(isbn))
        : typeof docIsbnValue === "string"
          ? [this.normalizeIsbn(docIsbnValue)]
          : [];
      return docIsbns.includes(normalizedIsbn);
    }

    const requestedTitle = this.normalizePreviewSearchText(
      this.book?.titel || "",
    );
    const requestedAuthor = this.normalizePreviewSearchText(
      this.book?.auteur || "",
    );
    if (!requestedTitle || !requestedAuthor) {
      return false;
    }

    const docTitleValue = doc["title"];
    const docTitle = this.normalizePreviewSearchText(
      typeof docTitleValue === "string" ? docTitleValue : "",
    );
    const authorNamesValue = doc["author_name"];
    const authorNames = Array.isArray(authorNamesValue)
      ? authorNamesValue.filter(
          (authorName): authorName is string => typeof authorName === "string",
        )
      : typeof authorNamesValue === "string"
        ? [authorNamesValue]
        : [];
    const normalizedAuthors = authorNames.map((author) =>
      this.normalizePreviewSearchText(author),
    );

    const titleMatches = this.isPreviewMetadataMatch(requestedTitle, docTitle);
    const authorMatches = normalizedAuthors.some((author) =>
      this.isPreviewMetadataMatch(requestedAuthor, author),
    );

    return titleMatches && authorMatches;
  }

  private isPreviewMetadataMatch(
    requested: string,
    candidate: string,
  ): boolean {
    if (!requested || !candidate) {
      return false;
    }

    if (requested === candidate) {
      return true;
    }

    const requestedTokens = this.getMeaningfulPreviewTokens(requested);
    const candidateTokens = this.getMeaningfulPreviewTokens(candidate);
    if (!requestedTokens.length || !candidateTokens.length) {
      return false;
    }

    const shorterTokens =
      requestedTokens.length <= candidateTokens.length
        ? requestedTokens
        : candidateTokens;
    const longerTokenSet = new Set(
      requestedTokens.length <= candidateTokens.length
        ? candidateTokens
        : requestedTokens,
    );

    // Require every meaningful token of the shorter value to appear in the longer one.
    return shorterTokens.every((token) => longerTokenSet.has(token));
  }

  private getMeaningfulPreviewTokens(value: string): string[] {
    return value
      .split(" ")
      .map((token) => token.trim())
      .filter((token) => token.length >= 3);
  }

  private normalizePreviewSearchText(value: string): string {
    return value
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, " ")
      .replace(/\s+/g, " ")
      .trim();
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
      const response = await axios.get(editionUrl, {
        headers: { Authorization: undefined },
      });
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

  getReviewAuthor(review: Review): string {
    const rawName = review.reviewerUserName?.trim() || "Anoniem";
    if (rawName === "Anoniem") {
      return rawName;
    }
    return normalizeReviewAuthorName(rawName);
  }

  setReviewRating(rating: number): void {
    this.newReviewRating = rating;
    this.reviewError = "";
    this.reviewSuccess = "";
  }

  setEditReviewRating(rating: number): void {
    this.editReviewRating = rating;
    this.reviewError = "";
    this.reviewSuccess = "";
  }

  async submitReview(): Promise<void> {
    if (this.currentBookId === null) {
      this.reviewError = "Boek kon niet gevonden worden.";
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
      this.reviewError = "Voeg een korte opmerking toe.";
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

      // Add experience for writing a review
      this.experienceService.addExperienceForReview();

      await this.emitReviewBadgeIfUnlocked();
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        if (status === 409) {
          this.reviewError =
            "Je hebt al een review voor dit boek geplaatst. Verwijder of bewerk je bestaande review.";
          this.reviewSuccess = "";
          return;
        }

        const apiMessage = error.response?.data?.message;
        if (typeof apiMessage === "string" && apiMessage.trim()) {
          this.reviewError = apiMessage;
          this.reviewSuccess = "";
          return;
        }
      }
      this.reviewError = "Review opslaan mislukt. Probeer het opnieuw.";
      this.reviewSuccess = "";
    }
  }

  private async emitReviewBadgeIfUnlocked(): Promise<void> {
    const reviewCount = await this.bookService.getMyReviewCount();
    const badgeMilestones = [1, 5, 10, 20, 50, 100];
    if (!badgeMilestones.includes(reviewCount)) {
      return;
    }

    const badgeMap: Record<number, BadgeUnlocked> = {
      1: {
        title: "Ontgrendeld: je eerste review",
        icon: "✍️",
        category: "review",
      },
      5: { title: "Ontgrendeld: 5 reviews", icon: "✍️", category: "review" },
      10: { title: "Ontgrendeld: 10 reviews", icon: "📝", category: "review" },
      20: { title: "Ontgrendeld: 20 reviews", icon: "📝", category: "review" },
      50: { title: "Ontgrendeld: 50 reviews", icon: "🌟", category: "review" },
      100: {
        title: "Ontgrendeld: 100 reviews",
        icon: "👑",
        category: "review",
      },
    };

    const badge = badgeMap[reviewCount];
    if (!badge) {
      return;
    }

    const storageKey = "profileBadgeCounts";
    let storedLoanCount = 0;
    try {
      const rawValue = localStorage.getItem(storageKey);
      if (rawValue) {
        const parsedValue = JSON.parse(rawValue) as {
          loanCount?: number;
          reviewCount?: number;
        };
        if (typeof parsedValue.loanCount === "number") {
          storedLoanCount = parsedValue.loanCount;
        }
      }
    } catch {
      storedLoanCount = 0;
    }

    localStorage.setItem(
      storageKey,
      JSON.stringify({ loanCount: storedLoanCount, reviewCount }),
    );

    this.badgeNotificationService.showBadgeNotification(badge);
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

      // Remove the review XP again when the review is deleted.
      this.experienceService.removeExperienceForReview();

      this.reviewError = "";
      this.reviewSuccess = "Review verwijderd.";
      if (this.editReviewId === reviewId) {
        this.cancelReviewEdit();
      }
    } catch {
      this.reviewError = "Review verwijderen mislukt. Probeer het opnieuw.";
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

  canEditReview(review: Review): boolean {
    return !!review.canEdit;
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

  cancelReviewEdit(): void {
    this.editReviewId = null;
    this.editReviewRating = 0;
    this.editReviewComment = "";
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
      this.reviewError = "Voeg een korte opmerking toe.";
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

      this.reviewError = "Review bewerken mislukt. Probeer het opnieuw.";
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
    if (!this.isTeacherOrLibrarian || this.currentBookId === null) {
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
      this.lestipError = "Een bestaande lestip kan niet bewerkt worden.";
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
            "Deze lestip bestaat al en kan niet meer bewerkt worden.";
          return;
        }
      }
      this.lestipSuccess = "";
      this.lestipError = "Lestip opslaan mislukt. Probeer het opnieuw.";
    }
  }

  async removeLestip(): Promise<void> {
    if (
      !this.isTeacherOrLibrarian ||
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
