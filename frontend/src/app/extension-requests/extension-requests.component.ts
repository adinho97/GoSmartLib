import { Component, OnInit, Inject } from '@angular/core';
import { LoanService, LoanExtensionRequestDto } from '../services/loan.service';
import { UiToastService } from '../services/ui-toast.service';

@Component({
  selector: 'app-extension-requests',
  templateUrl: './extension-requests.component.html',
  styleUrls: ['./extension-requests.component.css'],
  standalone: false
})
export class ExtensionRequestsComponent implements OnInit {
  requests: LoanExtensionRequestDto[] = [];
  isLoading = true;
  today = new Date().toISOString().split('T')[0];
  
  // Rejection state
  selectedRequestForReject: LoanExtensionRequestDto | null = null;
  rejectionReason = '';

  constructor(
    @Inject(LoanService) private loanService: LoanService,
    @Inject(UiToastService) private toast: UiToastService
  ) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  async loadRequests() {
    this.isLoading = true;
    try {
      this.requests = await this.loanService.getPendingExtensionRequests();
    } finally {
      this.isLoading = false;
    }
  }

  async approve(req: LoanExtensionRequestDto) {
    try {
      await this.loanService.approveExtensionRequest(req.id);
      this.toast.success(`Verlenging voor ${req.bookTitle} goedgekeurd.`);
      this.loadRequests();
    } catch {
      this.toast.error("Goedkeuren mislukt.");
    }
  }

  async reject(req: LoanExtensionRequestDto) {
    this.selectedRequestForReject = req;
    this.rejectionReason = '';
  }

  cancelReject() {
    this.selectedRequestForReject = null;
  }

  async confirmReject() {
    if (!this.selectedRequestForReject) return;
    
    try {
      await this.loanService.rejectExtensionRequest(this.selectedRequestForReject.id, this.rejectionReason);
      this.toast.info("Verlenging afgewezen.");
      this.selectedRequestForReject = null;
      this.loadRequests();
    }
    catch {
      this.toast.error("Afwijzen mislukt.");
    }
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }
}