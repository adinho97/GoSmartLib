import { Component, OnInit } from "@angular/core";
import { LoanService, Loan } from "../services/loan.service";

type Tab = "students" | "books";

@Component({
  selector: "app-loan-history-catalog",
  templateUrl: "./loan-history-catalog.component.html",
  styleUrls: ["./loan-history-catalog.component.css"],
  standalone: false,
})
export class LoanHistoryCatalogComponent implements OnInit {
  currentTab: Tab = "students";

  // Students tab
  studentSearch = "";
  selectedStudentSub = "";
  selectedStudentName = "";
  studentHistory: Loan[] = [];
  studentLoading = false;
  studentError = "";
  readonly historyPageSize = 5;
  currentHistoryPage = 1;

  // Books tab (skeleton)
  bookSearch = "";

  constructor(private loanService: LoanService) {}

  ngOnInit() {}

  switchTab(tab: Tab) {
    this.currentTab = tab;
    this.resetSearches();
  }

  resetSearches() {
    this.studentSearch = "";
    this.bookSearch = "";
    this.selectedStudentSub = "";
    this.selectedStudentName = "";
    this.studentHistory = [];
    this.studentError = "";
    this.currentHistoryPage = 1;
  }

  async searchStudent() {
    if (!this.studentSearch.trim()) {
      this.studentError = "Voer alstublieft een student sub in.";
      return;
    }

    this.studentLoading = true;
    this.studentError = "";
    this.studentHistory = [];

    try {
      this.selectedStudentSub = this.studentSearch.trim();
      this.studentHistory = await this.loanService.getLoanHistory(
        this.selectedStudentSub,
      );
      
      // Try to get student name from logged in users or use sub as fallback
      this.selectedStudentName = this.selectedStudentSub;
      
      if (this.studentHistory.length === 0) {
        this.studentError = "Geen uitleenhistoriek gevonden voor deze student.";
      }
    } catch (err) {
      this.studentError = "Fout bij ophalen van historiek. Probeer opnieuw.";
      this.studentHistory = [];
    } finally {
      this.studentLoading = false;
      this.currentHistoryPage = 1;
    }
  }

  get totalHistoryPages(): number {
    return Math.max(
      1,
      Math.ceil(this.studentHistory.length / this.historyPageSize),
    );
  }

  get historyPageNumbers(): number[] {
    return Array.from({ length: this.totalHistoryPages }, (_, i) => i + 1);
  }

  get pagedStudentHistory(): Loan[] {
    const start = (this.currentHistoryPage - 1) * this.historyPageSize;
    return this.studentHistory.slice(start, start + this.historyPageSize);
  }

  goToHistoryPage(page: number) {
    this.currentHistoryPage = page;
  }
}
