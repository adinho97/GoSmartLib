import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { UserPreferencesService } from './user-preferences.service';

export type ThemeMode = 'light' | 'dark';
const STORAGE_KEY = 'themeMode';

@Injectable({ providedIn: 'root' })
export class DarkModeService {
  private modeSubject = new BehaviorSubject<ThemeMode>('light');
  mode$ = this.modeSubject.asObservable();

  constructor(private prefsService: UserPreferencesService) {
    // 1. Snelle start: Kijk in localStorage (voorkomt flits van wit scherm)
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
    const initialMode = saved || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    this.applyMode(initialMode);

    // 2. Sync met Database: Luister naar updates van de UserPreferencesService
    this.prefsService.preferences$.subscribe(prefs => {
      if (prefs['ui_darkMode'] !== undefined) {
        const dbMode = prefs['ui_darkMode'] ? 'dark' : 'light';
        // Alleen updaten als het verschilt van de huidige state
        if (dbMode !== this.modeSubject.value) {
          this.applyMode(dbMode);
        }
      }
    });
  }

  isDark(): boolean { return this.modeSubject.value === 'dark'; }

  toggle(): void {
    const newMode = this.isDark() ? 'light' : 'dark';
    this.setMode(newMode);
  }

  private setMode(mode: ThemeMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);
    
    // Sla op in de database via de UserPreferencesService
    this.prefsService.savePreference("ui_darkMode", mode === 'dark');
  }

  private applyMode(mode: ThemeMode): void {
    this.modeSubject.next(mode);
    document.documentElement.classList.toggle('dark', mode === 'dark');
  }
}