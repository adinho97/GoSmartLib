import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule } from "@angular/router";
import { BookService, SchoolStatistics } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { formatUserInfoDisplayName } from "../utils/name-utils";
import axios from "axios";

@Component({
  selector: "app-school-statistics",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./school-statistics.component.html",
  styleUrls: ["./school-statistics.component.css"],
})
export class SchoolStatisticsComponent implements OnInit {
  stats: SchoolStatistics | null = null;
  isLoading = true;
  error = "";

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.error = "Geen school geselecteerd. Kies eerst een school.";
      this.isLoading = false;
      return;
    }

    try {
      this.stats = await this.bookService.getSchoolStatistics(schoolId);

      if (this.stats?.topReader?.sub) {
        this.stats.topReader.displayName = await this.getDisplayNameForSub(
          this.stats.topReader.sub
        );
      }
    } catch (err) {
      console.error("Failed to load school statistics", err);
      this.error = "Fout bij het laden van de statistieken.";
    } finally {
      this.isLoading = false;
    }
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    try {
      const profile = await axios.get(`/api/users/${encodeURIComponent(sub)}/profile`);
      return formatUserInfoDisplayName(profile.data, sub);
    } catch {
      return sub;
    }
  }
}
