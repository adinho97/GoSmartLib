import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";

@Component({
  selector: "app-login",
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
  standalone: false,
})
export class LoginComponent implements OnInit {
  private readonly clientId = "2ebf496d131b";
  private readonly redirectUri = window.location.origin + "/auth/callback";

  constructor(
    private router: Router,
    private route: ActivatedRoute,
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
    const authUrl = `https://oauth.smartschool.be/OAuth/Authorize?client_id=${this.clientId}&response_type=code&redirect_uri=${encodeURIComponent(this.redirectUri)}&scope=userinfo`;
    window.location.href = authUrl;
  }

  private handleSmartschoolCode(code: string): void {
    // TODO: send code to backend for token
    console.log("received code");
    this.setRole("student");
    // temp
  }

  setRole(role: string): void {
    localStorage.setItem("role", role);
    if (!localStorage.getItem("selectedSchoolId")) {
      localStorage.setItem("selectedSchoolId", "1");
    }
    this.router.navigate(["/dashboard"]);
  }
}
