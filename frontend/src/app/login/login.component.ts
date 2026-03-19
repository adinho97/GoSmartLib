import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";
import { HttpClient } from "@angular/common/http";

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
  ) {}

  ngOnInit(): void {
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
    const authUrl = `https://oauth.smartschool.be/OAuth?client_id=${this.clientId}&response_type=code&redirect_uri=${encodeURIComponent(this.redirectUri)}&scope=userinfo`;
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
        localStorage.setItem("userName", `${userInfo.given_name} ${userInfo.family_name}`);
        this.setRole(userInfo.role);
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
    if (!localStorage.getItem("selectedSchoolId")) {
      localStorage.setItem("selectedSchoolId", "1");
    }
    this.router.navigate(["/dashboard"]);
  }
}
