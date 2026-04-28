import { Component, OnInit } from "@angular/core";
import axios from "axios";
import { LoanService, Loan } from "../services/loan.service";
import {
  composeFullName,
  inferNameParts,
  formatUserInfoDisplayName,
} from "../utils/name-utils";

type Tab = "students" | "books";

interface StudentOption {
  sub: string;
  displayName: string;
}

interface BookStat {
  id: number;
  titel: string;
  auteur: string;
  cover: string;
  loanCount: number;
  totalCopies: number;
  availableCopies: number;
}

type PaginationItem = number | "...";

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

  // Books tab
  allBooks: BookStat[] = [];
  booksLoading = false;
  booksError = "";
  bookSearch = "";
  readonly booksPageSize = 10;
  currentBooksPage = 1;

  constructor(private loanService: LoanService) {}

  ngOnInit() {
    this.loadAllStudents();
  }

  switchTab(tab: Tab) {
    this.currentTab = tab;
    this.resetSearches();

    // Load books when switching to books tab
    if (tab === "books" && this.allBooks.length === 0) {
      this.loadAllBooks();
    }
  }

  resetSearches() {
    this.studentSearch = "";
    this.selectedStudent = null;
    this.studentHistory = [];
    this.studentError = "";
    this.currentHistoryPage = 1;
    this.filteredStudents = [...this.allStudents];
    this.currentBooksPage = 1;
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
    this.filteredStudents = this.allStudents.filter((s) =>
      query === ""
        ? true
        : s.displayName.toLowerCase().includes(query) ||
          s.sub.toLowerCase().includes(query),
    );
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

  get visibleHistoryPages(): PaginationItem[] {
    return this.buildVisiblePages(
      this.totalHistoryPages,
      this.currentHistoryPage,
    );
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
      const response = await axios.get(
        `/api/users/${encodeURIComponent(sub)}/profile`,
      );
      const userInfo = response.data as any;
      return formatUserInfoDisplayName(userInfo, sub);
    } catch {
      return sub;
    }
  }

  goToHistoryPage(page: number) {
    this.currentHistoryPage = Math.min(
      this.totalHistoryPages,
      Math.max(1, page),
    );
  }

  goToPreviousHistoryPage() {
    this.goToHistoryPage(this.currentHistoryPage - 1);
  }

  goToNextHistoryPage() {
    this.goToHistoryPage(this.currentHistoryPage + 1);
  }

  private buildVisiblePages(
    totalPages: number,
    currentPage: number,
  ): PaginationItem[] {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    const candidates = new Set<number>([
      1,
      2,
      totalPages - 1,
      totalPages,
      currentPage - 1,
      currentPage,
      currentPage + 1,
    ]);

    const pages = Array.from(candidates)
      .filter((page) => page >= 1 && page <= totalPages)
      .sort((left, right) => left - right);

    const result: PaginationItem[] = [];
    for (let index = 0; index < pages.length; index++) {
      const page = pages[index];
      if (index > 0) {
        const previousPage = pages[index - 1];
        if (page - previousPage > 1) {
          result.push("...");
        }
      }
      result.push(page);
    }

    return result;
  }

  // Books tab methods
  private async loadAllBooks() {
    this.booksLoading = true;
    this.booksError = "";

    try {
      const response = await axios.get("/api/boeken/stats");
      this.allBooks = response.data as BookStat[];
    } catch (err) {
      console.error("Error loading books", err);
      this.booksError = "Fout bij ophalen van boeken.";
      this.allBooks = [];
    } finally {
      this.booksLoading = false;
    }
  }

  get maxLoanCount(): number {
    return this.allBooks.length > 0
      ? Math.max(...this.allBooks.map((b) => b.loanCount))
      : 1;
  }

  getBarWidth(loanCount: number): number {
    return (loanCount / this.maxLoanCount) * 100;
  }

  shouldShowRecommendation(book: BookStat): boolean {
    const avgLoanThreshold = this.maxLoanCount * 0.5;
    const hasLowStock = book.availableCopies < book.totalCopies * 0.3;
    return book.loanCount >= avgLoanThreshold && hasLowStock;
  }

  get filteredBooks(): BookStat[] {
    const query = this.bookSearch.trim().toLowerCase();
    if (!query) {
      return this.allBooks;
    }
    return this.allBooks.filter(
      (book) =>
        book.titel.toLowerCase().includes(query) ||
        book.auteur.toLowerCase().includes(query),
    );
  }

  onBooksSearchInput() {
    this.currentBooksPage = 1;
  }

  get totalBooksPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredBooks.length / this.booksPageSize),
    );
  }

  get booksPageNumbers(): number[] {
    return Array.from({ length: this.totalBooksPages }, (_, i) => i + 1);
  }

  get pagedBooks(): BookStat[] {
    const start = (this.currentBooksPage - 1) * this.booksPageSize;
    return this.filteredBooks.slice(start, start + this.booksPageSize);
  }

  goToBooksPage(page: number) {
    this.currentBooksPage = page;
  }
}
