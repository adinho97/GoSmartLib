import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import {
  LeaderboardData,
  LeaderboardEntry,
} from "../leaderboard/leaderboard.component"; // Import interfaces

@Injectable({
  providedIn: "root",
})
export class LeaderboardService {
  private apiUrl = "/api/leaderboard";

  constructor(private http: HttpClient) {}

  getLeaderboardData(klasId?: number): Observable<LeaderboardData> {
    let url = this.apiUrl;
    if (klasId) {
      url += `?klasId=${klasId}`;
    }
    return this.http.get<LeaderboardData>(url);
  }
}
