import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { AppModule } from "./app/app.module";
import axios from "axios";

axios.interceptors.request.use((config: any) => {
  // Get user role from localStorage
  const userRole = localStorage.getItem("role");

  // SUPER_ADMIN users use JWT token; regular users use Smartschool OAuth token
  let token: string | null = null;
  if (userRole === "SUPER_ADMIN" || userRole === "super_admin") {
    token = localStorage.getItem("admin_jwt_token");
  } else {
    token = localStorage.getItem("smartschoolToken");
  }

  if (!config.headers) {
    config.headers = {};
  }

  if (token) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }

  return config;
});

// Apply UI prefs from cookie early so they persist across refresh/login flows
try {
  const UI_COOKIE_KEY = "ui_prefs_v1";
  const nameEQ = UI_COOKIE_KEY + "=";
  const ca = document.cookie.split(";");
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === " ") c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) === 0) {
      const raw = decodeURIComponent(c.substring(nameEQ.length));
      const parsed = JSON.parse(raw || "{}");
      if (parsed["ui_darkMode"]) {
        document.documentElement.classList.add("dark");
      }
      if (parsed["ui_colorblind"]) {
        document.documentElement.classList.add("cb-deuteranopia");
      }
      break;
    }
  }
} catch (err) {
  // ignore
}

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = error.config?.url?.includes("/api/auth/");
    const isAdminUser =
      localStorage.getItem("role") === "SUPER_ADMIN" ||
      localStorage.getItem("role") === "super_admin";

    // Token permanently revoked by Smartschool — clear session and force re-login
    if (
      error.response?.status === 401 &&
      error.response?.data?.code === "TOKEN_REVOKED" &&
      !isAuthEndpoint &&
      !isAdminUser
    ) {
      localStorage.removeItem("smartschoolToken");
      localStorage.removeItem("role");
      localStorage.removeItem("sub");
      localStorage.removeItem("userId");
      localStorage.removeItem("userName");
      localStorage.removeItem("smartschoolPlatform");
      window.location.href = "/";
    }

    return Promise.reject(error);
  },
);

platformBrowserDynamic()
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
