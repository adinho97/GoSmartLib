import { Component, HostListener, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";
import { AdminSchoolService } from "../services/admin-school.service";
import { AdminSchoolDashboardItem } from "../models/admin-school";

@Component({
  selector: "app-admin-dashboard",
  templateUrl: "./admin-dashboard.component.html",
  styleUrls: ["./admin-dashboard.component.css"],
  standalone: false,
})
export class AdminDashboardComponent implements OnInit {
  adminInfo: any = null;
  isLoading: boolean = false;
  isLoadingSchools: boolean = false;
  schoolError = "";
  schools: AdminSchoolDashboardItem[] = [];
  accountMenuOpen = false;

  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private adminSchoolService: AdminSchoolService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAdminInfo();
    this.loadSchools();
  }

  /**
   * Load current admin information
   */
  loadAdminInfo(): void {
    this.adminInfo = this.superAdminAuthService.getAdminInfo();
  }

  toggleAccountMenu(): void {
    this.accountMenuOpen = !this.accountMenuOpen;
  }

  goToChangePassword(): void {
    this.accountMenuOpen = false;
    this.router.navigate(["/admin/change-password"]);
  }

  goToSchoolWizard(): void {
    this.router.navigate(["/admin/schools/new"]);
  }

  loadSchools(): void {
    this.isLoadingSchools = true;
    this.schoolError = "";
    this.adminSchoolService.getSchools().subscribe({
      next: (schools) => {
        this.schools = schools;
        this.isLoadingSchools = false;
      },
      error: (error) => {
        this.schoolError =
          error?.error?.message || "Scholen laden is mislukt. Probeer opnieuw.";
        this.isLoadingSchools = false;
      },
    });
  }

  toggleSchoolStatus(school: AdminSchoolDashboardItem): void {
    const nextStatus = school.status === "INACTIVE" ? "ACTIVE" : "INACTIVE";
    this.adminSchoolService.updateSchoolStatus(school.id, nextStatus).subscribe({
      next: (updatedSchool) => {
        this.schools = this.schools.map((s) =>
          s.id === updatedSchool.id ? updatedSchool : s,
        );
      },
      error: (error) => {
        this.schoolError =
          error?.error?.message || "Status updaten is mislukt. Probeer opnieuw.";
      },
    });
  }

  /**
   * Logout admin and redirect to login
   */
  logout(): void {
    this.accountMenuOpen = false;
    this.isLoading = true;
    this.superAdminAuthService.logout();
    this.router.navigate(["/super-admin-login"]);
  }

  @HostListener("document:click", ["$event"])
  closeMenuOnOutsideClick(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest(".account-dropdown")) {
      this.accountMenuOpen = false;
    }
  }
}
