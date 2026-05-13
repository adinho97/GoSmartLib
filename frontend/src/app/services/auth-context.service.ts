import { Injectable } from "@angular/core";

@Injectable({ providedIn: "root" })
export class AuthContextService {
  isAdminMode(): boolean {
    return !!localStorage.getItem("admin_jwt_token");
  }

  getEffectiveBearerToken(): string {
    // Get user role from localStorage
    const userRole = localStorage.getItem("role");
    const roleLower = userRole?.toLowerCase() || "";

    // SUPER_ADMIN users use JWT token; regular users use Smartschool OAuth token
    if (roleLower === "super_admin") {
      return localStorage.getItem("admin_jwt_token") || "";
    } else if (roleLower) {
      return localStorage.getItem("smartschoolToken") || "";
    } else {
      // No role found, try both token types (fallback)
      return (
        localStorage.getItem("admin_jwt_token") ||
        localStorage.getItem("smartschoolToken") ||
        ""
      );
    }
  }

  getEffectiveRole(): string {
    return localStorage.getItem("role") || "";
  }

  getEffectiveSub(): string {
    return localStorage.getItem("sub") || localStorage.getItem("userId") || "";
  }
}
