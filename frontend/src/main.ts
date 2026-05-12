import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { AppModule } from "./app/app.module";
import axios from "axios";

axios.interceptors.request.use((config) => {
  // Get user role from localStorage
  const userRole = localStorage.getItem("role");

  // SUPER_ADMIN users use JWT token; regular users use Smartschool OAuth token
  let token: string | null = null;
  if (userRole === "SUPER_ADMIN" || userRole === "super_admin") {
    token = localStorage.getItem("admin_jwt_token");
  } else {
    token = localStorage.getItem("smartschoolToken");
  }

  if (token && config.headers) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

platformBrowserDynamic()
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
