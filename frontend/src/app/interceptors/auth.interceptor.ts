import { HttpInterceptorFn } from "@angular/common/http";

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

  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
