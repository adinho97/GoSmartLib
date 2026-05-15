import { Injectable } from "@angular/core";

@Injectable({ providedIn: "root" })
export class AuthContextService {
  isAdminMode(): boolean {
    return !!localStorage.getItem("admin_jwt_token");
  }

  getEffectiveBearerToken(): string {
    return (
      localStorage.getItem("admin_jwt_token") ||
      localStorage.getItem("smartschoolToken") ||
      ""
    );
  }

  getEffectiveRole(): string {
    return this.isAdminMode()
      ? "bibbeheerder"
      : localStorage.getItem("role") || "";
  }

  getEffectiveSub(): string {
    return this.isAdminMode()
      ? "admin"
      : localStorage.getItem("sub") || localStorage.getItem("userId") || "";
  }
}
