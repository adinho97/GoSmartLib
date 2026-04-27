import { Component, OnInit } from "@angular/core";
import axios from "axios";
import {
  BookStateOverview,
  LoanConditionOverview,
  LoanService,
  LostCopyOverview,
  WorsenedReturn,
} from "../services/loan.service";
import { composeFullName, inferNameParts } from "../utils/name-utils";

type PaginationItem = number | "...";

@Component({
  selector: "app-loan-condition-overview",
  templateUrl: "./loan-condition-overview.component.html",
  styleUrls: ["./loan-condition-overview.component.css"],
  standalone: false,
})
export class LoanConditionOverviewComponent implements OnInit {
  loading = true;
  error = "";

  worsenedReturns: WorsenedReturn[] = [];
  bookStates: BookStateOverview[] = [];
  lostCopies: LostCopyOverview[] = [];

  // Search and pagination for bookStates section
  bookSearch = "";
  bookPage = 1;
  readonly bookPageSize = 10;

  private userNames = new Map<string, string>();
  private loadingUserSubs = new Set<string>();

  constructor(private loanService: LoanService) {}

  async ngOnInit() {
    await this.loadOverview();
  }

  async loadOverview() {
    this.loading = true;
    this.error = "";

    try {
      const overview = await this.loanService.getConditionOverview();
      this.worsenedReturns = overview.worsenedReturns || [];
      this.bookStates = overview.bookStates || [];
      this.lostCopies = overview.lostCopies || [];
      await this.populateUserNames(this.worsenedReturns);
    } catch {
      this.error = "Overzicht laden mislukt.";
    } finally {
      this.loading = false;
    }
  }

  get worsenedReturnsCount(): number {
    return this.worsenedReturns.length;
  }

  get lostCopiesCount(): number {
    return this.lostCopies.length;
  }

  get booksWithIssuesCount(): number {
    return this.bookStates.filter(
      (book) => book.damagedCopies > 0 || book.lostCopies > 0,
    ).length;
  }

  get filteredBookStates(): BookStateOverview[] {
    const query = this.bookSearch.trim().toLowerCase();
    const filtered = query
      ? this.bookStates.filter((book) =>
          book.bookTitel.toLowerCase().includes(query),
        )
      : this.bookStates;

    // Reset to page 1 if search changes
    if (query && this.bookPage > 1) {
      this.bookPage = 1;
    }

    const start = (this.bookPage - 1) * this.bookPageSize;
    const end = start + this.bookPageSize;
    return filtered.slice(start, end);
  }

  get bookStatesTotalPages(): number {
    const query = this.bookSearch.trim().toLowerCase();
    const filtered = query
      ? this.bookStates.filter((book) =>
          book.bookTitel.toLowerCase().includes(query),
        )
      : this.bookStates;
    return Math.ceil(filtered.length / this.bookPageSize);
  }

  get visibleBookPages(): PaginationItem[] {
    return this.buildVisiblePages(this.bookStatesTotalPages, this.bookPage);
  }

  conditionLabel(
    condition: "GOOD" | "MODERATE" | "BAD" | null | undefined,
  ): string {
    if (condition === "GOOD") return "Goed";
    if (condition === "MODERATE") return "Matig";
    if (condition === "BAD") return "Slecht";
    return "Onbekend";
  }

  statusLabel(
    status: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST" | null | undefined,
  ): string {
    if (status === "AVAILABLE") return "Beschikbaar";
    if (status === "LOANED") return "Uitgeleend";
    if (status === "DAMAGED") return "Beschadigd";
    if (status === "LOST") return "Vermist";
    return "Onbekend";
  }

  getUserName(sub: string): string {
    const cached = this.userNames.get(sub);
    if (cached) {
      return cached;
    }
    if (!this.loadingUserSubs.has(sub)) {
      this.loadingUserSubs.add(sub);
      this.getDisplayNameForSub(sub)
        .then((name) => {
          this.userNames.set(sub, name);
        })
        .finally(() => {
          this.loadingUserSubs.delete(sub);
        });
    }
    return sub;
  }

  isLostReturn(item: WorsenedReturn): boolean {
    return item.returnedStatus === "LOST";
  }

  onBookSearch(): void {
    this.bookPage = 1;
  }

  goToBookPage(page: number | string): void {
    if (
      typeof page === "number" &&
      page >= 1 &&
      page <= this.bookStatesTotalPages
    ) {
      this.bookPage = page;
    }
  }

  goToFirstBookPage(): void {
    this.goToBookPage(1);
  }

  goToPreviousBookPage(): void {
    if (this.bookPage > 1) {
      this.goToBookPage(this.bookPage - 1);
    }
  }

  goToNextBookPage(): void {
    if (this.bookPage < this.bookStatesTotalPages) {
      this.goToBookPage(this.bookPage + 1);
    }
  }

  goToLastBookPage(): void {
    this.goToBookPage(this.bookStatesTotalPages);
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

  private async populateUserNames(rows: WorsenedReturn[]) {
    const uniqueSubs = Array.from(
      new Set(rows.map((row) => row.userSub).filter(Boolean)),
    );

    await Promise.all(
      uniqueSubs.map(async (sub) => {
        const displayName = await this.getDisplayNameForSub(sub);
        this.userNames.set(sub, displayName);
      }),
    );
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    try {
      const response = await axios.get(
        `/api/users/${encodeURIComponent(sub)}/profile`,
      );
      const userInfo = response.data as any;

      const rawFirstName =
        userInfo.actualUserFirstName ||
        userInfo.givenName ||
        userInfo.given_name ||
        userInfo.firstName ||
        userInfo.firstname ||
        "";

      const rawLastName =
        userInfo.actualUserSurname ||
        userInfo.actualUserLastName ||
        userInfo.familyName ||
        userInfo.family_name ||
        userInfo.lastName ||
        userInfo.lastname ||
        userInfo.surname ||
        "";

      const { firstName, lastName } = inferNameParts(
        rawFirstName,
        rawLastName,
        [
          userInfo.fullname,
          userInfo.displayName,
          `${userInfo.name || ""} ${userInfo.surname || ""}`.trim(),
          `${userInfo.givenName || userInfo.given_name || ""} ${
            userInfo.familyName || userInfo.family_name || ""
          }`.trim(),
          userInfo.name,
          userInfo.preferred_username,
        ],
      );

      const fullName = composeFullName(firstName, lastName);
      if (fullName) {
        return fullName;
      }

      const fallbackFullNameCandidates = [
        userInfo.fullname,
        userInfo.displayName,
        `${userInfo.name || ""} ${userInfo.surname || ""}`.trim(),
        `${userInfo.givenName || userInfo.given_name || ""} ${
          userInfo.familyName || userInfo.family_name || ""
        }`.trim(),
      ];

      const fallbackFullName = fallbackFullNameCandidates.find((candidate) => {
        const normalized = (candidate || "").trim();
        if (!normalized) {
          return false;
        }
        const parts = normalized.split(/\s+/).filter(Boolean);
        return parts.length >= 2;
      });

      if (fallbackFullName) {
        return fallbackFullName.trim();
      }

      return (
        (
          rawFirstName ||
          rawLastName ||
          userInfo.name ||
          userInfo.givenName ||
          userInfo.given_name ||
          ""
        ).trim() || sub
      );
    } catch {
      return sub;
    }
  }
}
