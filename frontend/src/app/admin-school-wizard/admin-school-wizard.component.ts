import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { AdminSchoolService } from "../services/admin-school.service";
import { CreateAdminSchoolResponse } from "../models/admin-school";

@Component({
  selector: "app-admin-school-wizard",
  templateUrl: "./admin-school-wizard.component.html",
  styleUrls: ["./admin-school-wizard.component.css"],
  standalone: false,
})
export class AdminSchoolWizardComponent {
  step = 1;
  isSubmitting = false;
  errorMessage = "";
  subdomain = "";
  naam = "";
  createdSchool: CreateAdminSchoolResponse | null = null;

  constructor(
    private readonly adminSchoolService: AdminSchoolService,
    private readonly router: Router,
  ) {}

  get normalizedSubdomain(): string {
    return this.subdomain.trim().toLowerCase();
  }

  get previewUrl(): string {
    return this.normalizedSubdomain
      ? `https://${this.normalizedSubdomain}.smartschool.be`
      : "";
  }

  validateSubdomain(): boolean {
    const value = this.normalizedSubdomain;
    return /^[a-z0-9-]+$/.test(value);
  }

  goToStep2(): void {
    this.errorMessage = "";
    if (!this.validateSubdomain()) {
      this.errorMessage =
        "Ongeldig subdomein. Gebruik enkel a-z, 0-9 en een koppelteken.";
      return;
    }
    this.step = 2;
  }

  backToStep1(): void {
    this.errorMessage = "";
    this.step = 1;
  }

  confirmCreate(): void {
    if (this.isSubmitting) {
      return;
    }
    this.errorMessage = "";
    this.isSubmitting = true;

    this.adminSchoolService
      .createSchool({
        subdomain: this.normalizedSubdomain,
        naam: this.naam.trim() || undefined,
      })
      .subscribe({
        next: (created) => {
          this.createdSchool = created;
          this.step = 3;
          this.isSubmitting = false;
        },
        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            "School aanmaken is mislukt. Probeer opnieuw.";
          this.isSubmitting = false;
        },
      });
  }

  goToDashboard(): void {
    this.router.navigate(["/admin/dashboard"]);
  }
}
