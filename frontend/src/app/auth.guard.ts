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

  private validateTokenWithServer(accessToken: string): Observable<boolean> {
    // Use the userinfo endpoint to validate the token
    // If token is invalid/expired, the server will return 401
    return this.http.get(
      "/api/V1/userinfo",
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    ).pipe(
      map(() => true), // Token is valid
      catchError(() => of(false)), // Token is invalid
    );
  }

  private getUserRole(): string {
    return localStorage.getItem("role") || "";
  }

  private getAccessToken(): string {
    return localStorage.getItem("smartschoolToken") || "";
  }
}
