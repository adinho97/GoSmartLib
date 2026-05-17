import { Injectable } from "@angular/core";
import { Observable, of } from "rxjs";
import { delay } from "rxjs/operators";

export interface KlasListItem {
  id: number;
  naam: string;
}

export interface PagedResult<T> {
  items: T[];
  total: number;
}

@Injectable({
  providedIn: "root",
})
export class SchoolService {
  constructor() {}

  /**
   * Simulates fetching available classes from a backend database.
   * In a real application, this would make an HTTP request.
   */
  getClasses(): Observable<string[]> {
    return of(["Klas A", "Klas B", "Klas C", "Klas D", "Klas E"]).pipe(
      delay(500),
    ); // Simulate network delay
  }

  /**
   * Returns all schools as an Observable (used by map/admin components).
   */
  getAllSchools(): Observable<any[]> {
    // Use the async getSchools() under the hood and convert to an Observable via of()
    return new Observable((subscriber) => {
      void this.getSchools()
        .then((s) => {
          subscriber.next(s);
          subscriber.complete();
        })
        .catch((err) => subscriber.error(err));
    });
  }

  /**
   * Returns a paged list of schools. If backend paging isn't available, performs client-side paging.
   */
  getPagedSchools(
    page: number,
    pageSize: number,
    search: string,
  ): Observable<PagedResult<any>> {
    return new Observable((subscriber) => {
      void this.getSchools()
        .then((all) => {
          const filtered = (all || []).filter((s: any) => {
            if (!search || search.trim() === "") return true;
            return (
              (s.naam || "")
                .toString()
                .toLowerCase()
                .includes(search.toLowerCase()) ||
              (s.adres || "")
                .toString()
                .toLowerCase()
                .includes(search.toLowerCase())
            );
          });
          const total = filtered.length;
          const start = Math.max(0, page * pageSize);
          const items = filtered.slice(start, start + pageSize);
          subscriber.next({ items, total });
          subscriber.complete();
        })
        .catch((err) => subscriber.error(err));
    });
  }

  // Selected school helpers (persisted in localStorage)
  getSelectedSchoolId(): number | null {
    const v = localStorage.getItem("selectedSchoolId");
    return v ? Number(v) : null;
  }

  setSelectedSchoolId(id: number | null): void {
    if (id === null || id === undefined) {
      localStorage.removeItem("selectedSchoolId");
    } else {
      localStorage.setItem("selectedSchoolId", String(id));
    }
  }

  getUserOwnSchoolId(): number | null {
    const v = localStorage.getItem("userOwnSchoolId");
    return v ? Number(v) : null;
  }

  /**
   * Ensures the current user's school is known and stored in localStorage.
   * If no `selectedSchoolId` exists, it will default to the user's own school.
   */
  async selectUserDefaultSchool(): Promise<void> {
    try {
      const res = await fetch(`/api/gebruikers/me/school`, {
        headers: this.buildAuthHeaders(),
      });
      if (!res.ok) return;
      const payload = await res.json();
      const schoolId = payload?.schoolId ?? null;
      if (schoolId !== null) {
        localStorage.setItem("userOwnSchoolId", String(schoolId));
        if (!this.getSelectedSchoolId()) {
          this.setSelectedSchoolId(schoolId);
        }
      }
    } catch (err) {
      // ignore failures; callers handle absence
      console.error("selectUserDefaultSchool failed:", err);
    }
  }

  async getSchools(): Promise<any[]> {
    try {
      const res = await fetch(`/api/admin/schools`, {
        headers: this.buildAuthHeaders(),
      });
      if (!res.ok) return [];
      return await res.json();
    } catch (err) {
      console.error("getSchools failed:", err);
      return [];
    }
  }

  // The auth interceptor only attaches the bearer token to HttpClient requests;
  // raw fetch() bypasses it, so we replicate the same role-based token lookup here.
  private buildAuthHeaders(): Record<string, string> {
    const role = localStorage.getItem("role");
    const token =
      role === "SUPER_ADMIN" || role === "super_admin"
        ? localStorage.getItem("admin_jwt_token")
        : localStorage.getItem("smartschoolToken");
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  /**
   * Returns klas list items for a given school id. Falls back to an empty array on failure.
   */
  async getKlassenBySchool(schoolId: number): Promise<KlasListItem[]> {
    try {
      const res = await fetch(
        `/api/schools/${encodeURIComponent(schoolId)}/klassen`,
      );
      if (!res.ok) return [];
      return (await res.json()) as KlasListItem[];
    } catch (err) {
      console.error("getKlassenBySchool failed:", err);
      return [];
    }
  }
}
