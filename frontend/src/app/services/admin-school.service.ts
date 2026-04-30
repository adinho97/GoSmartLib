import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import {
  AdminSchoolDashboardItem,
  AdminUserListItem,
  CreateAdminSchoolRequest,
  CreateAdminSchoolResponse,
  KlasListItem,
  SchoolDetail,
  SchoolStatus,
  UpdateSchoolInfoRequest,
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

  createSchool(request: CreateAdminSchoolRequest): Observable<CreateAdminSchoolResponse> {
    return this.http.post<CreateAdminSchoolResponse>(this.apiUrl, request, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  getSchools(): Observable<AdminSchoolDashboardItem[]> {
    return this.http.get<AdminSchoolDashboardItem[]>(this.apiUrl, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  getSchoolDetail(id: number): Observable<SchoolDetail> {
    return this.http.get<SchoolDetail>(`${this.apiUrl}/${id}`, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  updateSchoolInfo(id: number, request: UpdateSchoolInfoRequest): Observable<SchoolDetail> {
    return this.http.patch<SchoolDetail>(`${this.apiUrl}/${id}/info`, request, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  updateSchoolStatus(
    schoolId: number,
    status: Exclude<SchoolStatus, "PENDING">,
  ): Observable<SchoolDetail> {
    return this.http.patch<SchoolDetail>(
      `${this.apiUrl}/${schoolId}/status`,
      { status },
      { headers: this.superAdminAuthService.getAuthHeaders() },
    );
  }

  getSchoolUsers(id: number): Observable<AdminUserListItem[]> {
    return this.http.get<AdminUserListItem[]>(`${this.apiUrl}/${id}/users`, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }

  toggleUserActive(schoolId: number, userId: number): Observable<AdminUserListItem> {
    return this.http.patch<AdminUserListItem>(
      `${this.apiUrl}/${schoolId}/users/${userId}/active`,
      {},
      { headers: this.superAdminAuthService.getAuthHeaders() },
    );
  }

  setUserRole(schoolId: number, userId: number, role: string): Observable<AdminUserListItem> {
    return this.http.patch<AdminUserListItem>(
      `${this.apiUrl}/${schoolId}/users/${userId}/role`,
      { role },
      { headers: this.superAdminAuthService.getAuthHeaders() },
    );
  }

  getSchoolKlassen(id: number): Observable<KlasListItem[]> {
    return this.http.get<KlasListItem[]>(`${this.apiUrl}/${id}/klassen`, {
      headers: this.superAdminAuthService.getAuthHeaders(),
    });
  }
}
