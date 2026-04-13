import { Injectable } from "@angular/core";
import { Subject } from "rxjs";

export interface BadgeUnlocked {
  title: string;
  icon: string;
  category: "loan" | "review";
}

@Injectable({
  providedIn: "root",
})
export class BadgeNotificationService {
  badgeUnlocked$ = new Subject<BadgeUnlocked>();

  showBadgeNotification(badge: BadgeUnlocked) {
    this.badgeUnlocked$.next(badge);
  }
}
