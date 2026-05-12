import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { AppModule } from "./app/app.module";
import axios from "axios";

axios.interceptors.request.use((config) => {
  // Get user role from localStorage
  const userRole = localStorage.getItem("role");

  // SUPER_ADMIN users use JWT token; regular users use Smartschool OAuth token
  let token: string | null = null;
  const roleLower = userRole?.toLowerCase() || "";
  
  if (roleLower === "super_admin") {
    // SUPER_ADMIN must use JWT token
    token = localStorage.getItem("admin_jwt_token");
  } else if (roleLower) {
    // Regular users use Smartschool OAuth token
    token = localStorage.getItem("smartschoolToken");
  } else {
    // No role found, try both token types (fallback for missing role)
    token = localStorage.getItem("admin_jwt_token") || localStorage.getItem("smartschoolToken");
  }

  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
