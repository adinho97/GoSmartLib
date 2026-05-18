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
  private readonly UI_COOKIE_KEY = "ui_prefs_v1";

  private hasSyncedWithBackend = false;
  private activeSyncPromise: Promise<void> | null = null;

  // Reactive state - components subscribe to this observable.
  private preferencesSubject: BehaviorSubject<Record<string, boolean>>;

  public preferences$: Observable<Record<string, boolean>> = null as any; // Initialized in constructor

  constructor() {
    const cached = this.getFromLocalStorage();
    const cookiePrefs = this.getUiPrefsFromCookie();
    // Cookie values are a user-visible override when local cache is empty or missing
    const mergedInitial = { ...cached, ...cookiePrefs };
    this.preferencesSubject = new BehaviorSubject<Record<string, boolean>>(
      mergedInitial,
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

  public isSynced(): boolean {
    return this.hasSyncedWithBackend;
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
      // Persist UI-related prefs to cookie so they survive logout/localStorage.clear()
      if (key === "ui_darkMode" || key === "ui_colorblind") {
        this.saveUiPrefsToCookie(this.preferencesSubject.value);
      }
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

  // --- Cookie helpers for UI preferences (persist across logout) ---
  private saveUiPrefsToCookie(allPrefs: Record<string, boolean>): void {
    try {
      const uiSubset: Record<string, boolean> = {};
      if (allPrefs["ui_darkMode"] !== undefined)
        uiSubset["ui_darkMode"] = !!allPrefs["ui_darkMode"];
      if (allPrefs["ui_colorblind"] !== undefined)
        uiSubset["ui_colorblind"] = !!allPrefs["ui_colorblind"];
      const json = JSON.stringify(uiSubset);
      const expires = new Date();
      expires.setFullYear(expires.getFullYear() + 1);
      document.cookie = `${this.UI_COOKIE_KEY}=${encodeURIComponent(
        json,
      )}; path=/; expires=${expires.toUTCString()}; SameSite=Lax`;
    } catch (err) {
      // non-fatal
    }
  }

  private getUiPrefsFromCookie(): Record<string, boolean> {
    try {
      const nameEQ = this.UI_COOKIE_KEY + "=";
      const ca = document.cookie.split(";");
      for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === " ") c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) {
          const raw = decodeURIComponent(c.substring(nameEQ.length));
          const parsed = JSON.parse(raw || "{}");
          const out: Record<string, boolean> = {};
          if (parsed["ui_darkMode"] !== undefined)
            out["ui_darkMode"] = !!parsed["ui_darkMode"];
          if (parsed["ui_colorblind"] !== undefined)
            out["ui_colorblind"] = !!parsed["ui_colorblind"];
          return out;
        }
      }
    } catch (err) {
      // ignore
    }
    return {};
  }

  public clearUiPrefsCookie(): void {
    try {
      document.cookie = `${this.UI_COOKIE_KEY}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT;`;
    } catch {
      // ignore
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
