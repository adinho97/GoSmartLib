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
    // 1. Snelle initialisatie vanuit de centrale preferences cache
    const initialPref =
      this.prefsService.getSnapshotForLegacyUse()["ui_colorblind"];
    if (initialPref !== undefined) {
      this.applyMode(initialPref ? "deuteranopia" : "none");
    }

    // 2. Luister naar updates vanuit de database sync
    this.prefsService.preferences$.subscribe((prefs) => {
      const dbValue = prefs["ui_colorblind"];

      // Wait for sync to complete before falling back to default
      if (dbValue === undefined && !this.prefsService.isSynced()) return;

      // Default to 'none' if we synced but no preference was found
      const targetMode: ColorblindMode =
        dbValue === true ? "deuteranopia" : "none";

      if (targetMode !== this.currentMode) {
        this.applyMode(targetMode);
      }
    });
  }

  getMode(): ColorblindMode {
    return this.currentMode;
  }

  async toggle(): Promise<void> {
    const newMode: ColorblindMode =
      this.currentMode === "none" ? "deuteranopia" : "none";
    await this.setMode(newMode);
  }

  private async setMode(mode: ColorblindMode): Promise<void> {
    this.applyMode(mode);
    await this.prefsService.savePreference("ui_colorblind", mode !== "none");
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
