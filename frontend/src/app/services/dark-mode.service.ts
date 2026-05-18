import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { UserPreferencesService } from "./user-preferences.service";

export type ThemeMode = "light" | "dark";
const STORAGE_KEY = "themeMode";

@Injectable({ providedIn: "root" })
export class DarkModeService {
  private modeSubject = new BehaviorSubject<ThemeMode>("light");
  mode$ = this.modeSubject.asObservable();

  constructor(private prefsService: UserPreferencesService) {
    // 1. Snel laden uit localStorage voor de eerste seconde
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
    if (saved) {
      this.applyMode(saved);
    } else {
      // Fallback naar OS als er niks in storage staat
      const prefersDark = window.matchMedia(
        "(prefers-color-scheme: dark)",
      ).matches;
      this.applyMode(prefersDark ? "dark" : "light");
    }

    // 2. ABONNEER op database updates (Dit regelt de switch na login!)
    this.prefsService.preferences$.subscribe((prefs) => {
      const dbValue = prefs["ui_darkMode"];

      // If the key is missing from the stream, it means the sync hasn't provided
      // a value yet or the user has no preference. DO NOT fall back to OS
      // here as it will overwrite our valid localStorage state.
      if (dbValue === undefined) return;

      const targetMode: ThemeMode = dbValue ? "dark" : "light";

      if (targetMode !== this.modeSubject.value) {
        this.applyMode(targetMode);
      }
    });
  }

  isDark(): boolean {
    return this.modeSubject.value === "dark";
  }

  toggle(): void {
    const newMode = this.isDark() ? "light" : "dark";
    this.setMode(newMode);
  }

  private setMode(mode: ThemeMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);
    // Sla op in DB
    this.prefsService.savePreference("ui_darkMode", mode === "dark");
  }

  private applyMode(mode: ThemeMode): void {
    this.modeSubject.next(mode);
    document.documentElement.classList.toggle("dark", mode === "dark");
  }
}
