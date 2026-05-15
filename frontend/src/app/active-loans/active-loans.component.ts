import { Component, Input } from "@angular/core";
import { CommonModule, DatePipe } from "@angular/common";
import { LoanService, Loan } from "../services/loan.service";
import { Router } from "@angular/router";
import { from } from "rxjs";

@Component({
  selector: "app-active-loans",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./active-loans.component.html", 
  styleUrl: "./active-loans.component.css", 
})
export class ActiveLoansComponent {
  @Input() loans: Loan[] = [];

  constructor(
    private router: Router,
    private loanService: LoanService,
  ) {}

  isOverdue(dueDate: string): boolean {
    return new Date(dueDate) < new Date();
  }

  isDueToday(dueDate: string): boolean {
    const today = new Date().toISOString().split("T")[0];
    return dueDate === today;
  }

  goToDetail(bookId: number) {
    this.router.navigate(["/book", bookId]);
  }

  onExtendLoan(loan: Loan, event: Event): void {
    event.stopPropagation(); // Prevent the card's click event from firing

    // For simplicity, extend by 1 week. In a real app, you might have a dialog
    // to select the new due date or a predefined extension period.
    const currentDueDate = new Date(loan.dueDate);
    currentDueDate.setDate(currentDueDate.getDate() + 7); // Extend by 7 days
    const newDueDate = currentDueDate.toISOString().split("T")[0];

    from(this.loanService.updateLoanDueDate(loan.id, newDueDate)).subscribe({
      next: () => {
        // Update the loan in the local array
        const index = this.loans.findIndex((l) => l.id === loan.id);
        if (index !== -1) {
          this.loans[index].dueDate = newDueDate;
        }
        // Optionally, show a success message
        console.log("Loan extended successfully to:", newDueDate);
      },
      error: (err) => {
        console.error("Error extending loan:", err);
        // Optionally, show an error message to the user
      },
    });
  }
}
