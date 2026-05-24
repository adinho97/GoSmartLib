import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { ExperienceService } from "../services/experience.service";
import { UserPreferencesService } from "../services/user-preferences.service";
import { DashboardConfigService } from "../services/dashboard-config.service";
import { inferNameParts, composeFullName } from "../utils/name-utils";
import { environment } from "../../environments/environment";

@Component({
  selector: "app-login",
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
  standalone: false,
})
export class LoginComponent implements OnInit {
  private readonly clientId = "2ebf496d131b";
  private readonly redirectUri = environment.smartschool.redirectUri;
  public isLoading = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private experienceService: ExperienceService,
    private userPreferencesService: UserPreferencesService,
    private dashboardConfigService: DashboardConfigService,
  ) {}

  ngOnInit(): void {
    [
      "admin_jwt_token",
      "admin_info",
      "adminLibrarySchoolName",
      "adminLibrarySchoolId",
    ].forEach((key) => localStorage.removeItem(key));

    // Store the returnUrl from the query parameters so it survives the Smartschool redirect
    const returnUrl = this.route.snapshot.queryParams["returnUrl"];
    if (returnUrl) {
      sessionStorage.setItem("login_return_url", returnUrl);
    }

    this.route.queryParams.subscribe((params) => {
      const code = params["code"];
      if (code) {
        this.handleSmartschoolCode(code);
      }
    });
  }

  smartschoolLogin(): void {
    if (this.isLoading) return;
    const authUrl = `https://oauth.smartschool.be/OAuth?client_id=${this.clientId}&response_type=code&redirect_uri=${encodeURIComponent(this.redirectUri)}&scope=userinfo fulluserinfo sendmessage groupinfo`;
    window.location.href = authUrl;
  }

  private handleSmartschoolCode(code: string): void {
    if (this.isLoading) return;
    this.isLoading = true;

    this.http.post<any>("/api/auth/smartschool-login", { code }).subscribe({
      next: async (userInfo) => {
        let rawFirstName: string = (
          userInfo.actualUserFirstName ||
          userInfo.givenName ||
          userInfo.given_name ||
          ""
        ).trim();
        let rawLastName: string = (
          userInfo.actualUserSurname ||
          userInfo.actualUserLastName ||
          userInfo.familyName ||
          userInfo.family_name ||
          ""
        ).trim();
        const displayName = (userInfo.username || "").trim();

        // Smartschool's `voornaam`/`achternaam` fields are unreliable on some
        // platforms — they can contain the surname duplicated in both, or be
        // missing entirely. The `name` field is reliable on Belgian Smartschool:
        // always "LastName FirstName" order. Fall back to parsing it when the
        // structured pair is missing or duplicated.
        if (displayName) {
          const parts = displayName.split(/\s+/);
          if (parts.length >= 2) {
            const sameValues =
              rawFirstName &&
              rawLastName &&
              rawFirstName.toLowerCase() === rawLastName.toLowerCase();
            if (!rawFirstName || !rawLastName || sameValues) {
              rawLastName = parts[0];
              rawFirstName = parts.slice(1).join(" ");
            }
          }
        }

        const { firstName, lastName } = inferNameParts(
          rawFirstName,
          rawLastName,
          [
            userInfo.fullname,
            userInfo.fullName,
            userInfo.userName,
            displayName,
          ],
        );

        const finalFullName =
          composeFullName(firstName, lastName) || displayName || "Gebruiker";

        localStorage.setItem("userName", finalFullName);
        if (firstName) localStorage.setItem("firstName", firstName);
        if (lastName) localStorage.setItem("lastName", lastName);

        if (userInfo.accessToken)
          localStorage.setItem("smartschoolToken", userInfo.accessToken);
        if (userInfo.sub) {
          localStorage.setItem("userId", userInfo.sub);
          localStorage.setItem("sub", userInfo.sub);
        }
        localStorage.setItem("role", userInfo.role);
        if (userInfo.platform)
          localStorage.setItem("smartschoolPlatform", userInfo.platform);
        if (userInfo.schoolId)
          localStorage.setItem("selectedSchoolId", String(userInfo.schoolId));

        this.userPreferencesService.clearCache();
        await this.userPreferencesService.syncNow(); // Use syncNow to ensure fresh fetch
        await this.dashboardConfigService.syncNow(); // Use syncNow to ensure fresh fetch

        await this.experienceService
          .refreshForCurrentUser()
          .catch((err) => console.warn(err));

        const target =
          sessionStorage.getItem("login_return_url") || "/dashboard";
        sessionStorage.removeItem("login_return_url");
        this.router.navigateByUrl(target);
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.router.navigate(["/login"]);
      },
    });
  }

  async setRole(role: string, useDevIdentity = false): Promise<void> {
    localStorage.setItem("role", role);

    if (useDevIdentity) {
      const devId = "dev-user-" + role + "-" + Date.now();
      localStorage.setItem("smartschoolToken", "dev-token-" + role);
      localStorage.setItem("sub", devId);
      localStorage.setItem("userId", devId);
      localStorage.setItem(
        "userName",
        role.charAt(0).toUpperCase() + role.slice(1),
      );
    }

    if (!localStorage.getItem("selectedSchoolId")) {
      localStorage.setItem("selectedSchoolId", "1");
    }

    this.userPreferencesService.clearCache();
    await this.userPreferencesService.syncNow().catch(() => {}); // Use syncNow

    await this.dashboardConfigService.syncNow(); // Use syncNow

    const target = sessionStorage.getItem("login_return_url") || "/dashboard";
    sessionStorage.removeItem("login_return_url");
    this.router.navigateByUrl(target);
  }
}
