import { Component, OnInit } from "@angular/core";
import { BookService } from "../services/book.service";

@Component({
  selector: "app-mijn-taken",
  templateUrl: "./mijn-taken.component.html",
  styleUrls: ["./mijn-taken.component.css"],
  standalone: false,
})
export class MijnTakenComponent implements OnInit {
  pendingReportsCount = 0;

  constructor(private bookService: BookService) {}

  ngOnInit(): void {
    if (this.isLibrarian) {
      this.loadPendingReportsCount();
    }
  }

  get isLibrarian(): boolean {
    return (localStorage.getItem("role") || "")
      .toLowerCase()
      .includes("bibbeheerder");
  }

  private async loadPendingReportsCount(): Promise<void> {
    try {
      this.pendingReportsCount =
        await this.bookService.getReportedReviewCount();
    } catch (error) {
      this.pendingReportsCount = 0;
    }
  }
}
