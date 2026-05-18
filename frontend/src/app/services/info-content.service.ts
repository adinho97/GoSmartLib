import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type Sectie = 'STAP' | 'FEATURE' | 'TIP' | 'FAQ';

export type InfoContentItem = {
  id?: number;
  sectie: Sectie;
  titel?: string | null;
  inhoud: string;
  sortOrder?: number;
  schoolId?: number | null;
};

@Injectable({ providedIn: 'root' })
export class InfoContentService {
  private readonly apiUrl = '/api/info-content';

  constructor(private http: HttpClient) {}

  getAll(sectie: Sectie, schoolId?: number | null): Observable<InfoContentItem[]> {
    let params = new HttpParams().set('sectie', sectie);
    if (schoolId != null) params = params.set('schoolId', schoolId.toString());
    return this.http.get<InfoContentItem[]>(this.apiUrl, { params });
  }

  create(item: InfoContentItem): Observable<InfoContentItem> {
    return this.http.post<InfoContentItem>(this.apiUrl, item);
  }

  update(id: number, item: InfoContentItem): Observable<InfoContentItem> {
    return this.http.put<InfoContentItem>(`${this.apiUrl}/${id}`, item);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  hide(id: number, schoolId?: number | null): Observable<void> {
    let params = new HttpParams();
    if (schoolId != null) params = params.set('schoolId', schoolId.toString());
    return this.http.post<void>(`${this.apiUrl}/${id}/hide`, null, { params });
  }
}
