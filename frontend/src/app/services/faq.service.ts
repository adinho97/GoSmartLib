import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";

export type FaqItem = {
  id?: number;
  question: string;
  answer: string;
  sortOrder?: number;
};

@Injectable({ providedIn: "root" })
export class FaqService {
  private readonly apiUrl = "/api/faq";

  constructor(private http: HttpClient) {}

  getAll(): Observable<FaqItem[]> {
    return this.http.get<FaqItem[]>(this.apiUrl);
  }

  create(faq: FaqItem): Observable<FaqItem> {
    return this.http.post<FaqItem>(this.apiUrl, faq, {
      headers: this.authHeaders(),
    });
  }

  update(id: number, faq: FaqItem): Observable<FaqItem> {
    return this.http.put<FaqItem>(`${this.apiUrl}/${id}`, faq, {
      headers: this.authHeaders(),
    });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: this.authHeaders(),
    });
  }

  private authHeaders(): HttpHeaders {
    const sub = localStorage.getItem("sub") || "";
    const role = localStorage.getItem("role") || "";
    return new HttpHeaders({
      "X-User-Sub": sub,
      "X-User-Role": role,
    });
  }
}
