import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { LeaderboardService } from "../services/leaderboard.service"; // Assuming a service for data fetching
import { formatUserInfoDisplayName } from "../utils/name-utils";
import axios from "axios";

// Define interfaces for better type safety and clarity
export interface LeaderboardEntry {
  rank: number;
  displayName: string;
  count: number; // Number of books
  isCurrentUser?: boolean; // Optional, to mark the current user if they are in the top 10
}

export interface LeaderboardData {
  topClassReaders: LeaderboardEntry[];
  topSchoolReaders: LeaderboardEntry[];
  userClassRank?: LeaderboardEntry; // User's rank in class, if not in top 10 or if they are
  userSchoolRank?: LeaderboardEntry; // User's rank in school, if not in top 10 or if they are
  availableClasses?: { id: number; naam: string }[];
}

@Component({
  selector: "app-leaderboard",
  templateUrl: "./leaderboard.component.html",
  styleUrls: ["./leaderboard.component.scss"],
})
export class LeaderboardComponent implements OnInit {
  isLoading: boolean = true;
  error: string | null = null;
  leaderboardData: LeaderboardData | null = null;
  selectedKlasId: number | null = null;
  isTeacherOrLibrarian: boolean = false;

  constructor(
    private router: Router,
    private leaderboardService: LeaderboardService, // Inject the service
  ) {}

  ngOnInit(): void {
    const role = localStorage.getItem("role") || "";
    this.isTeacherOrLibrarian =
      role === "leerkracht" || role === "bibbeheerder";
    this.fetchLeaderboardData();
  }

  /**
   * Fetches leaderboard data from the service.
   * Handles loading state and error messages.
   */
  fetchLeaderboardData(): void {
    this.isLoading = true;
    this.error = null;
    this.leaderboardService
      .getLeaderboardData(this.selectedKlasId ?? undefined)
      .subscribe({
        next: async (data: LeaderboardData) => {
          this.leaderboardData = data;
          await this.resolveNames();
          this.isLoading = false;
        },
        error: (err) => {
          console.error("Error fetching leaderboard data:", err);
          this.error =
            "Er is een fout opgetreden bij het laden van de ranglijsten. Probeer het later opnieuw.";
          this.isLoading = false;
        },
      });
  }

  private async resolveNames(): Promise<void> {
    if (!this.leaderboardData) return;

    const resolveEntry = async (entry: LeaderboardEntry) => {
      entry.displayName = await this.getDisplayNameForSub(entry.displayName);
    };

    const promises = [
      ...this.leaderboardData.topClassReaders.map(resolveEntry),
      ...this.leaderboardData.topSchoolReaders.map(resolveEntry),
    ];

    if (this.leaderboardData.userClassRank)
      promises.push(resolveEntry(this.leaderboardData.userClassRank));
    if (this.leaderboardData.userSchoolRank)
      promises.push(resolveEntry(this.leaderboardData.userSchoolRank));

    await Promise.all(promises);
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    try {
      const profile = await axios.get(
        `/api/users/${encodeURIComponent(sub)}/profile`,
      );
      return formatUserInfoDisplayName(profile.data, sub);
    } catch {
      return sub;
    }
  }

  /**
   * Triggered when a teacher/librarian selects a different class.
   */
  onKlasChange(event: any): void {
    this.selectedKlasId = event.target.value
      ? Number(event.target.value)
      : null;
    this.fetchLeaderboardData();
  }

  /**
   * Navigates back to the previous page or a default dashboard.
   */
  goBack(): void {
    // You might want to use `this.location.back()` from `@angular/common`
    // or navigate to a specific route like '/dashboard'
    this.router.navigate(["/dashboard"]); // Adjust this route as per your application's navigation
  }

  /**
   * Checks if the current user's rank is within the top 10 for the class leaderboard.
   */
  isUserInTopClass(userRank: LeaderboardEntry | undefined): boolean {
    return userRank !== undefined && userRank.rank > 0 && userRank.rank <= 10;
  }

  /**
   * Checks if the current user's rank is within the top 10 for the school leaderboard.
   */
  isUserInTopSchool(userRank: LeaderboardEntry | undefined): boolean {
    return userRank !== undefined && userRank.rank > 0 && userRank.rank <= 10;
  }
}
