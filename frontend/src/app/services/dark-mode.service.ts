import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

export type ThemeMode = "light" | "dark";
const STORAGE_KEY = "themeMode";

@Injectable({ providedIn: "root" })
export class DarkModeService {
  // Gebruik een BehaviorSubject zodat de component de status altijd live ziet
  private modeSubject = new BehaviorSubject<ThemeMode>("light");
  mode$ = this.modeSubject.asObservable();

  constructor() {
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
    const prefersDark = window.matchMedia(
      "(prefers-color-scheme: dark)",
    ).matches;

    // Bepaal de initiële modus
    const initialMode = saved ?? (prefersDark ? "dark" : "light");
    this.applyMode(initialMode);

    // Alleen luisteren naar OS als de gebruiker nog NOOIT zelf gekozen heeft
    window
      .matchMedia("(prefers-color-scheme: dark)")
      .addEventListener("change", (e) => {
        if (!localStorage.getItem(STORAGE_KEY)) {
          this.applyMode(e.matches ? "dark" : "light");
        }
      });
  }

  isDark(): boolean {
    return this.modeSubject.value === "dark";
  }

  toggle(): void {
    const newMode = this.modeSubject.value === "dark" ? "light" : "dark";
    this.setMode(newMode);
  }

  setMode(mode: ThemeMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);
  }

  private applyMode(mode: ThemeMode): void {
    this.modeSubject.next(mode);
    document.documentElement.classList.toggle("dark", mode === "dark");
  }
}
