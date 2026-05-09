import { HttpInterceptorFn } from "@angular/common/http";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith("/api/")) {
    return next(req);
  }
  if (req.headers.has("Authorization")) {
    return next(req);
  }
  const token =
    localStorage.getItem("admin_jwt_token") ||
    localStorage.getItem("smartschoolToken");
  if (!token) {
    return next(req);
  }
  return next(
    req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }),
  );
};
