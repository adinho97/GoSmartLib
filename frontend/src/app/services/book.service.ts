import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { Book } from "../models/book";
import { Review } from "../models/review";
import axios from "axios";

export type BulkImportStatus =
  | "ADDED"
  | "NOT_FOUND"
  | "INVALID_ISBN"
  | "ERROR";

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

@Injectable({
  providedIn: "root",
})
export class BookService {
  private apiUrl = "/api/boeken";

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
    return {
      headers: {
        "X-User-Role": role,
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
    const res = await axios.get<Review[]>(`${this.apiUrl}/${bookId}/reviews`);
    return res.data;
  }

  async addBookReview(
    bookId: number,
    payload: {
      rating: number;
      comment: string;
      reviewerName: string;
      anonymous: boolean;
    },
  ): Promise<Review> {
    const res = await axios.post<Review>(
      `${this.apiUrl}/${bookId}/reviews`,
      payload,
    );
    return res.data;
  }

  async deleteBookReview(bookId: number, reviewId: number): Promise<void> {
    await axios.delete(
      `${this.apiUrl}/${bookId}/reviews/${reviewId}`,
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
  
  async getBookByIsbnFromLibrary(isbn: string, schoolId?: number): Promise<Book | null> {
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
}
