// src/app/services/admin-genre.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Genre {
  id: number;
  naam: string;
}

@Injectable({ providedIn: 'root' })
export class AdminGenreService {
  private readonly base = '/api/admin/genres';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Genre[]> {
    return this.http.get<Genre[]>(this.base);
  }

  create(naam: string): Observable<Genre> {
    return this.http.post<Genre>(this.base, { naam });
  }

  update(id: number, naam: string): Observable<Genre> {
    return this.http.put<Genre>(`${this.base}/${id}`, { naam });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}