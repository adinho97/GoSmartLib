import { Injectable } from "@angular/core";
import { Observable, of } from "rxjs";
import { delay } from "rxjs/operators";

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
}
