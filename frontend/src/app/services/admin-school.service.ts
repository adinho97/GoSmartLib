import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import {
  AdminSchoolDashboardItem,
  CreateAdminSchoolRequest,
  CreateAdminSchoolResponse,
  SchoolStatus,
} from "../models/admin-school";
import { SuperAdminAuthService } from "./super-admin-auth.service";

@Injectable({
  providedIn: "root",
})
export class AdminSchoolService {
  private readonly apiUrl = "/api/admin/schools";

  constructor(
    private readonly http: HttpClient,
    private readonly superAdminAuthService: SuperAdminAuthService,
  ) {}

  createSchool(
    request: CreateAdminSchoolRequest,
  ): Observable<CreateAdminSchoolResponse> {
    return this.http.post<CreateAdminSchoolResponse>(this.apiUrl, request, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  getSchools(): Observable<AdminSchoolDashboardItem[]> {
    return this.http.get<AdminSchoolDashboardItem[]>(this.apiUrl, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  updateSchoolStatus(
    schoolId: number,
    status: Exclude<SchoolStatus, "PENDING">,
  ): Observable<AdminSchoolDashboardItem> {
    return this.http.patch<AdminSchoolDashboardItem>(
      `${this.apiUrl}/${schoolId}/status`,
      { status },
      { headers: this.superAdminAuthService.getAuthHeaders() },
    );
  }
}
