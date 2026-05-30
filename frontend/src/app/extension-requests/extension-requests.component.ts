import { Component, OnInit, Inject } from "@angular/core";
import { LoanService, LoanExtensionRequestDto } from "../services/loan.service";
import { UiToastService } from "../services/ui-toast.service";

@Component({
  selector: "app-extension-requests",
  templateUrl: "./extension-requests.component.html",
  styleUrls: ["./extension-requests.component.css"],
  standalone: false,
})
export class ExtensionRequestsComponent implements OnInit {
  requests: LoanExtensionRequestDto[] = [];
  isLoading = true;
  today = new Date().toISOString().split("T")[0];
  visibleCount = 10;

  // Rejection state
  selectedRequestForReject: LoanExtensionRequestDto | null = null;
  rejectionReason = "";

  // Approval state
  selectedRequestForApprove: LoanExtensionRequestDto | null = null;
  approveDueDate = "";
  isProcessing = false;
  messageSentSuccess = "";
  messageSentError = "";

  constructor(
    @Inject(LoanService) private loanService: LoanService,
    @Inject(UiToastService) private toast: UiToastService,
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

  showMore() {
    this.visibleCount += 10;
  }

  startApprove(req: LoanExtensionRequestDto) {
    this.selectedRequestForApprove = req;
    this.messageSentSuccess = "";
    this.messageSentError = "";
    // Default to 14 days after the current due date
    const current = new Date(req.currentDueDate);
    current.setDate(current.getDate() + 14);
    this.approveDueDate = current.toISOString().split("T")[0];
  }

  cancelApprove() {
    this.selectedRequestForApprove = null;
    this.isProcessing = false;
  }

  async confirmApprove() {
    if (!this.selectedRequestForApprove || this.isProcessing) return;
    this.isProcessing = true;
    this.messageSentError = "";

    try {
      await this.loanService.approveExtensionRequest(
        this.selectedRequestForApprove.id,
        this.approveDueDate,
      );
      this.messageSentSuccess = `Verlenging voor ${this.selectedRequestForApprove.bookTitle} succesvol verwerkt.`;
      setTimeout(() => {
        this.loadRequests();
        this.cancelApprove();
      }, 1500);
    } catch {
      this.messageSentError =
        "Fout bij verwerken aanvraag. Probeer het later opnieuw.";
    } finally {
      this.isProcessing = false;
    }
  }

  async reject(req: LoanExtensionRequestDto) {
    this.selectedRequestForReject = req;
    this.rejectionReason = "";
  }

  cancelReject() {
    this.selectedRequestForReject = null;
  }

  async confirmReject() {
    if (!this.selectedRequestForReject) return;

    try {
      await this.loanService.rejectExtensionRequest(
        this.selectedRequestForReject.id,
        this.rejectionReason,
      );
      this.toast.info("Verlenging afgewezen.");
      this.selectedRequestForReject = null;
      this.loadRequests();
    } catch {
      this.toast.error("Afwijzen mislukt.");
    }
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }
}
