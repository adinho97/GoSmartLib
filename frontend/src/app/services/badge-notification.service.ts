import { Injectable } from "@angular/core";
import { Subject } from "rxjs";

export type BadgeIconKey =
  | "boek"
  | "rij"
  | "nacht"
  | "klas"
  | "bib"
  | "leg"
  | "rev"
  | "recens"
  | "crit"
  | "schr"
  | "lit"
  | "master";

export interface BadgeUnlocked {
  title: string;
  iconKey: BadgeIconKey;
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
