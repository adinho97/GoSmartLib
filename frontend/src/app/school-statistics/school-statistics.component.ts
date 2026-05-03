import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule } from "@angular/router";
import { BookService, SchoolStatistics } from "../services/book.service";
import { SchoolService } from "../services/school.service";

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
    } catch (err) {
      console.error("Failed to load school statistics", err);
      this.error = "Fout bij het laden van de statistieken.";
    } finally {
      this.isLoading = false;
    }
  }
}
