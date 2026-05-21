import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthContextService } from './auth-context.service';

export interface Tag {
  id: number;
  naam: string;
  usageCount?: number;
}

@Injectable({ providedIn: 'root' })
export class AdminTagService {
  private readonly base = '/api/admin/tags';

  constructor(
    private http: HttpClient,
    private authContext: AuthContextService
  ) {}

  private getHeaders() {
    return {
      headers: {
        'X-User-Role': this.authContext.getEffectiveRole(),
        'X-User-Sub': this.authContext.getEffectiveSub(),
        'Authorization': `Bearer ${this.authContext.getEffectiveBearerToken()}`
      },
    };
  }

  getAll(): Observable<Tag[]> {
    return this.http.get<Tag[]>(this.base, this.getHeaders());
  }

  create(naam: string): Observable<Tag> {
    return this.http.post<Tag>(this.base, { naam }, this.getHeaders());
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`, this.getHeaders());
  }
}