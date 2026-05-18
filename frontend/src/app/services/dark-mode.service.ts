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
      // Determine mode:
      // 1. DB value wins if explicitly set (true/false).
      // 2. If DB value is undefined (user never set it), fall back to OS preference.
      // 3. If OS preference is not available, use the current modeSubject value (which would be OS default from initial load).
      const dbValue = prefs["ui_darkMode"];
      const targetMode: ThemeMode =
        dbValue !== undefined
          ? dbValue
            ? "dark"
            : "light"
          : window.matchMedia("(prefers-color-scheme: dark)").matches
            ? "dark"
            : "light";

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
