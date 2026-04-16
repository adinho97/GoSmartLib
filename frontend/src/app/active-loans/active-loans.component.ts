import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoanService, Loan } from "../services/loan.service";
import { Router } from '@angular/router';

@Component({
  selector: 'app-active-loans',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './active-loans.component.html',
  styleUrl: './active-loans.component.css'
})
export class ActiveLoansComponent {
  @Input() loans: Loan[] = [];

  constructor(private router: Router) {}

  isOverdue(dueDate: string): boolean {
    return new Date(dueDate) < new Date();
  }

  isDueToday(dueDate: string): boolean {
    const today = new Date().toISOString().split('T')[0];
    return dueDate === today;
  }

  goToDetail(bookId: number) {
    this.router.navigate(['/book', bookId]);
  }
}