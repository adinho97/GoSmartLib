import { Injectable } from '@angular/core';
import { UserPreferencesService } from './user-preferences.service';

export type ColorblindMode = 'none' | 'deuteranopia'; // Je kunt dit uitbreiden
const STORAGE_KEY = 'colorblindMode';

@Injectable({ providedIn: 'root' })
export class ColorblindService {
  private current: ColorblindMode = 'none';

  constructor(private prefsService: UserPreferencesService) {
    // Init uit localStorage
    const saved = (localStorage.getItem(STORAGE_KEY) as ColorblindMode) || 'none';
    this.applyMode(saved);

    // Sync met Database
    this.prefsService.preferences$.subscribe(prefs => {
      if (prefs['ui_colorblind'] !== undefined) {
        const dbMode = prefs['ui_colorblind'] ? 'deuteranopia' : 'none';
        if (dbMode !== this.current) {
          this.applyMode(dbMode);
        }
      }
    });
  }

  getMode(): ColorblindMode { return this.current; }

  toggle(): void {
    const newMode = this.current === 'none' ? 'deuteranopia' : 'none';
    this.setMode(newMode);
  }

  private setMode(mode: ColorblindMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);
    this.prefsService.savePreference("ui_colorblind", mode !== 'none');
  }

  private applyMode(mode: ColorblindMode): void {
    this.current = mode;
    const html = document.documentElement;
    html.classList.remove('cb-deuteranopia', 'cb-protanopia', 'cb-tritanopia');
    if (mode !== 'none') {
      html.classList.add(`cb-${mode}`);
    }
  }
}