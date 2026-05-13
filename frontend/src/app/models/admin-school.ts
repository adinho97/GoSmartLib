export type SchoolStatus = "PENDING" | "ACTIVE" | "INACTIVE";

export interface CreateAdminSchoolRequest {
  subdomain: string;
  naam?: string;
  adres?: string;
  latitude?: number;
  longitude?: number;
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

export interface SchoolDetail {
  id: number;
  subdomain: string;
  smartschoolUrl: string;
  naam: string | null;
  adres: string | null;
  latitude: number | null;
  longitude: number | null;
  status: SchoolStatus;
  aangemaaktOp: string;
  userCount: number;
  klasCount: number;
  bookCount: number;
  activeLoansCount: number;
  wishlistCount: number;
  classReadingListCount: number;
}

export interface UpdateSchoolInfoRequest {
  naam?: string | null;
  adres?: string | null;
  latitude?: number | null;
  longitude?: number | null;
}

export interface AdminUserListItem {
  id: number;
  sub: string;
  displayName: string | null;
  role: string;
  klasNaam: string | null;
  active: boolean;
}

export interface KlasListItem {
  id: number;
  groupId: string;
  naam: string;
}
