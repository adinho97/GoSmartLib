import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, Subject } from "rxjs";
import { Book } from "../models/book";
import { Review } from "../models/review";
import { inferNameParts, composeFullName } from "../utils/name-utils";
import { SchoolService } from "./school.service";
import { AuthContextService } from "./auth-context.service";
import axios from "axios";

export type BulkImportStatus = "ADDED" | "NOT_FOUND" | "INVALID_ISBN" | "ERROR";

export interface BulkImportRowResult {
  isbn: string;
  status: BulkImportStatus;
  message: string;
  bookId: number | null;
}

export interface BulkImportResult {
  totalRows: number;
  uniqueIsbnsProcessed: number;
  duplicateRowsSkipped: number;
  totalCopiesAdded: number;
  results: BulkImportRowResult[];
}

export interface WishlistItem {
  id: number;
  bookId: number;
  titel: string;
  auteur: string;
  cover: string | null;
  addedAt: string;
  notificationEnabled?: boolean;
  lastNotifiedAt?: string | null;
  availableCopies?: number;
  totalCopies?: number;
}

export interface LestipResponse {
  lestip: string;
  auteurNaam: string;
  magVerwijderen: boolean;
}

export interface PagedBooksResponse {
  items: Book[];
  total: number;
}

export interface SchoolStatistics {
  mostReadBook: { id: number; titel: string; auteur: string; count: number } | null;
  topReader: { sub: string; displayName: string; count: number } | null;
  topClass: { name: string; count: number } | null;
}

@Injectable({
  providedIn: "root",
})
export class BookService {
  private apiUrl = "/api/boeken";
  private wishlistChangedSource = new Subject<void>();
  wishlistChanged$ = this.wishlistChangedSource.asObservable();

  private bookCache: Map<number | null, any[]> = new Map(); // TODO: Clear this cache when a book is updated/deleted

  constructor(
    private http: HttpClient,
    private authContext: AuthContextService,
  ) {}

  private resolveSchoolId(explicitSchoolId?: number): number | null {
    if (typeof explicitSchoolId === "number") {
      return explicitSchoolId;
    }

    const stored = localStorage.getItem("selectedSchoolId");
    if (!stored) {
      return null;
    }

    const parsed = Number(stored);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private withSchoolId(path: string, explicitSchoolId?: number): string {
    const schoolId = this.resolveSchoolId(explicitSchoolId);
    if (!schoolId) {
      return path;
    }

    const separator = path.includes("?") ? "&" : "?";
    return `${path}${separator}schoolId=${schoolId}`;
  }

  private getRoleHeaders() {
    const role = this.authContext.getEffectiveRole();
    const userSub = this.authContext.getEffectiveSub();

    let userName = "Gebruiker";
    if (!this.authContext.isAdminMode()) {
      const firstName = (localStorage.getItem("firstName") || "").trim();
      const lastName = (localStorage.getItem("lastName") || "").trim();
      const composedName = composeFullName(firstName, lastName);
      if (composedName) {
        userName = composedName;
      } else {
        const nameCandidates = [
          localStorage.getItem("userName"),
          localStorage.getItem("fullname"),
          localStorage.getItem("name"),
        ];
        const { firstName: inferredFirst, lastName: inferredLast } =
          inferNameParts(firstName || null, lastName || null, nameCandidates);
        userName =
          composeFullName(inferredFirst, inferredLast) ||
          firstName ||
          lastName ||
          localStorage.getItem("userName") ||
          localStorage.getItem("fullname") ||
          localStorage.getItem("username") ||
          localStorage.getItem("name") ||
          "Gebruiker";
      }
    }
    return {
      headers: {
        "X-User-Role": role,
        "X-User-Sub": userSub,
        "X-User-Name": userName,
      },
    };
  }

  getAllBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
  }

  getBookById(id: number): Observable<Book> { // TODO: This should use axios for consistency
    return this.http.get<Book>(this.withSchoolId(`${this.apiUrl}/${id}`));
  }

  async addBook(
    book: {
      titel: string;
      auteur: string;
      isbn?: string;
      goNumber?: string;
      cover: string;
      beschrijving: string;
      genre: string;
      uitgaveDatum: string;
      paginas: number | null;
      taal: string;
      uitgeverij: string;
      leesniveau?: string | null;
      schoolId?: number;
    },
    schoolId?: number,
  ) {
    const payload = {
      ...book,
      schoolId: this.resolveSchoolId(schoolId) ?? book.schoolId,
    };
    const res = await axios.post(this.apiUrl, payload);
    return res.data;
  }

  async getBooks(schoolId?: number) {
    const cacheKey = schoolId ?? null;

    if (this.bookCache.has(cacheKey)) {
      return this.bookCache.get(cacheKey)!;
    }

    const res = await axios.get(this.withSchoolId(this.apiUrl, schoolId));
    this.bookCache.set(cacheKey, res.data);
    return res.data;
  }

  async getBooksPage(
    page: number,
    size: number,
    query?: string,
    schoolId?: number,
  ): Promise<PagedBooksResponse> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });
    const resolvedSchoolId = this.resolveSchoolId(schoolId);
    if (resolvedSchoolId) {
      params.set("schoolId", String(resolvedSchoolId));
    }
    if (query && query.trim().length > 0) {
      params.set("query", query.trim());
    }

    const res = await axios.get<PagedBooksResponse>(
      `${this.apiUrl}/paged?${params.toString()}`,
    );
    return res.data;
  }

  clearCache(): void {
    this.bookCache.clear();
  }

  async enrichBooksWithDetails(books: any[]): Promise<any[]> {
    const allBooks = (await this.getBooks()) as any[];
    const bookMap = new Map(allBooks.map((b: any) => [b.id, b]));

    return books.map((book) => ({
      ...book,
      cover: bookMap.get(book.bookId)?.cover || book.cover || null,
      auteur: bookMap.get(book.bookId)?.auteur || book.auteur || null,
      taal: bookMap.get(book.bookId)?.taal || book.taal || null,
      paginas: bookMap.get(book.bookId)?.paginas || book.paginas || null,
      genre: bookMap.get(book.bookId)?.genre || book.genre || null,
    }));
  }

  // Batch enrich multiple book sets with a shared book list (for performance)
  async enrichMultipleBooksWithDetails(
    bookSets: Record<string, any[]>,
  ): Promise<Record<string, any[]>> {
    const allBooks = (await this.getBooks()) as any[];
    const bookMap = new Map(allBooks.map((b: any) => [b.id, b]));

    const enrichBook = (book: any) => ({
      ...book,
      cover: bookMap.get(book.bookId)?.cover || book.cover || null,
      auteur: bookMap.get(book.bookId)?.auteur || book.auteur || null,
      taal: bookMap.get(book.bookId)?.taal || book.taal || null,
      paginas: bookMap.get(book.bookId)?.paginas || book.paginas || null,
      genre: bookMap.get(book.bookId)?.genre || book.genre || null,
    });

    const enriched: Record<string, any[]> = {};
    for (const [key, books] of Object.entries(bookSets)) {
      enriched[key] = books.map(enrichBook);
    }
    return enriched;
  }

  async deleteBook(id: number, schoolId?: number) {
    await axios.delete(
      this.withSchoolId(`${this.apiUrl}/${id}`, schoolId),
      this.getRoleHeaders(),
    );
  }

  async getBookReviews(bookId: number): Promise<Review[]> {
    const res = await axios.get<Review[]>(
      `${this.apiUrl}/${bookId}/reviews`,
      this.getRoleHeaders(),
    );
    return res.data;
  }

  async getMyReviewCount(): Promise<number> {
    const res = await axios.get<{ count: number }>(
      `${this.apiUrl}/reviews/mijn/aantal`,
      this.getRoleHeaders(),
    );
    return res.data?.count ?? 0;
  }

  async addBookReview(
    bookId: number,
    payload: {
      rating: number;
      comment: string;
      anonymous: boolean;
    },
  ): Promise<Review> {
    const res = await axios.post<Review>(
      `${this.apiUrl}/${bookId}/reviews`,
      payload,
      this.getRoleHeaders(),
    );
    return res.data;
  }

  async deleteBookReview(bookId: number, reviewId: number): Promise<void> {
    await axios.delete(
      `${this.apiUrl}/${bookId}/reviews/${reviewId}`,
      this.getRoleHeaders(),
    );
  }

  async updateBookReview(
    bookId: number,
    reviewId: number,
    payload: { rating: number; comment: string },
  ): Promise<Review> {
    const res = await axios.put<Review>(
      `${this.apiUrl}/${bookId}/reviews/${reviewId}`,
      payload,
      this.getRoleHeaders(),
    );
    return res.data;
  }

  async getBookLestip(bookId: number): Promise<string> {
    const data = await this.getBookLestipDetails(bookId);
    return data.lestip || "";
  }

  async getBookLestipDetails(bookId: number): Promise<LestipResponse> {
    const res = await axios.get<LestipResponse>(
      `${this.apiUrl}/${bookId}/lestip`,
      this.getRoleHeaders(),
    );
    return res.data;
  }

  async updateBookLestip(
    bookId: number,
    lestip: string,
  ): Promise<LestipResponse> {
    const res = await axios.put<LestipResponse>(
      `${this.apiUrl}/${bookId}/lestip`,
      { lestip },
      this.getRoleHeaders(),
    );
    return res.data;
  }

  async deleteBookLestip(bookId: number): Promise<void> {
    await axios.delete(
      `${this.apiUrl}/${bookId}/lestip`,
      this.getRoleHeaders(),
    );
  }

  async fetchBookByIsbn(isbn: string) {
    const res = await axios.get(`${this.apiUrl}/preview/${isbn}`);
    return res.data;
  }

  async fetchBookByGoNumber(goNumber: string) {
    const res = await axios.get(`${this.apiUrl}/go/${goNumber}`);
    return res.data;
  }

  async getBookByGoNumberFromLibrary(
    goNumber: string,
    schoolId?: number,
  ): Promise<Book | null> {
    try {
      const res = await axios.get<Book>(
        this.withSchoolId(`${this.apiUrl}/go/${goNumber}`, schoolId),
      );
      return res.data;
    } catch (err: any) {
      if (err?.response?.status === 404) {
        return null;
      }
      throw err;
    }
  }

  async importBookByIsbn(isbn: string, schoolId?: number) {
    const res = await axios.post(
      this.withSchoolId(`${this.apiUrl}/isbn/${isbn}`, schoolId),
    );
    return res.data;
  }

  async importBooksByUpload(
    file: File,
    schoolId?: number,
  ): Promise<BulkImportResult> {
    const formData = new FormData();
    formData.append("file", file);

    const res = await axios.post<BulkImportResult>(
      this.withSchoolId(`${this.apiUrl}/isbn/bulk`, schoolId),
      formData,
    );
    return res.data;
  }

  async isBookInLibrary(isbn: string, schoolId?: number): Promise<boolean> {
    try {
      await axios.get(
        this.withSchoolId(`${this.apiUrl}/isbn/${isbn}`, schoolId),
      );
      return true;
    } catch (err: any) {
      if (err?.response?.status === 404) {
        return false;
      }
      throw err;
    }
  }

  async getBookByIsbnFromLibrary(
    isbn: string,
    schoolId?: number,
  ): Promise<Book | null> {
    try {
      const res = await axios.get<Book>(
        this.withSchoolId(`${this.apiUrl}/isbn/${isbn}`, schoolId),
      );
      return res.data;
    } catch (err: any) {
      if (err?.response?.status === 404) {
        return null;
      }
      throw err;
    }
  }
  async updateBook(id: number, book: Book): Promise<Book> {
    const res = await axios.put(`${this.apiUrl}/${id}`, book);
    return res.data;
  }

  private getUserSubHeaders() {
    const userSub = this.authContext.getEffectiveSub();
    const token = this.authContext.getEffectiveBearerToken();
    return {
      headers: {
        "X-User-Sub": userSub,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    };
  }

  async addToWishlist(bookId: number): Promise<void> {
    await axios.post("/api/verlanglijst", { bookId }, this.getUserSubHeaders());
    this.wishlistChangedSource.next();
  }

  async removeFromWishlist(bookId: number): Promise<void> {
    await axios.delete(`/api/verlanglijst/${bookId}`, this.getUserSubHeaders());
    this.wishlistChangedSource.next();
  }

  async getUserWishlist(): Promise<WishlistItem[]> {
    const res = await axios.get<WishlistItem[]>(
      "/api/verlanglijst",
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async updateWishlistNotification(
    wishlistId: number,
    notificationEnabled: boolean,
  ): Promise<WishlistItem> {
    const res = await axios.patch<WishlistItem>(
      `/api/verlanglijst/${wishlistId}`,
      { notificationEnabled },
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async isWishlisted(bookId: number): Promise<boolean> {
    try {
      const res = await axios.get<boolean>(
        `/api/verlanglijst/${bookId}/check`,
        this.getUserSubHeaders(),
      );
      return res.data;
    } catch (err: any) {
      if (err?.response?.status === 404) {
        return false;
      }
      throw err;
    }
  }

  // New methods for the distinct "highlighted" feature
  async toggleHighlight(bookId: number): Promise<boolean> {
    const schoolId = this.resolveSchoolId();
    if (!schoolId) {
      console.error("No school selected to toggle highlight.");
      throw new Error("No school selected.");
    }
    // This now points to the NEW /api/highlighted-books endpoint
    const res = await axios.post<boolean>(
      `/api/highlighted-books/${bookId}/toggle?schoolId=${schoolId}`,
      {}, // Empty body for POST
      this.getUserSubHeaders(),
    );
    return res.data;
  }
  async isHighlighted(bookId: number): Promise<boolean> {
    const schoolId = this.resolveSchoolId();
    if (!schoolId) {
      return false; // If no school selected, it can't be highlighted for a school
    }
    // This now points to the NEW /api/highlighted-books endpoint
    const res = await axios.get<boolean>(
      `/api/highlighted-books/${bookId}/status?schoolId=${schoolId}`,
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async getHighlightedBookIds(schoolId: number): Promise<number[]> {
    const res = await axios.get<number[]>(
      `/api/highlighted-books/school/${schoolId}`,
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async toggleClassReadingListItem(bookId: number): Promise<boolean> {
    const schoolId = this.resolveSchoolId();
    if (!schoolId) {
      console.error("No school selected to toggle class reading list item.");
      throw new Error("No school selected.");
    }
    // This now points to the /api/class-reading-list endpoint
    const res = await axios.post<boolean>(
      `/api/class-reading-list/${bookId}/toggle?schoolId=${schoolId}`,
      {}, // Empty body for POST
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async isClassReadingListItem(bookId: number): Promise<boolean> {
    const schoolId = this.resolveSchoolId();
    if (!schoolId) {
      return false; // If no school selected, it can't be in the class reading list
    }
    // This now points to the /api/class-reading-list endpoint
    const res = await axios.get<boolean>(
      `/api/class-reading-list/${bookId}/status?schoolId=${schoolId}`,
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async getClassReadingListItemIds(schoolId: number): Promise<number[]> {
    const res = await axios.get<number[]>(
      `/api/class-reading-list/school/${schoolId}`,
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  /**
   * Combines role-based headers with authentication token headers.
   * Used for management actions that require both identity and permission context.
   */
  private getFullAuthHeaders() {
    const roleHeaders = this.getRoleHeaders();
    const subHeaders = this.getUserSubHeaders();
    return {
      headers: {
        ...roleHeaders.headers,
        ...subHeaders.headers,
      },
    };
  }

  async getSchoolStatistics(schoolId: number): Promise<SchoolStatistics> {
    const res = await axios.get<SchoolStatistics>(
      `/api/scholen/${schoolId}/statistieken`,
      this.getFullAuthHeaders(),
    );
    return res.data;
  }
}
