import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable, firstValueFrom } from "rxjs";
import { AdminUserListItem, KlasListItem } from "../models/admin-school";

@Injectable({ providedIn: "root" })
export class BibbeheerderService {
  private readonly apiUrl = "/api/bibbeheerder";

  constructor(private readonly http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const sub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    return new HttpHeaders({
      "X-User-Sub": sub,
    });
  }

  getLeerkrachten(): Observable<AdminUserListItem[]> {
    return this.http.get<AdminUserListItem[]>(`${this.apiUrl}/leerkrachten`, {
      headers: this.getHeaders(),
    });
  }

  searchLeerkrachten(query: string): Observable<AdminUserListItem[]> {
    const params = new URLSearchParams({ q: query });
    return this.http.get<AdminUserListItem[]>(
      `${this.apiUrl}/leerkrachten/search?${params.toString()}`,
      { headers: this.getHeaders() },
    );
  }

  getAllUsers(): Observable<AdminUserListItem[]> {
    return this.http.get<AdminUserListItem[]>(`${this.apiUrl}/users`, {
      headers: this.getHeaders(),
    });
  }

  getKlassen(): Observable<KlasListItem[]> {
    return this.http.get<KlasListItem[]>(`${this.apiUrl}/klassen`, {
      headers: this.getHeaders(),
    });
  }

  promoteLeerkracht(userId: number): Observable<AdminUserListItem> {
    return this.http.patch<AdminUserListItem>(
      `${this.apiUrl}/leerkrachten/${userId}/promote`,
      {},
      { headers: this.getHeaders() },
    );
  }

  getLibrariansForSchool(schoolId: number): Promise<AdminUserListItem[]> {
    return firstValueFrom(this.http.get<AdminUserListItem[]>(`${this.apiUrl}/school/${schoolId}`, {
      headers: this.getHeaders(),
    }));
  }
}
