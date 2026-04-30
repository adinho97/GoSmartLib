import { Component, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { DatePipe } from "@angular/common";
import { LoanService, Loan } from "../services/loan.service";
import { formatUserInfoDisplayName } from "../utils/name-utils";
import axios from "axios";

type PaginationItem = number | "...";

interface LoanWithUserName extends Loan {
  userName: string;
  isOverdue: boolean;
  daysUntilDue: number;
}

@Component({
  selector: "app-loan-overview",
  templateUrl: "./loan-overview.component.html",
  styleUrls: ["./loan-overview.component.css"],
  standalone: false,
  providers: [DatePipe],
})
export class LoanOverviewComponent implements OnInit, OnDestroy {
  allLoans: LoanWithUserName[] = [];
  filteredLoans: LoanWithUserName[] = [];
  loading = true;
  error = "";
  searchQuery = "";
  filterStatus: "all" | "overdue" | "ontime" = "all";

  pageSize = 15;
  currentPage = 1;

  private userNamesCache = new Map<string, string>();
  private refreshInterval: any;

  constructor(private loanService: LoanService) {}

  async ngOnInit() {
    await this.loadLoans();
    // Refresh every 30 seconds
    this.refreshInterval = setInterval(() => {
      this.loadLoans();
    }, 30000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  async loadLoans() {
    this.loading = true;
    this.error = "";
    try {
      const loans = await this.loanService.getAllActiveLoans();

      // Enrich with user names and calculate days until due
      this.allLoans = await Promise.all(
        loans.map(async (loan) => {
          const userName = await this.getDisplayNameForSub(loan.userSub);
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          const dueDate = new Date(loan.dueDate);
          dueDate.setHours(0, 0, 0, 0);
          const daysUntilDue = Math.floor(
            (dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24),
          );
          const isOverdue = daysUntilDue < 0;

          return {
            ...loan,
            userName,
            isOverdue,
            daysUntilDue,
          };
        }),
      );

      this.applyFilters();
    } catch (err) {
      console.error("Error loading loans", err);
      this.error = "Fout bij ophalen van uitleningen.";
    } finally {
      this.loading = false;
    }
  }

  private applyFilters() {
    let filtered = [...this.allLoans];

    // Filter by status
    if (this.filterStatus === "overdue") {
      filtered = filtered.filter((loan) => loan.isOverdue);
    } else if (this.filterStatus === "ontime") {
      filtered = filtered.filter((loan) => !loan.isOverdue);
    }

    // Filter by search query
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(
        (loan) =>
          loan.bookTitel.toLowerCase().includes(query) ||
          loan.userName.toLowerCase().includes(query),
      );
    }

    this.filteredLoans = filtered;
    this.currentPage = 1;
  }

  onSearch() {
    this.applyFilters();
  }

  onFilterChange() {
    this.applyFilters();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredLoans.length / this.pageSize));
  }

  get pagedLoans(): LoanWithUserName[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredLoans.slice(start, start + this.pageSize);
  }

  get visiblePages(): PaginationItem[] {
    return this.buildVisiblePages(this.totalPages, this.currentPage);
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

  goToPage(page: number) {
    if (page < 1 || page > this.totalPages) {
      return;
    }
    this.currentPage = page;
  }

  goToFirstPage() {
    this.goToPage(1);
  }

  goToLastPage() {
    this.goToPage(this.totalPages);
  }

  goToPreviousPage() {
    this.goToPage(this.currentPage - 1);
  }

  goToNextPage() {
    this.goToPage(this.currentPage + 1);
  }

  getDaysLabel(daysUntilDue: number): string {
    if (daysUntilDue < 0) {
      const daysPassed = Math.abs(daysUntilDue);
      return daysPassed === 1 ? "1 dag te laat" : `${daysPassed} dagen te laat`;
    } else if (daysUntilDue === 0) {
      return "Vandaag";
    } else {
      return daysUntilDue === 1
        ? "1 dag resterend"
        : `${daysUntilDue} dagen resterend`;
    }
  }

  refreshLoans() {
    this.loadLoans();
  }

  getOverdueCount(): number {
    return this.filteredLoans.filter((loan) => loan.isOverdue).length;
  }

  getOntimeCount(): number {
    return this.filteredLoans.filter((loan) => !loan.isOverdue).length;
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    if (this.userNamesCache.has(sub)) {
      return this.userNamesCache.get(sub)!;
    }

    try {
      const response = await axios.get(
        `/api/users/${encodeURIComponent(sub)}/profile`,
      );
      const userInfo = response.data as any;
      const displayName = formatUserInfoDisplayName(userInfo, sub);
      this.userNamesCache.set(sub, displayName);
      return displayName;
    } catch {
      return sub;
    }
  }
}
