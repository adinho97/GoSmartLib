import { Injectable } from '@angular/core';
import axios from 'axios';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Strongly typed preference keys to prevent typos at compile time
 */
export type PreferenceKey =
  | 'recommendationExcludeRead_trending'
  | 'recommendationExcludeRead_genre'
  | 'recommendationExcludeRead_author'
  | 'recommendationExcludeRead_newArrivals';

@Injectable({
  providedIn: 'root',
})
export class UserPreferencesService {
  private apiUrl = '/api/user/preferences';
  private readonly STORAGE_KEY = 'userPreferences';
  private initialized = false;

  // Reactive state - components subscribe to this observable
  private preferencesSubject = new BehaviorSubject<Record<string, boolean>>({});
  public preferences$: Observable<Record<string, boolean>> = this.preferencesSubject.asObservable();

  /**
   * Initialize preferences from localStorage (synchronous, no flicker)
   * Then sync with backend in background without blocking UI
   * Note: AppComponent can call this without awaiting — localStorage seed is instant
   * Idempotent: safe to call multiple times, only initializes once
   */
  async init(): Promise<void> {
    if (this.initialized) return;
    this.initialized = true;

    // Load from localStorage immediately (synchronous - no flicker)
    const cached = this.getFromLocalStorage();
    this.preferencesSubject.next(cached);

    // Then sync with backend silently in the background (fire and forget)
    this.syncWithBackendInBackground();
  }

  /**
   * Sync with backend without awaiting - runs in background
   * Merges backend response with cached values (backend wins on conflicts, cache fills gaps)
   */
  private syncWithBackendInBackground(): void {
    this.fetchFromBackend()
      .then((backendPrefs) => {
        // Update local state if backend has data
        if (Object.keys(backendPrefs).length > 0) {
          // Merge backend prefs with cache (don't lose cached keys missing in backend response)
          const merged = { ...this.preferencesSubject.value, ...backendPrefs };
          this.preferencesSubject.next(merged);
          this.saveToLocalStorage(merged);
        }
      })
      .catch((error) => {
        console.warn('Failed to sync preferences with backend:', error);
        // Silently fail - keep using cached localStorage value
      });
  }

  /**
   * Save a preference: update immediately (optimistic), sync to backend
   * Rollback on backend failure to prevent silent data loss
   */
  async savePreference(key: PreferenceKey, value: boolean): Promise<void> {
    // Snapshot state before optimistic update
    const previous = { ...this.preferencesSubject.value };
    const updated = { ...previous, [key]: value };

    // Update local state immediately (optimistic update - no flicker)
    this.preferencesSubject.next(updated);
    this.saveToLocalStorage(updated);

    // Sync to backend
    try {
      await axios.patch(
        this.apiUrl,
        { key, value },
        this.getUserHeaders()
      );
    } catch (error) {
      // Backend failed — rollback to prevent silent data loss on next sync
      console.warn(
        `Failed to sync preference ${key}, rolling back to previous state:`,
        error
      );
      this.preferencesSubject.next(previous);
      this.saveToLocalStorage(previous);
    }
  }

  clearCache(): void {
    this.preferencesSubject.next({});
    localStorage.removeItem(this.STORAGE_KEY);
  }

  /**
   * @deprecated Prefer preferences$ observable. Only use for non-reactive legacy contexts.
   */
  getSnapshotForLegacyUse(): Record<string, boolean> {
    return this.preferencesSubject.value;
  }


  private getFromLocalStorage(): Record<string, boolean> {
    try {
      const cached = localStorage.getItem(this.STORAGE_KEY);
      return cached ? JSON.parse(cached) : {};
    } catch (error) {
      console.warn('Failed to parse cached preferences:', error);
      return {};
    }
  }

  private saveToLocalStorage(prefs: Record<string, boolean>): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(prefs));
    } catch (error) {
      console.warn('Failed to save preferences to localStorage:', error);
    }
  }

  private async fetchFromBackend(): Promise<Record<string, boolean>> {
    const res = await axios.get<Record<string, boolean>>(
      this.apiUrl,
      this.getUserHeaders()
    );
    return res.data || {};
  }

  private getUserHeaders() {
    const userSub =
      localStorage.getItem('sub') || localStorage.getItem('userId') || '';
    return {
      headers: {
        'X-User-Sub': userSub,
      },
    };
  }
}
