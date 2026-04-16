import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { UserPreferencesService } from "../services/user-preferences.service";
import { ExperienceService } from "../services/experience.service";

@Component({
  selector: "app-login",
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
  standalone: false,
})
export class LoginComponent implements OnInit {
  private readonly clientId = "2ebf496d131b";
  private readonly redirectUri = "https://gosmartlibs07.tech/auth/callback";
  public isLoading = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private userPreferencesService: UserPreferencesService,
    private experienceService: ExperienceService,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const code = params["code"];
      const inviteToken = params["invite"];
      
      // Store invite token in sessionStorage if present
      if (inviteToken) {
        sessionStorage.setItem("inviteToken", inviteToken);
      }
      
      if (code) {
        this.handleSmartschoolCode(code);
      }
    });
  }

  smartschoolLogin(): void {
    if (this.isLoading) {
      return;
    }
    const authUrl = `https://oauth.smartschool.be/OAuth?client_id=${this.clientId}&response_type=code&redirect_uri=${encodeURIComponent(this.redirectUri)}&scope=userinfo fulluserinfo sendmessage groupinfo`;
    window.location.href = authUrl;
  }

  private handleSmartschoolCode(code: string): void {
    if (this.isLoading) {
      return;
    }
    this.isLoading = true;

    this.http.post<any>("/api/auth/smartschool-login", { code }).subscribe({
      next: (userInfo) => {
        console.log("Logged in user:", userInfo);
        const firstName =
          userInfo.givenName || userInfo.username || userInfo.name || "";
        const lastName = userInfo.familyName || userInfo.family_name || "";
        localStorage.setItem("userName", `${firstName} ${lastName}`.trim());

        if (userInfo.accessToken) {
          localStorage.setItem("smartschoolToken", userInfo.accessToken);
        }

        this.setRole(userInfo.role);
        if (userInfo.sub) {
          localStorage.setItem("userId", userInfo.sub);
          localStorage.setItem("sub", userInfo.sub);
        }
        if (userInfo.platform) {
          localStorage.setItem("smartschoolPlatform", userInfo.platform);
        }
        this.userPreferencesService.init();

        this.experienceService.refreshForCurrentUser().catch((error) => {
          console.warn("Failed to hydrate experience after login:", error);
        });

        // Check redirectTo field from backend (new invite flow logic)
        const redirectTo = userInfo.redirectTo || "dashboard";
        const path = redirectTo === "select-teacher" ? `/setup/${redirectTo}` : `/${redirectTo}`;
        this.router.navigate([path]);

        this.isLoading = false;
      },
      error: (err) => {
        console.error("Smartschool login failed", err);
        const errorMessage =
          err.error?.message || err.message || "Onbekende fout bij inloggen.";
        console.error("Login error:", errorMessage);
        this.isLoading = false;
        // Navigate back to the clean login page to remove the 'code' from the URL
        this.router.navigate(["/login"]);
      },
    });
  }

  setRole(role: string): void {
    localStorage.setItem("role", role);
    // Set dev token and user ID for dev login flow
    localStorage.setItem("smartschoolToken", "dev-token-" + role);
    localStorage.setItem("sub", "dev-user-" + role + "-" + Date.now());
    localStorage.setItem("userId", "dev-user-" + role + "-" + Date.now());
    localStorage.setItem(
      "userName",
      role.charAt(0).toUpperCase() + role.slice(1),
    );
    if (!localStorage.getItem("selectedSchoolId")) {
      localStorage.setItem("selectedSchoolId", "1");
    }
    this.userPreferencesService.init();
    this.router.navigate(["/dashboard"]);
  }
}
