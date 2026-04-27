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

  private userNames = new Map<string, string>();

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
    return this.userNames.get(sub) || sub;
  }

  isLostReturn(item: WorsenedReturn): boolean {
    return item.returnedStatus === "LOST";
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
          userInfo.name,
          userInfo.displayName,
          userInfo.preferred_username,
        ],
      );

      const fullName = composeFullName(firstName, lastName);
      return fullName || sub;
    } catch {
      return sub;
    }
  }
}
