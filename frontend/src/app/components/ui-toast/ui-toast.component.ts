import { Component, OnDestroy, OnInit } from "@angular/core";
import { Subscription } from "rxjs";
import {
  UiToastKind,
  UiToastMessage,
  UiToastService,
} from "../../services/ui-toast.service";

type RenderedToast = {
  text: string;
  kind: UiToastKind;
};

@Component({
  selector: "app-ui-toast",
  standalone: true,
  template: `
    @if (currentToast) {
      <div
        class="ui-toast"
        [class]="'ui-toast ' + currentToast.kind"
        role="status"
        aria-live="polite"
      >
        <span>{{ currentToast.text }}</span>
        <button
          type="button"
          class="ui-toast-close"
          aria-label="Sluit melding"
          (click)="dismiss()"
        >
          ×
        </button>
      </div>
    }
  `,
  styleUrls: ["./ui-toast.component.css"],
})
export class UiToastComponent implements OnInit, OnDestroy {
  currentToast: RenderedToast | null = null;
  private toastSubscription?: Subscription;
  private hideTimeoutId?: ReturnType<typeof setTimeout>;

  constructor(private uiToastService: UiToastService) {}

  ngOnInit(): void {
    this.toastSubscription = this.uiToastService.toasts$.subscribe((toast) => {
      this.renderToast(toast);
    });
  }

  ngOnDestroy(): void {
    this.toastSubscription?.unsubscribe();
    if (this.hideTimeoutId) {
      clearTimeout(this.hideTimeoutId);
    }
  }

  dismiss(): void {
    this.currentToast = null;
    if (this.hideTimeoutId) {
      clearTimeout(this.hideTimeoutId);
      this.hideTimeoutId = undefined;
    }
  }

  private renderToast(toast: UiToastMessage): void {
    this.currentToast = {
      text: toast.text,
      kind: toast.kind ?? "info",
    };

    if (this.hideTimeoutId) {
      clearTimeout(this.hideTimeoutId);
    }

    this.hideTimeoutId = setTimeout(() => {
      this.currentToast = null;
    }, toast.durationMs ?? 2800);
  }
}
