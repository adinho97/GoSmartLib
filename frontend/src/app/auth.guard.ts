import { Injectable } from "@angular/core";
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
} from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { map, catchError } from "rxjs/operators";
import { of } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router, private http: HttpClient) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): boolean | Observable<boolean> | Promise<boolean> {
    // Super admin JWT overrides all role requirements
    const adminToken = this.getAdminToken();
    if (adminToken) {
      return this.validateAdminToken(adminToken);
    }

    // Check if user has role in localStorage
    const userRole = this.getUserRole();
    const accessToken = this.getAccessToken();

    // If no role or token, deny access immediately
    if (!userRole || !accessToken) {
      this.router.navigate(["/login"]);
      return false;
    }

    // Check required roles
    const requiredRoles: string[] = route.data["roles"] || [];
    if (requiredRoles.length > 0 && !requiredRoles.includes(userRole)) {
      this.router.navigate(["/login"]);
      return false;
    }

    // Validate token server-side
    return this.validateTokenWithServer(accessToken).pipe(
      map((isValid) => {
        if (!isValid) {
          console.warn("Access token is invalid or expired");
          localStorage.clear();
          this.router.navigate(["/login"]);
          return false;
        }
        return true;
      }),
      catchError((error) => {
        console.error("Error validating token", error);
        localStorage.clear();
        this.router.navigate(["/login"]);
        return of(false);
      }),
    );
  }

  private validateAdminToken(adminToken: string): Observable<boolean> {
    return this.http.get("/api/admin/validate-token", {
      headers: { Authorization: `Bearer ${adminToken}` },
      responseType: "text",
    }).pipe(
      map(() => true),
      catchError(() => {
        localStorage.removeItem("admin_jwt_token");
        this.router.navigate(["/super-admin-login"]);
        return of(false);
      }),
    );
  }

  private validateTokenWithServer(accessToken: string): Observable<boolean> {
    // Allow dev tokens for development (skip server validation)
    if (accessToken.startsWith("dev-token-")) {
      return of(true);
    }

    // Use the backend token validation endpoint
    // If token is invalid/expired, the server will return false
    return this.http.get<boolean>(
      "/api/auth/validate-token",
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    ).pipe(
      map((isValid) => isValid), // Token is valid if response is true
      catchError(() => of(false)), // Token is invalid if request fails
    );
  }

  private getAdminToken(): string {
    return localStorage.getItem("admin_jwt_token") || "";
  }

  private getUserRole(): string {
    return localStorage.getItem("role") || "";
  }

  private getAccessToken(): string {
    return localStorage.getItem("smartschoolToken") || "";
  }
}
