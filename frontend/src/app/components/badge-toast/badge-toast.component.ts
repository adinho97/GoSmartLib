import { Component, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import {
  BadgeNotificationService,
  BadgeUnlocked,
} from "../../services/badge-notification.service";
import { Subscription } from "rxjs";

@Component({
  selector: "app-badge-toast",
  template: `
    @if (visibleBadge) {
      <div class="badge-toast-global" role="status" aria-live="polite">
        <button
          type="button"
          class="badge-toast-close-btn"
          aria-label="Sluit badge melding"
          (click)="dismissBadge()"
        >
          ×
        </button>
        <div class="badge-toast-content-center">
          <div class="badge-toast-icon-large">{{ visibleBadge.icon }}</div>
          <h3>Nieuwe badge ontgrendeld!</h3>
          <p>{{ visibleBadge.title }}</p>
          <p class="badge-toast-subtitle">Goed bezig, hou je streak vol.</p>
        </div>
      </div>
    }
  `,
  styleUrls: ["./badge-toast.component.css"],
  standalone: true,
})
export class BadgeToastComponent implements OnInit, OnDestroy {
  visibleBadge: BadgeUnlocked | null = null;
  private badgeSubscription?: Subscription;
  private fadeOutTimeoutId?: ReturnType<typeof setTimeout>;

  constructor(
    private badgeNotificationService: BadgeNotificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.badgeSubscription =
      this.badgeNotificationService.badgeUnlocked$.subscribe(
        (badge: BadgeUnlocked) => {
          this.visibleBadge = badge;
          this.cdr.detectChanges();

          if (this.fadeOutTimeoutId) {
            clearTimeout(this.fadeOutTimeoutId);
          }

          this.fadeOutTimeoutId = setTimeout(() => {
            this.visibleBadge = null;
            this.cdr.detectChanges();
          }, 4500); // Show for 4.5 seconds
        },
      );
  }

  dismissBadge() {
    this.visibleBadge = null;
    this.cdr.detectChanges();
    if (this.fadeOutTimeoutId) {
      clearTimeout(this.fadeOutTimeoutId);
      this.fadeOutTimeoutId = undefined;
    }
  }

  ngOnDestroy() {
    this.badgeSubscription?.unsubscribe();
    if (this.fadeOutTimeoutId) {
      clearTimeout(this.fadeOutTimeoutId);
    }
  }
}
