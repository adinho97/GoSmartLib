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
  | "ui_darkMode"
  | "ui_colorblind"
  | "dashboard_showWishlist"
  | "dashboard_showReadingHistory"
  | "dashboard_showBorrowed"
  | "dashboard_showClassReadingList" // Renamed
  | "dashboard_showHighlighted" // New preference
  | "dashboard_showDeadline";

@Injectable({
  providedIn: "root",
})
export class UserPreferencesService {
  private apiUrl = "/api/user/preferences";
  private readonly STORAGE_KEY = "userPreferences";

  private hasSyncedWithBackend = false;
  private activeSyncPromise: Promise<void> | null = null;

  // Reactive state - components subscribe to this observable.
  private preferencesSubject: BehaviorSubject<Record<string, boolean>>;

  public preferences$: Observable<Record<string, boolean>> = null as any; // Initialized in constructor

  constructor() {
    const cached = this.getFromLocalStorage();
    this.preferencesSubject = new BehaviorSubject<Record<string, boolean>>(
      cached,
    );
    this.preferences$ = this.preferencesSubject.asObservable();
  }

  async init(): Promise<void> {
    // Sync with backend if user is already authenticated on app load
    if (this.isUserAuthenticated()) {
      this.syncWithBackendInBackground();
    }
  }

  async loadPreferencesFromBackend(): Promise<void> {
    if (!this.isUserAuthenticated()) {
      return;
    }

    // If a sync is already in progress, return the existing promise to avoid race conditions
    if (this.activeSyncPromise) {
      return this.activeSyncPromise;
    }

    this.activeSyncPromise = (async () => {
      try {
        const backendPrefs = await this.fetchFromBackend();
        console.debug("[UserPreferences] Loaded from backend:", backendPrefs);

        // Update local state with backend data; backend always wins on conflicts
        const merged = { ...this.preferencesSubject.value, ...backendPrefs };
        this.preferencesSubject.next(merged);
        this.saveToLocalStorage(merged);
        this.hasSyncedWithBackend = true;
      } catch (error: any) {
        // Silence 403 Forbidden - some roles don't have preferences enabled/configured
        if (error.response?.status === 403 || error.status === 403) {
          console.debug("Preferences are not enabled for this user role.");
          return;
        }

        // Handle 401 errors by clearing stale auth data and returning silently
        if (error.response?.status === 401 || error.status === 401) {
          this.clearStaleAuthData();
          return;
        }

        console.warn("Failed to load preferences from backend:", error);
      } finally {
        this.activeSyncPromise = null;
      }
    })();

    return this.activeSyncPromise;
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
    console.debug(`[UserPreferences] Attempting to save ${key}=${value}`);

    // Snapshot state before optimistic update
    const previous = { ...this.preferencesSubject.value };
    const updated = { ...previous, [key]: value };

    // Update local state immediately (optimistic update - no flicker)
    this.preferencesSubject.next(updated);
    this.saveToLocalStorage(updated);

    // Sync to backend
    try {
      await axios.patch(this.apiUrl, { key, value }, this.getUserHeaders());
      console.debug(`[UserPreferences] Successfully synced ${key} to backend`);
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
    this.hasSyncedWithBackend = false;
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

  /**
   * Forces a refresh of preferences from the backend.
   * Useful after login or when authentication state changes.
   */
  public async syncNow(): Promise<void> {
    this.hasSyncedWithBackend = false;
    await this.loadPreferencesFromBackend();
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
