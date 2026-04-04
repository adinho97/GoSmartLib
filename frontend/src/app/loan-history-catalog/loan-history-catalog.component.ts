import { Component, OnInit } from "@angular/core";
import axios from "axios";
import { LoanService, Loan } from "../services/loan.service";

type Tab = "students" | "books";

interface StudentOption {
  sub: string;
  displayName: string;
}

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
  allStudents: StudentOption[] = [];
  filteredStudents: StudentOption[] = [];
  selectedStudent: StudentOption | null = null;
  studentHistory: Loan[] = [];
  studentLoading = false;
  studentsLoadingError = "";
  studentError = "";
  readonly historyPageSize = 5;
  currentHistoryPage = 1;

  // Books tab (skeleton)
  bookSearch = "";

  constructor(private loanService: LoanService) {}

  ngOnInit() {
    this.loadAllStudents();
  }

  switchTab(tab: Tab) {
    this.currentTab = tab;
    this.resetSearches();
  }

  resetSearches() {
    this.studentSearch = "";
    this.bookSearch = "";
    this.selectedStudent = null;
    this.studentHistory = [];
    this.studentError = "";
    this.currentHistoryPage = 1;
    this.filteredStudents = [...this.allStudents];
  }

  private async loadAllStudents() {
    try {
      const res = await axios.get("/api/gebruikers/leerlingen");
      const students = (res.data || []) as Array<{ sub: string }>;
      const enriched = await Promise.all(
        students.map(async (student) => ({
          sub: student.sub,
          displayName: await this.getDisplayNameForSub(student.sub),
        })),
      );
      this.allStudents = enriched;
      this.filteredStudents = [...this.allStudents];
    } catch (err) {
      console.error("Error loading students", err);
      this.studentsLoadingError = "Fout bij ophalen van leerlingen.";
    }
  }

  onStudentSearchInput() {
    const query = this.studentSearch.trim().toLowerCase();
    this.filteredStudents = query
      ? this.allStudents.filter(
          (s) =>
            s.displayName.toLowerCase().includes(query) ||
            s.sub.toLowerCase().includes(query),
        )
      : [...this.allStudents];
  }

  async selectStudent(student: StudentOption) {
    this.selectedStudent = student;
    this.studentSearch = student.displayName;
    this.studentHistory = [];
    this.studentError = "";
    this.currentHistoryPage = 1;
    this.filteredStudents = [];
    await this.loadStudentHistory();
  }

  private async loadStudentHistory() {
    if (!this.selectedStudent) return;

    this.studentLoading = true;
    this.studentError = "";

    try {
      this.studentHistory = await this.loanService.getLoanHistory(
        this.selectedStudent.sub,
      );
      if (this.studentHistory.length === 0) {
        this.studentError = "Geen uitleenhistoriek gevonden voor deze student.";
      }
    } catch (err) {
      this.studentError = "Fout bij ophalen van historiek. Probeer opnieuw.";
      this.studentHistory = [];
    } finally {
      this.studentLoading = false;
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

  get showStudentDropdown(): boolean {
    return this.studentSearch.length > 0 && this.filteredStudents.length > 0;
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    try {
      const response = await axios.get(`/api/users/${encodeURIComponent(sub)}/profile`);
      const userInfo = response.data as any;
      
      // Try multiple field combinations for maximum compatibility
      const fullname = userInfo.fullname || 
        `${userInfo.name || ''} ${userInfo.surname || ''}`.trim();
      
      return (fullname || userInfo.name || userInfo.givenName || userInfo.given_name || 
              userInfo.familyName || userInfo.sub || '').trim() || sub;
    } catch {
      // Gracefully fallback to sub if API fails
      return sub;
    }
  }

  goToHistoryPage(page: number) {
    this.currentHistoryPage = page;
  }
}
