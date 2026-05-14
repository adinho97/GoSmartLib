import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith("/api/")) {
    return next(req);
  }
  if (req.headers.has("Authorization")) {
    return next(req);
  }

  // Get user role from localStorage
  const userRole = localStorage.getItem("role");

  // SUPER_ADMIN users use JWT token; regular users use Smartschool OAuth token
  let token: string | null = null;
  if (userRole === "SUPER_ADMIN" || userRole === "super_admin") {
    token = localStorage.getItem("admin_jwt_token");
  } else {
    token = localStorage.getItem("smartschoolToken");
  }

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  const router = inject(Router);

  return next(authReq).pipe(
    catchError((error) => {
      if (
        error.status === 401 &&
        error.error?.code === "TOKEN_REVOKED" &&
        !req.url.includes("/api/auth/")
      ) {
        // Session expired due to revoked Smartschool token — clear local state
        // and send user back to login so they can re-authenticate via OAuth.
        localStorage.removeItem("smartschoolToken");
        localStorage.removeItem("role");
        localStorage.removeItem("sub");
        localStorage.removeItem("userId");
        localStorage.removeItem("userName");
        localStorage.removeItem("smartschoolPlatform");
        router.navigate(["/"]);
      }
      return throwError(() => error);
    })
  );
};
