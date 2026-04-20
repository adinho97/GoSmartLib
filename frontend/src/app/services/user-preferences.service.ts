import { Injectable } from "@angular/core";
import axios from "axios";
import { BehaviorSubject, Observable } from "rxjs";

/**
 * Strongly typed preference keys to prevent typos at compile time
 */
export type PreferenceKey =
  | "recommendationExcludeRead_trending"
  | "recommendationExcludeRead_genre"
  | "recommendationExcludeRead_author"
  | "recommendationExcludeRead_newArrivals"
  | "dashboard_showWishlist"
  | "dashboard_showFavorites"
  | "dashboard_showReadingHistory"
  | "dashboard_showBorrowed"
  | "dashboard_showHighlighted"
  | "dashboard_showDeadline";

@Injectable({
  providedIn: "root",
})
export class UserPreferencesService {
  private apiUrl = "/api/user/preferences";
  private readonly STORAGE_KEY = "userPreferences";

  // Reactive state - components subscribe to this observable
  private preferencesSubject = new BehaviorSubject<Record<string, boolean>>({});
  public preferences$: Observable<Record<string, boolean>> =
    this.preferencesSubject.asObservable();

  /**
   * Initialize preferences from localStorage (synchronous, no flicker)
   * Then sync with backend in background without blocking UI
   * Note: AppComponent can call this without awaiting — localStorage seed is instant
   * Can be called multiple times safely - will re-sync if needed
   */
  async init(): Promise<void> {
    // Load from localStorage immediately (synchronous - no flicker)
    const cached = this.getFromLocalStorage();
    this.preferencesSubject.next(cached);

    // Only sync with backend if user is authenticated
    if (this.isUserAuthenticated()) {
      this.syncWithBackendInBackground();
    }
  }

  async loadPreferencesFromBackend(): Promise<void> {
    // Skip if user is not authenticated to avoid 401 errors
    if (!this.isUserAuthenticated()) {
      return;
    }

    try {
      const backendPrefs = await this.fetchFromBackend();
      // Update local state with backend data; backend always wins on conflicts
      const merged = { ...this.preferencesSubject.value, ...backendPrefs };
      this.preferencesSubject.next(merged);
      this.saveToLocalStorage(merged);
    } catch (error: any) {
      // Handle 401 errors by clearing stale auth data and returning silently
      if (error.response?.status === 401 || error.status === 401) {
        console.debug(
          "Authentication failed when loading preferences, clearing stale auth data",
        );
        this.clearStaleAuthData();
        return;
      }

      console.warn("Failed to load preferences from backend:", error);
      // Keep using cached localStorage values if backend is unavailable
    }
  }

  /**
   * Sync with backend without awaiting - runs in background
   * Merges backend response with cached values (backend wins on conflicts, cache fills gaps)
   */
  private syncWithBackendInBackground(): void {
    this.loadPreferencesFromBackend();
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
      await axios.patch(this.apiUrl, { key, value }, this.getUserHeaders());
    } catch (error) {
      // Backend failed — rollback to prevent silent data loss on next sync
      console.warn(
        `Failed to sync preference ${key}, rolling back to previous state:`,
        error,
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
      console.warn("Failed to parse cached preferences:", error);
      return {};
    }
  }

  private saveToLocalStorage(prefs: Record<string, boolean>): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(prefs));
    } catch (error) {
      console.warn("Failed to save preferences to localStorage:", error);
    }
  }

  private async fetchFromBackend(): Promise<Record<string, boolean>> {
    const res = await axios.get<Record<string, boolean>>(
      this.apiUrl,
      this.getUserHeaders(),
    );
    return res.data || {};
  }

  private getUserHeaders() {
    const userSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    const token = localStorage.getItem("smartschoolToken") || "";
    return {
      headers: {
        "X-User-Sub": userSub,
        "Cache-Control": "no-cache",
        Pragma: "no-cache",
        Authorization: token ? `Bearer ${token}` : "",
      },
    };
  }

  /**
   * Check if user has the required authentication data
   * Returns true only if both user identifier and token are present
   */
  private isUserAuthenticated(): boolean {
    const userSub =
      localStorage.getItem("sub") || localStorage.getItem("userId");
    const token = localStorage.getItem("smartschoolToken");
    return !!(userSub && token);
  }

  /**
   * Clear stale authentication data from localStorage
   * Called when a 401 error indicates the auth tokens are invalid
   */
  private clearStaleAuthData(): void {
    localStorage.removeItem("sub");
    localStorage.removeItem("userId");
    localStorage.removeItem("smartschoolToken");
    localStorage.removeItem("role");
    localStorage.removeItem("userName");
    localStorage.removeItem("smartschoolPlatform");
  }
}
