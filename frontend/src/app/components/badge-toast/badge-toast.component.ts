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
          <div class="badge-toast-icon-large" aria-hidden="true">
            @switch (visibleBadge.iconKey) {
              @case ('boek') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h7a3 3 0 0 1 3 3v14"/><path d="M20 4h-7a3 3 0 0 0-3 3v14"/></svg>
              }
              @case ('rij') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 13h16M4 8h16M4 18h16"/><circle cx="7" cy="8" r="1.3" fill="currentColor"/><circle cx="11" cy="13" r="1.3" fill="currentColor"/><circle cx="15" cy="18" r="1.3" fill="currentColor"/><circle cx="19" cy="13" r="1.3" fill="currentColor"/><circle cx="9" cy="18" r="1.3" fill="currentColor"/></svg>
              }
              @case ('nacht') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13a8 8 0 1 1-9-9 6 6 0 0 0 9 9z"/></svg>
              }
              @case ('klas') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 21h16M5 21V9M9 21V9M15 21V9M19 21V9M3 9h18L12 3z"/></svg>
              }
              @case ('bib') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
              }
              @case ('leg') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.4 6.8 19.1l1-5.8L3.5 9.2l5.9-.9z"/></svg>
              }
              @case ('rev') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20l4-1 9.5-9.5a2.1 2.1 0 0 0-3-3L5 16l-1 4z"/></svg>
              }
              @case ('recens') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="4" width="12" height="17" rx="2"/><path d="M9 9h6M9 13h6M9 17h4"/></svg>
              }
              @case ('crit') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="6"/><path d="m20 20-4.3-4.3"/></svg>
              }
              @case ('schr') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 4l5 5-9 9-5 1 1-5z"/><path d="M3 21h7"/></svg>
              }
              @case ('lit') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z"/><path d="M16 8 2 22"/><path d="M17.5 15H9"/></svg>
              }
              @case ('master') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 19h20l-2-9-5 3-3-7-3 7-5-3z"/><path d="M2 22h20"/></svg>
              }
            }
          </div>
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
