import { Injectable } from '@angular/core';

export type ThemeMode = 'light' | 'dark';
const STORAGE_KEY = 'themeMode';

@Injectable({ providedIn: 'root' })
export class DarkModeService {
  private current: ThemeMode = 'light';

  constructor() {
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.applyMode(saved ?? (prefersDark ? 'dark' : 'light'));

    // Volg OS voorkeur als er geen opgeslagen keuze is
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
      if (!localStorage.getItem(STORAGE_KEY)) {
        this.applyMode(e.matches ? 'dark' : 'light');
      }
    });
  }

  getMode(): ThemeMode { return this.current; }
  isDark(): boolean { return this.current === 'dark'; }

  toggle(): void {
    this.setMode(this.current === 'dark' ? 'light' : 'dark');
  }

  setMode(mode: ThemeMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);
  }

  private applyMode(mode: ThemeMode): void {
    this.current = mode;
    document.documentElement.classList.toggle('dark', mode === 'dark');
  }
}