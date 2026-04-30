import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";

@Component({
  selector: "app-admin-change-password",
  templateUrl: "./admin-change-password.component.html",
  styleUrls: ["./admin-change-password.component.css"],
  standalone: false,
})
export class AdminChangePasswordComponent {
  oldPassword = "";
  newPassword = "";
  confirmPassword = "";
  isSaving = false;
  errorMessage = "";
  successMessage = "";

  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private router: Router,
  ) {}

  submit(): void {
    this.errorMessage = "";
    this.successMessage = "";

    if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
      this.errorMessage = "Vul alle velden in.";
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = "Nieuwe wachtwoorden komen niet overeen.";
      return;
    }

    if (!this.isPasswordStrong(this.newPassword)) {
      this.errorMessage =
        "Wachtwoord moet minstens 8 tekens bevatten, met hoofdletter, kleine letter en cijfer.";
      return;
    }

    this.isSaving = true;
    this.superAdminAuthService
      .changePassword(this.oldPassword, this.newPassword)
      .subscribe({
        next: () => {
          this.successMessage = "Wachtwoord succesvol gewijzigd.";
          this.oldPassword = "";
          this.newPassword = "";
          this.confirmPassword = "";
          this.isSaving = false;
        },
        error: (error) => {
          this.errorMessage =
            error?.error?.message || "Wachtwoord wijzigen is mislukt.";
          this.isSaving = false;
        },
      });
  }

  private isPasswordStrong(password: string): boolean {
    if (!password || password.length < 8) {
      return false;
    }
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasDigit = /[0-9]/.test(password);
    return hasUpper && hasLower && hasDigit;
  }

  backToDashboard(): void {
    this.router.navigate(["/admin/dashboard"]);
  }
}
