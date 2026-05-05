import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { ExperienceService } from "../services/experience.service";
import { UserPreferencesService } from "../services/user-preferences.service";
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
  ) {}

  ngOnInit(): void {
    ['admin_jwt_token', 'admin_info', 'adminLibrarySchoolName', 'adminLibrarySchoolId']
      .forEach(key => localStorage.removeItem(key));

    this.route.queryParams.subscribe((params) => {
      const code = params["code"];
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

  loginAsRole(role: string): void {
    this.setRole(role, true);
  }

  private handleSmartschoolCode(code: string): void {
    if (this.isLoading) {
      return;
    }
    this.isLoading = true;

    this.http.post<any>("/api/auth/smartschool-login", { code }).subscribe({
      next: async (userInfo) => {
        console.log("Logged in user:", userInfo);
        const rawFirstName =
          userInfo.actualUserName ||
          userInfo.actualUserFirstName ||
          userInfo.givenName ||
          userInfo.given_name ||
          userInfo.name ||
          userInfo.firstName ||
          userInfo.firstname ||
          "";
        const rawLastName =
          userInfo.actualUserSurname ||
          userInfo.actualUserLastName ||
          userInfo.familyName ||
          userInfo.family_name ||
          userInfo.lastName ||
          userInfo.lastname ||
          userInfo.surname ||
          "";
        const candidates = [
          userInfo.fullname,
          userInfo.fullName,
          userInfo.userName,
          userInfo.username,
          userInfo.name,
        ];
        const { firstName, lastName } = inferNameParts(
          rawFirstName,
          rawLastName,
          candidates,
        );
        let storedFirstName = firstName;
        let storedLastName = lastName;

        if (!storedFirstName || !storedLastName) {
          const fullNameCandidate =
            userInfo.fullname ||
            userInfo.fullName ||
            userInfo.name ||
            userInfo.username ||
            "";
          if (fullNameCandidate && !storedFirstName && !storedLastName) {
            const parts = fullNameCandidate.split(/\s+/).filter(Boolean);
            if (parts.length >= 2) {
              storedLastName = parts[0];
              storedFirstName = parts.slice(1).join(" ");
            } else if (parts.length === 1) {
              storedFirstName = parts[0];
              storedLastName = "";
            }
          } else if (fullNameCandidate) {
            if (!storedFirstName && storedLastName) {
              const parts = fullNameCandidate.split(/\s+/).filter(Boolean);
              for (let i = parts.length - 1; i >= 0; i--) {
                if (parts[i].toLowerCase() !== storedLastName.toLowerCase()) {
                  storedFirstName = parts.slice(i).join(" ");
                  break;
                }
              }
            } else if (!storedLastName && storedFirstName) {
              const parts = fullNameCandidate.split(/\s+/).filter(Boolean);
              for (const part of parts) {
                if (part.toLowerCase() !== storedFirstName.toLowerCase()) {
                  storedLastName = part;
                  break;
                }
              }
            }
          }
        }

        const finalFullName =
          composeFullName(storedFirstName, storedLastName) ||
          userInfo.fullname ||
          userInfo.fullName ||
          userInfo.name ||
          userInfo.username ||
          "Gebruiker";

        localStorage.setItem("userName", finalFullName);
        localStorage.setItem("fullname", finalFullName);
        if (storedFirstName) {
          localStorage.setItem("firstName", storedFirstName);
        }
        if (storedLastName) {
          localStorage.setItem("lastName", storedLastName);
        }
        if (userInfo.username) {
          localStorage.setItem("username", userInfo.username);
        }

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

        if (userInfo.role) {
          localStorage.setItem("role", userInfo.role);
        }

        if (userInfo.schoolId) {
          localStorage.setItem("selectedSchoolId", String(userInfo.schoolId));
        }

        this.userPreferencesService.clearCache();
        await this.userPreferencesService.loadPreferencesFromBackend();

        await this.experienceService.refreshForCurrentUser().catch((error) => {
          console.warn("Failed to hydrate experience after login:", error);
        });

        this.router.navigate(["/dashboard"]);

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

  setRole(role: string, useDevIdentity = false): void {
    localStorage.setItem("role", role);

    // Only seed dev identity fields in explicit dev mode.
    if (useDevIdentity) {
      const devId = "dev-user-" + role + "-" + Date.now();
      localStorage.setItem("smartschoolToken", "dev-token-" + role);
      localStorage.setItem("sub", devId);
      localStorage.setItem("userId", devId);
      if (!localStorage.getItem("userName")) {
        localStorage.setItem(
          "userName",
          role.charAt(0).toUpperCase() + role.slice(1),
        );
      }
    }

    if (!localStorage.getItem("selectedSchoolId")) {
      localStorage.setItem("selectedSchoolId", "1");
    }
    this.router.navigate(["/dashboard"]);
  }
}
