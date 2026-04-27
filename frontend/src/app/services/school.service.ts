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
}
