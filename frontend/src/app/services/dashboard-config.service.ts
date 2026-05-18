import { Injectable } from "@angular/core";
import axios from "axios";
import { BehaviorSubject, Observable } from "rxjs";

export interface DashboardConfig {
  tiles: string[];
  pages: Record<string, string[]>;
  shortcuts?: string[];
}

export const DEFAULT_DASHBOARD_CONFIG: DashboardConfig = {
  tiles: ["mijn-boeken", "bibliotheek", "snelkoppelingen"],
  pages: {
    "mijn-boeken": ["verder-lezen", "laatst-ingeleverd"],
    bibliotheek: ["in-de-kijker", "boek-vd-maand", "themaboek"],
  },
};

@Injectable({
  providedIn: "root",
})
export class DashboardConfigService {
  private readonly apiUrl = "/api/user/dashboard-config";
  private readonly STORAGE_KEY = "gosmartlib.dashboard.config.v2";

  private hasSynced = false;
  private isSyncing = false;

  // Reactive config stream. The initial value is loaded from localStorage in init().
  private configSubject = new BehaviorSubject<DashboardConfig>(
    this.cloneDefault(),
  );

  public config$: Observable<DashboardConfig> =
    this.configSubject.asObservable();

  async init(): Promise<void> {
    const cached = this.readLocal();
    this.configSubject.next(cached ?? this.cloneDefault());

    // Proactive sync if already logged in
    const role = localStorage.getItem("role");
    if (this.isAuthenticated() && role === "leerling") {
      this.syncFromBackendInBackground();
    }
  }

  getSnapshot(): DashboardConfig {
    return this.configSubject.value;
  }

  /**
   * Persist a full DashboardConfig: update immediately (optimistic),
   * write through to backend, roll back on failure.
   */
  async save(next: DashboardConfig): Promise<void> {
    const previous = this.configSubject.value;
    this.configSubject.next(next);
    this.writeLocal(next);

    if (!this.isAuthenticated()) return;

    try {
      await axios.put(
        this.apiUrl,
        { configJson: JSON.stringify(next) },
        this.authHeaders(),
      );
    } catch (error) {
      console.warn("Failed to sync dashboard config, rolling back:", error);
      this.configSubject.next(previous);
      this.writeLocal(previous);
    }
  }

  clearCache(): void {
    this.configSubject.next(this.cloneDefault());
    try {
      localStorage.removeItem(this.STORAGE_KEY);
      this.hasSynced = false;
    } catch {}
  }

  /**
   * Forces a refresh of the dashboard configuration from the backend.
   * Useful after login or when authentication state changes.
   */
  async syncNow(): Promise<void> {
    this.hasSynced = false; // Ensure a fresh sync
    await this.loadFromBackend();
  }

  private syncFromBackendInBackground(): void {
    this.loadFromBackend();
  }

  private async loadFromBackend(): Promise<void> {
    if (this.isSyncing) return;

    const role = localStorage.getItem("role");
    if (role !== "leerling") return; // Only students have dashboard configs

    this.isSyncing = true;
    try {
      const res = await axios.get<{ configJson: string | null }>(
        this.apiUrl,
        this.authHeaders(),
      );
      const json = res.data?.configJson;
      if (!json) return;

      const parsed = this.parseConfig(json);
      if (parsed) {
        this.configSubject.next(parsed);
        this.writeLocal(parsed);
        this.hasSynced = true;
      }
    } catch (error: any) {
      // Silence 401/403 errors for unauthorized roles
      if (error?.response?.status === 401 || error?.response?.status === 403) {
        return;
      }
      if (error?.response?.status === 401) {
        console.debug("Unauthorized when loading dashboard config");
        return;
      }
      console.warn("Failed to load dashboard config from backend:", error);
    } finally {
      this.isSyncing = false;
    }
  }

  private readLocal(): DashboardConfig | null {
    try {
      const raw = localStorage.getItem(this.STORAGE_KEY);
      if (!raw) return null;
      return this.parseConfig(raw);
    } catch {
      return null;
    }
  }

  private writeLocal(cfg: DashboardConfig): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(cfg));
    } catch {}
  }

  private parseConfig(raw: string): DashboardConfig | null {
    try {
      const obj = JSON.parse(raw);
      if (
        obj &&
        Array.isArray(obj.tiles) &&
        obj.pages &&
        typeof obj.pages === "object"
      ) {
        return obj as DashboardConfig;
      }
    } catch {}
    return null;
  }

  private cloneDefault(): DashboardConfig {
    return {
      tiles: [...DEFAULT_DASHBOARD_CONFIG.tiles],
      pages: Object.fromEntries(
        Object.entries(DEFAULT_DASHBOARD_CONFIG.pages).map(([k, v]) => [
          k,
          [...v],
        ]),
      ),
    };
  }

  private isAuthenticated(): boolean {
    const sub = localStorage.getItem("sub") || localStorage.getItem("userId");
    const token = localStorage.getItem("smartschoolToken");
    return !!(sub && token);
  }

  private authHeaders() {
    const sub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    const token = localStorage.getItem("smartschoolToken") || "";
    return {
      headers: {
        "X-User-Sub": sub,
        "Cache-Control": "no-cache",
        Pragma: "no-cache",
        Authorization: token ? `Bearer ${token}` : "",
      },
    };
  }
}
