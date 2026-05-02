import { Component, OnInit } from "@angular/core";
import { BookService, SchoolStatistics } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { UiToastService } from "../services/ui-toast.service";

@Component({
  selector: "app-statistics",
  templateUrl: "./statistics.component.html",
  styleUrls: ["./statistics.component.css"],
  standalone: false,
})
export class StatisticsComponent implements OnInit {
  stats: SchoolStatistics | null = null;
  isLoading = true;

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
    private uiToastService: UiToastService,
  ) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  async loadStatistics() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.uiToastService.error("Geen school geselecteerd.");
      this.isLoading = false;
      return;
    }

    try {
      this.stats = await this.bookService.getSchoolStatistics(schoolId);
    } catch (error) {
      console.error("Fout bij het laden van statistieken:", error);
      this.uiToastService.error("Laden van statistieken mislukt.");
    } finally {
      this.isLoading = false;
    }
  }
}
