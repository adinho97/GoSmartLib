import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";
import { AuthContextService } from "./auth-context.service";
import {
  LeaderboardData,
  LeaderboardEntry,
} from "../leaderboard/leaderboard.component"; // Import interfaces

@Injectable({
  providedIn: "root",
})
export class LeaderboardService {
  private apiUrl = "/api/leaderboard";

  constructor(
    private http: HttpClient,
    private authContext: AuthContextService,
  ) {}

  getLeaderboardData(klasId?: number): Observable<LeaderboardData> {
    let url = this.apiUrl;
    if (klasId) {
      url += `?klasId=${klasId}`;
    }
    const headers = new HttpHeaders({
      "X-User-Sub": this.authContext.getEffectiveSub(),
    });
    return this.http.get<LeaderboardData>(url, { headers });
  }
}
