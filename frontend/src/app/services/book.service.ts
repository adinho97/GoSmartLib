import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, Subject } from "rxjs";
import { Book } from "../models/book";
import { Review } from "../models/review";
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

export interface FavoriteItem {
  id: number;
  bookId: number;
  titel: string;
  auteur: string;
  cover: string | null;
  addedAt: string;
}

export interface LestipResponse {
  lestip: string;
  auteurNaam: string;
  magVerwijderen: boolean;
}

@Injectable({
  providedIn: "root",
})
export class BookService {
  private apiUrl = "/api/boeken";
  private wishlistChangedSource = new Subject<void>();
  wishlistChanged$ = this.wishlistChangedSource.asObservable();
  private favoriteChangedSource = new Subject<void>();
  favoriteChanged$ = this.favoriteChangedSource.asObservable();

  constructor(private http: HttpClient) {}

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
    const role = localStorage.getItem("role") || "";
    const userSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    const userName =
      localStorage.getItem("userName") ||
      localStorage.getItem("username") ||
      localStorage.getItem("name") ||
      "Gebruiker";
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

  getBookById(id: number): Observable<Book> {
    return this.http.get<Book>(this.withSchoolId(`${this.apiUrl}/${id}`));
  }

  async addBook(
    book: {
      titel: string;
      auteur: string;
      cover: string;
      beschrijving: string;
      genre: string;
      uitgaveDatum: string;
      paginas: number | null;
      taal: string;
      uitgeverij: string;
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
    const res = await axios.get(this.withSchoolId(this.apiUrl, schoolId));
    return res.data;
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
    const userSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    return {
      headers: {
        "X-User-Sub": userSub,
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

  async addToFavorites(bookId: number): Promise<void> {
    await axios.post("/api/favorieten", { bookId }, this.getUserSubHeaders());
    this.favoriteChangedSource.next();
  }

  async removeFromFavorites(bookId: number): Promise<void> {
    await axios.delete(`/api/favorieten/${bookId}`, this.getUserSubHeaders());
    this.favoriteChangedSource.next();
  }

  async getUserFavorites(): Promise<FavoriteItem[]> {
    const res = await axios.get<FavoriteItem[]>(
      "/api/favorieten",
      this.getUserSubHeaders(),
    );
    return res.data;
  }

  async isFavorited(bookId: number): Promise<boolean> {
    try {
      const res = await axios.get<boolean>(
        `/api/favorieten/${bookId}/check`,
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
}
