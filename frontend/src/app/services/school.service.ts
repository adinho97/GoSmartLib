import { Injectable } from "@angular/core";
import { Observable, of } from "rxjs";
import { delay } from "rxjs/operators";
import { AuthContextService } from "./auth-context.service";
import axios from "axios";

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
  constructor(private authContext: AuthContextService) {}

  /**
   * Fetches available classes for the currently selected school or the user's own school.
   * Falls back to an empty array when no school is selected.
   */
  getClasses(schoolId?: number): Observable<string[]> {
    return new Observable((subscriber) => {
      void this.resolveSchoolId(schoolId)
        .then((effectiveSchoolId) => {
          if (!effectiveSchoolId) {
            subscriber.next([]);
            subscriber.complete();
            return;
          }

          return this.getKlassenBySchool(effectiveSchoolId);
        })
        .then((klassen) => {
          if (!klassen) {
            subscriber.next([]);
            subscriber.complete();
            return;
          }
          subscriber.next(klassen.map((k) => k.naam));
          subscriber.complete();
        })
        .catch((err) => subscriber.error(err));
    });
  }

  private async resolveSchoolId(schoolId?: number): Promise<number | null> {
    if (schoolId) return schoolId;
    const selectedSchoolId = this.getSelectedSchoolId();
    if (selectedSchoolId) return selectedSchoolId;
    const ownSchoolId = this.getUserOwnSchoolId();
    if (ownSchoolId) return ownSchoolId;

    await this.selectUserDefaultSchool();
    return this.getSelectedSchoolId() ?? this.getUserOwnSchoolId();
  }

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
        `/api/scholen/${encodeURIComponent(schoolId)}/klassen`,
        {
          headers: this.buildAuthHeaders(),
        },
      );
      if (!res.ok) return [];
      return (await res.json()) as KlasListItem[];
    } catch (err) {
      console.error("getKlassenBySchool failed:", err);
      return [];
    }
  }

  async getDefaultLoanDays(schoolId: number): Promise<number> {
    try {
      const token = this.authContext.getEffectiveBearerToken();
      const res = await axios.get(
        `/api/scholen/${schoolId}/default-loan-days`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "X-User-Role": this.authContext.getEffectiveRole(),
            "X-User-Sub": this.authContext.getEffectiveSub(),
          },
        }
      );
      return res.data?.defaultLoanDays ?? 14;
    } catch {
      return 14;
    }
  }

  async updateDefaultLoanDays(schoolId: number, days: number): Promise<void> {
    const token = this.authContext.getEffectiveBearerToken();
    await axios.patch(
      `/api/scholen/${schoolId}/default-loan-days`,
      { defaultLoanDays: days },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          "X-User-Role": this.authContext.getEffectiveRole(),
          "X-User-Sub": this.authContext.getEffectiveSub(),
        },
      }
    );
  }
}