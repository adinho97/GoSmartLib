import { Injectable } from "@angular/core";
import axios from "axios";
import { School } from "../models/school";

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
