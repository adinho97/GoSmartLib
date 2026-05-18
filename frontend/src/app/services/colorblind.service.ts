import { Injectable } from "@angular/core";
import { UserPreferencesService } from "./user-preferences.service";

export type ColorblindMode = "none" | "deuteranopia";
const STORAGE_KEY = "colorblindMode";

@Injectable({
  providedIn: "root",
})
export class ColorblindService {
  private currentMode: ColorblindMode = "none";

  constructor(private prefsService: UserPreferencesService) {
    // 1. Snelle initialisatie vanuit localStorage
    const saved = localStorage.getItem(STORAGE_KEY) as ColorblindMode;
    if (saved) {
      this.applyMode(saved);
    }

    // 2. Luister naar updates vanuit de database sync
    this.prefsService.preferences$.subscribe((prefs) => {
      const dbValue = prefs["ui_colorblind"];

      // Only react if the sync actually returned a value for this key
      if (dbValue === undefined) return;

      const targetMode: ColorblindMode = dbValue ? "deuteranopia" : "none";

      if (targetMode !== this.currentMode) {
        this.applyMode(targetMode);
      }
    });
  }

  getMode(): ColorblindMode {
    return this.currentMode;
  }

  toggle(): void {
    const newMode: ColorblindMode =
      this.currentMode === "none" ? "deuteranopia" : "none";
    this.setMode(newMode);
  }

  private setMode(mode: ColorblindMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);

    // Opslaan in database via de centrale service
    this.prefsService.savePreference("ui_colorblind", mode !== "none");
  }

  private applyMode(mode: ColorblindMode): void {
    this.currentMode = mode;
    const htmlElement = document.documentElement;

    // Verwijder oude klassen en voeg nieuwe toe indien nodig
    htmlElement.classList.remove("cb-deuteranopia");
    if (mode !== "none") {
      htmlElement.classList.add(`cb-${mode}`);
    }
  }
}
