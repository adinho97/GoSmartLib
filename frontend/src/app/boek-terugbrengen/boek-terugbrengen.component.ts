import { Component, OnDestroy } from "@angular/core";
import {
  LoanService,
  Loan,
  ReturnCondition,
  ReturnLoanRequest,
} from "../services/loan.service";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";

type Leerling = { sub: string; displayName: string };
type Step = "leerling" | "uitleningen";

@Component({
  selector: "app-boek-terugbrengen",
  templateUrl: "./boek-terugbrengen.component.html",
  styleUrls: ["./boek-terugbrengen.component.css"],
  standalone: false,
})
export class BoekTerugbrengenComponent implements OnDestroy {
  step: Step = "leerling";

  filteredLeerlingen: Leerling[] = [];
  leerlingSearch = "";
  selectedLeerling: Leerling | null = null;
  leerlingenLoading = false;
  leerlingError = "";
  hasSearched = false;

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;
  private activeSearchToken = 0;

  get isAdmin(): boolean {
    return !!localStorage.getItem("admin_jwt_token");
  }

  activeLoans: Loan[] = [];
  loansLoading = false;

  returnDialogOpen = false;
  returnDialogLoan: Loan | null = null;
  returnCondition: ReturnCondition = "GOOD";
  returnLostBook = false;
  isReturningLoan = false;

  successMessage = "";
  errorMessage = "";
  today = new Date().toISOString().split("T")[0];

  constructor(
    private loanService: LoanService,
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {}

  ngOnDestroy(): void {
    if (this.searchDebounce !== null) {
      clearTimeout(this.searchDebounce);
    }
  }

  onLeerlingSearch(): void {
    if (this.searchDebounce !== null) clearTimeout(this.searchDebounce);
    const q = this.leerlingSearch.trim();
    if (!q) {
      this.filteredLeerlingen = [];
      this.leerlingenLoading = false;
      this.hasSearched = false;
      this.activeSearchToken++;
      return;
    }
    this.leerlingenLoading = true;
    this.searchDebounce = setTimeout(() => this.runLeerlingSearch(q), 300);
  }

  private async runLeerlingSearch(query: string): Promise<void> {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.leerlingError = "Geen school geselecteerd.";
      this.leerlingenLoading = false;
      this.filteredLeerlingen = [];
      this.hasSearched = true;
      return;
    }
    const token = ++this.activeSearchToken;
    try {
      const results = await this.bookService.searchUsers(query, schoolId);
      if (token !== this.activeSearchToken) return;
      this.filteredLeerlingen = (results || []).map((l: any) => ({
        sub: l.sub,
        displayName: l.displayName || l.sub,
      }));
      this.leerlingError = "";
    } catch {
      if (token !== this.activeSearchToken) return;
      this.filteredLeerlingen = [];
      this.leerlingError = "Zoeken mislukt.";
    } finally {
      if (token === this.activeSearchToken) {
        this.leerlingenLoading = false;
        this.hasSearched = true;
      }
    }
  }

  selectLeerling(leerling: Leerling): void {
    this.selectedLeerling = leerling;
    this.leerlingError = "";
  }

  async confirmLeerling(): Promise<void> {
    if (!this.selectedLeerling) {
      this.leerlingError = "Selecteer een leerling.";
      return;
    }
    this.step = "uitleningen";
    this.loansLoading = true;
    try {
      this.activeLoans = await this.loanService.getActiveLoans(
        this.selectedLeerling.sub,
      );
    } catch {
      this.activeLoans = [];
    } finally {
      this.loansLoading = false;
    }
  }

  goBack(): void {
    this.step = "leerling";
    this.activeLoans = [];
    this.errorMessage = "";
    this.successMessage = "";
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  async openReturnDialog(loan: Loan, event?: MouseEvent): Promise<void> {
    event?.stopPropagation();
    this.returnDialogLoan = loan;
    this.returnLostBook = false;
    this.errorMessage = "";
    this.returnCondition = await this.resolveReturnCondition(loan);
    this.returnDialogOpen = true;
  }

  private async resolveReturnCondition(loan: Loan): Promise<ReturnCondition> {
    try {
      const copies = await this.loanService.getCopiesForBook(loan.bookId);
      const copy = copies.find((c) => c.id === loan.copyId);
      if (!copy) return "GOOD";
      if (copy.condition === "MODERATE") return "MODERATE";
      if (copy.condition === "BAD") return "BAD";
      return "GOOD";
    } catch {
      return "GOOD";
    }
  }

  closeReturnDialog(): void {
    this.returnDialogOpen = false;
    this.returnDialogLoan = null;
    this.returnCondition = "GOOD";
    this.returnLostBook = false;
    this.isReturningLoan = false;
  }

  get returnConditionLabel(): string {
    if (this.returnLostBook) return "verloren";
    if (this.returnCondition === "MODERATE") return "matig";
    if (this.returnCondition === "BAD") return "slecht";
    return "goed";
  }

  async confirmReturnLoan(): Promise<void> {
    if (!this.returnDialogLoan || this.isReturningLoan) return;
    const request: ReturnLoanRequest = {
      condition: this.returnCondition,
      lost: this.returnLostBook,
    };
    this.isReturningLoan = true;
    try {
      const loan = this.returnDialogLoan;
      await this.loanService.returnLoan(loan.id, request);
      this.activeLoans = this.activeLoans.filter((l) => l.id !== loan.id);
      this.successMessage = request.lost
        ? `Boek "${loan.bookTitel}" als verloren gemeld.`
        : `Boek "${loan.bookTitel}" succesvol teruggebracht.`;
      this.closeReturnDialog();
    } catch {
      this.errorMessage = "Terugbrengen mislukt. Probeer opnieuw.";
    } finally {
      this.isReturningLoan = false;
    }
  }
}
