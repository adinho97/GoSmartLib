import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type Sectie = 'STAP' | 'FEATURE' | 'TIP' | 'FAQ';

export type InfoContentItem = {
  id?: number;
  sectie: Sectie;
  titel?: string;
  inhoud: string;
  sortOrder?: number;
  schoolId?: number;
};

@Injectable({ providedIn: 'root' })
export class InfoContentService {
  private readonly apiUrl = '/api/info-content';

  constructor(private http: HttpClient) {}

  getAll(sectie: Sectie, schoolId?: number): Observable<InfoContentItem[]> {
    let params = new HttpParams().set('sectie', sectie);
    if (schoolId) params = params.set('schoolId', schoolId.toString());
    return this.http.get<InfoContentItem[]>(this.apiUrl, { params });
  }

  hasContent(sectie: Sectie, schoolId?: number): Observable<boolean> {
    let params = new HttpParams().set('sectie', sectie);
    if (schoolId) params = params.set('schoolId', schoolId.toString());
    return this.http.get<boolean>(`${this.apiUrl}/has-content`, { params });
  }

  create(item: InfoContentItem): Observable<InfoContentItem> {
    return this.http.post<InfoContentItem>(this.apiUrl, item, {
      headers: this.authHeaders(),
    });
  }

  update(id: number, item: InfoContentItem): Observable<InfoContentItem> {
    return this.http.put<InfoContentItem>(`${this.apiUrl}/${id}`, item, {
      headers: this.authHeaders(),
    });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: this.authHeaders(),
    });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-User-Sub': localStorage.getItem('sub') || '',
      'X-User-Role': localStorage.getItem('role') || '',
    });
  }
}