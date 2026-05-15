import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { AdminUserListItem } from "../models/admin-school";

@Injectable({ providedIn: "root" })
export class BibbeheerderService {
  private readonly apiUrl = "/api/bibbeheerder";

  constructor(private readonly http: HttpClient) {}

  getLeerkrachten(): Observable<AdminUserListItem[]> {
    return this.http.get<AdminUserListItem[]>(`${this.apiUrl}/leerkrachten`);
  }

  promoteLeerkracht(userId: number): Observable<AdminUserListItem> {
    return this.http.patch<AdminUserListItem>(
      `${this.apiUrl}/leerkrachten/${userId}/promote`,
      {},
    );
  }
}
