import { Component, OnInit } from '@angular/core';
import { LoanService, Loan, ReturnCondition, ReturnLoanRequest } from '../services/loan.service';
import axios from 'axios';

type Leerling = { sub: string; displayName: string };
type Step = 'leerling' | 'uitleningen';

@Component({
  selector: 'app-boek-terugbrengen',
  templateUrl: './boek-terugbrengen.component.html',
  styleUrls: ['./boek-terugbrengen.component.css'],
  standalone: false,
})
export class BoekTerugbrengenComponent implements OnInit {
  step: Step = 'leerling';

  leerlingen: Leerling[] = [];
  filteredLeerlingen: Leerling[] = [];
  leerlingSearch = '';
  selectedLeerling: Leerling | null = null;
  leerlingenLoading = false;
  leerlingError = '';

  activeLoans: Loan[] = [];
  loansLoading = false;

  returnDialogOpen = false;
  returnDialogLoan: Loan | null = null;
  returnCondition: ReturnCondition = 'GOOD';
  returnLostBook = false;
  isReturningLoan = false;

  successMessage = '';
  errorMessage = '';
  today = new Date().toISOString().split('T')[0];

  constructor(private loanService: LoanService) {}

  ngOnInit(): void {
    this.loadLeerlingen();
  }

  async loadLeerlingen(): Promise<void> {
    this.leerlingenLoading = true;
    try {
      const res = await axios.get('/api/gebruikers/leerlingen');
      const students = (res.data || []) as Array<{ sub: string; displayName?: string }>;
      this.leerlingen = students.map((l) => ({
        sub: l.sub,
        displayName: l.displayName || l.sub,
      }));
      this.filteredLeerlingen = [...this.leerlingen];
    } catch {
      this.leerlingError = 'Leerlingen laden mislukt.';
    } finally {
      this.leerlingenLoading = false;
    }
  }

  onLeerlingSearch(): void {
    const q = this.leerlingSearch.trim().toLowerCase();
    this.filteredLeerlingen = q
      ? this.leerlingen.filter(
          (l) => l.displayName.toLowerCase().includes(q) || l.sub.toLowerCase().includes(q),
        )
      : [...this.leerlingen];
  }

  selectLeerling(leerling: Leerling): void {
    this.selectedLeerling = leerling;
    this.leerlingError = '';
  }

  async confirmLeerling(): Promise<void> {
    if (!this.selectedLeerling) {
      this.leerlingError = 'Selecteer een leerling.';
      return;
    }
    this.step = 'uitleningen';
    this.loansLoading = true;
    try {
      this.activeLoans = await this.loanService.getActiveLoans(this.selectedLeerling.sub);
    } catch {
      this.activeLoans = [];
    } finally {
      this.loansLoading = false;
    }
  }

  goBack(): void {
    this.step = 'leerling';
    this.activeLoans = [];
    this.errorMessage = '';
    this.successMessage = '';
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  async openReturnDialog(loan: Loan, event?: MouseEvent): Promise<void> {
    event?.stopPropagation();
    this.returnDialogLoan = loan;
    this.returnLostBook = false;
    this.errorMessage = '';
    this.returnCondition = await this.resolveReturnCondition(loan);
    this.returnDialogOpen = true;
  }

  private async resolveReturnCondition(loan: Loan): Promise<ReturnCondition> {
    try {
      const copies = await this.loanService.getCopiesForBook(loan.bookId);
      const copy = copies.find((c) => c.id === loan.copyId);
      if (!copy) return 'GOOD';
      if (copy.condition === 'MODERATE') return 'MODERATE';
      if (copy.condition === 'BAD') return 'BAD';
      return 'GOOD';
    } catch {
      return 'GOOD';
    }
  }

  closeReturnDialog(): void {
    this.returnDialogOpen = false;
    this.returnDialogLoan = null;
    this.returnCondition = 'GOOD';
    this.returnLostBook = false;
    this.isReturningLoan = false;
  }

  get returnConditionLabel(): string {
    if (this.returnLostBook) return 'verloren';
    if (this.returnCondition === 'MODERATE') return 'matig';
    if (this.returnCondition === 'BAD') return 'slecht';
    return 'goed';
  }

  async confirmReturnLoan(): Promise<void> {
    if (!this.returnDialogLoan || this.isReturningLoan) return;
    const request: ReturnLoanRequest = { condition: this.returnCondition, lost: this.returnLostBook };
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
      this.errorMessage = 'Terugbrengen mislukt. Probeer opnieuw.';
    } finally {
      this.isReturningLoan = false;
    }
  }
}
