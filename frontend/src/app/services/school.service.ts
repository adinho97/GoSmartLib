import { Injectable } from "@angular/core";
import axios from "axios";
import { School } from "../models/school";
import { from, Observable, map } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class SchoolService {
  private readonly apiUrl = "/api/scholen";
  private readonly selectedSchoolIdKey = "selectedSchoolId";

  async getSchools(): Promise<School[]> {
    const res = await axios.get<School[]>(this.apiUrl);
    return res.data;
  }

  /**
   * Returns schools as an Observable for the Map Component
   */
  getAllSchools(): Observable<School[]> {
    return from(axios.get<School[]>(this.apiUrl)).pipe(map((res) => res.data));
  }

  /**
   * Returns a paged list of schools
   */
  getPagedSchools(
    page: number,
    size: number,
    query: string = "",
  ): Observable<{ items: School[]; total: number }> {
    return from(
      axios.get<any>(`${this.apiUrl}/paged`, {
        params: { page, size, query },
      }),
    ).pipe(map((res) => ({ items: res.data.items, total: res.data.total })));
  }

  getSelectedSchoolId(): number | null {
    const value = localStorage.getItem(this.selectedSchoolIdKey);
    if (!value) {
      return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  setSelectedSchoolId(schoolId: number): void {
    localStorage.setItem(this.selectedSchoolIdKey, String(schoolId));
  }

  /**
   * Fetches the logged-in user's assigned school from the database
   * and sets it as the active selection.
   */
  async selectUserDefaultSchool(): Promise<void> {
    try {
      const res = await axios.get<{ schoolId: number }>(
        "/api/gebruikers/me/school",
      );
      this.setSelectedSchoolId(res.data.schoolId);
    } catch (err) {
      console.error("Could not fetch user's assigned school:", err);
    }
  }
}
