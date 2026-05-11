import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GenreSubgenre {
  id: number;
  naam: string;
}

export interface Genre {
  id: number;
  naam: string;
  subgenres: GenreSubgenre[];
}

@Injectable({ providedIn: 'root' })
export class AdminGenreService {
  private readonly base = '/api/admin/genres';

  constructor(private http: HttpClient) {}

  // Lees (publiek voor alle ingelogde gebruikers)
  getAll(): Observable<Genre[]> {
    return this.http.get<Genre[]>(this.base);
  }

  // Top-level genres
  create(naam: string): Observable<Genre> {
    return this.http.post<Genre>(this.base, { naam });
  }

  update(id: number, naam: string): Observable<Genre> {
    return this.http.put<Genre>(`${this.base}/${id}`, { naam });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  // Subgenres
  createSubgenre(parentId: number, naam: string): Observable<Genre> {
    return this.http.post<Genre>(`${this.base}/${parentId}/subgenres`, { naam });
  }

  updateSubgenre(parentId: number, subId: number, naam: string): Observable<Genre> {
    return this.http.put<Genre>(`${this.base}/${parentId}/subgenres/${subId}`, { naam });
  }

  deleteSubgenre(parentId: number, subId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${parentId}/subgenres/${subId}`);
  }
}