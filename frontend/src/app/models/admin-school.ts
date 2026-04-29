export type SchoolStatus = "PENDING" | "ACTIVE" | "INACTIVE";

export interface CreateAdminSchoolRequest {
  subdomain: string;
  naam?: string;
}

export interface CreateAdminSchoolResponse {
  id: number;
  subdomain: string;
  smartschoolUrl: string;
  status: SchoolStatus;
  createdAt: string;
}

export interface AdminSchoolDashboardItem {
  id: number;
  naam: string | null;
  subdomain: string;
  status: SchoolStatus;
  userCount: number;
  klasCount: number;
}
