import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { Book } from "../models/book";
import axios from "axios";

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

  getAllBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
  }

  getBookById(id: number): Observable<Book> {
    return this.http.get<Book>(this.withSchoolId(`${this.apiUrl}/${id}`));
  }

  async addBoek(
    boek: {
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
      ...boek,
      schoolId: this.resolveSchoolId(schoolId) ?? boek.schoolId,
    };
    const res = await axios.post(this.apiUrl, payload);
    return res.data;
  }

  async getBoeken(schoolId?: number) {
    const res = await axios.get(this.withSchoolId(this.apiUrl, schoolId));
    return res.data;
  }

  async deleteBoek(id: number, schoolId?: number) {
    await axios.delete(this.withSchoolId(`${this.apiUrl}/${id}`, schoolId));
  }

  async fetchBoekByIsbn(isbn: string) {
    const res = await axios.get(`${this.apiUrl}/preview/${isbn}`);
    return res.data;
  }

  async importBoekByIsbn(isbn: string, schoolId?: number) {
    const res = await axios.post(
      this.withSchoolId(`${this.apiUrl}/isbn/${isbn}`, schoolId),
    );
    return res.data;
  }

  async bestaatBoekInBibliotheek(
    isbn: string,
    schoolId?: number,
  ): Promise<boolean> {
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
}
