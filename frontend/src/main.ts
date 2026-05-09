import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { AppModule } from "./app/app.module";
import axios from "axios";

axios.interceptors.request.use((config) => {
  const token =
    localStorage.getItem("admin_jwt_token") ||
    localStorage.getItem("smartschoolToken");
  if (token && config.headers) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

platformBrowserDynamic()
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));