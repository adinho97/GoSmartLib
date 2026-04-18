import { Injectable } from "@angular/core";
import { Subject } from "rxjs";

export type UiToastKind = "success" | "error" | "info";

export type UiToastMessage = {
  text: string;
  kind?: UiToastKind;
  durationMs?: number;
};

@Injectable({ providedIn: "root" })
export class UiToastService {
  private readonly toastSubject = new Subject<UiToastMessage>();
  readonly toasts$ = this.toastSubject.asObservable();

  show(message: UiToastMessage): void {
    this.toastSubject.next(message);
  }

  success(text: string, durationMs = 2800): void {
    this.show({ text, kind: "success", durationMs });
  }

  error(text: string, durationMs = 3200): void {
    this.show({ text, kind: "error", durationMs });
  }

  info(text: string, durationMs = 2600): void {
    this.show({ text, kind: "info", durationMs });
  }
}
