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
   * Fetches available classes for the currently selected school or the user's own school.
   * Falls back to an empty array when no school is selected.
   */
  getClasses(schoolId?: number): Observable<string[]> {
    const effectiveSchoolId =
      schoolId ?? this.getSelectedSchoolId() ?? this.getUserOwnSchoolId();

    return new Observable((subscriber) => {
      if (!effectiveSchoolId) {
        subscriber.next([]);
        subscriber.complete();
        return;
      }

      void this.getKlassenBySchool(effectiveSchoolId)
        .then((klassen) => {
          subscriber.next(klassen.map((k) => k.naam));
          subscriber.complete();
        })
        .catch((err) => subscriber.error(err));
    });
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
      const res = await fetch(`/api/gebruikers/me/school`);
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
      const res = await fetch(`/api/admin/schools`);
      if (!res.ok) return [];
      return await res.json();
    } catch (err) {
      console.error("getSchools failed:", err);
      return [];
    }
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
