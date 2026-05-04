import { Injectable } from '@angular/core';

export type ColorblindMode = 'none' | 'deuteranopia' | 'protanopia' | 'tritanopia';

const STORAGE_KEY = 'colorblindMode';

@Injectable({ providedIn: 'root' })
export class ColorblindService {

  private current: ColorblindMode = 'none';

  constructor() {
    this.applyMode((localStorage.getItem(STORAGE_KEY) as ColorblindMode) || 'none');
  }

  getMode(): ColorblindMode {
    return this.current;
  }

  setMode(mode: ColorblindMode): void {
    localStorage.setItem(STORAGE_KEY, mode);
    this.applyMode(mode);
  }

  toggle(): void {
    this.setMode(this.current === 'none' ? 'deuteranopia' : 'none');
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