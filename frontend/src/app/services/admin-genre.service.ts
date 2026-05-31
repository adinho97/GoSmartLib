import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { AuthContextService } from "./auth-context.service";

export interface GenreSubgenre {
  id: number;
  naam: string;
}

export interface Genre {
  id: number;
  naam: string;
  subgenres: GenreSubgenre[];
}

@Injectable({ providedIn: "root" })
export class AdminGenreService {
  private readonly base = "/api/genres";

  constructor(
    private http: HttpClient,
    private authContext: AuthContextService,
  ) {}

  private getHeaders() {
    return {
      headers: {
        "X-User-Role": this.authContext.getEffectiveRole(),
        "X-User-Sub": this.authContext.getEffectiveSub(),
        Authorization: `Bearer ${this.authContext.getEffectiveBearerToken()}`,
      },
    };
  }

  // Lees (publiek voor alle ingelogde gebruikers)
  getAll(): Observable<Genre[]> {
    return this.http.get<Genre[]>(this.base, this.getHeaders());
  }

  // Top-level genres
  create(naam: string): Observable<Genre> {
    return this.http.post<Genre>(this.base, { naam }, this.getHeaders());
  }

  update(id: number, naam: string): Observable<Genre> {
    return this.http.put<Genre>(
      `${this.base}/${id}`,
      { naam },
      this.getHeaders(),
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`, this.getHeaders());
  }

  // Subgenres
  createSubgenre(parentId: number, naam: string): Observable<Genre> {
    return this.http.post<Genre>(
      `${this.base}/${parentId}/subgenres`,
      { naam },
      this.getHeaders(),
    );
  }

  updateSubgenre(
    parentId: number,
    subId: number,
    naam: string,
  ): Observable<Genre> {
    return this.http.put<Genre>(
      `${this.base}/${parentId}/subgenres/${subId}`,
      { naam },
      this.getHeaders(),
    );
  }

  deleteSubgenre(parentId: number, subId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/${parentId}/subgenres/${subId}`,
      this.getHeaders(),
    );
  }
}
