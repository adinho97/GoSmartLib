import { Component, HostListener, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";
import { AdminSchoolService } from "../services/admin-school.service";
import { AdminSchoolDashboardItem, SchoolStatus } from "../models/admin-school";

@Component({
  selector: "app-admin-dashboard",
  templateUrl: "./admin-dashboard.component.html",
  styleUrls: ["./admin-dashboard.component.css"],
  standalone: false,
})
export class AdminDashboardComponent implements OnInit {
  adminInfo: any = null;
  isLoading = false;
  isLoadingSchools = false;
  schoolError = "";
  schools: AdminSchoolDashboardItem[] = [];
  accountMenuOpen = false;

  constructor(
    private readonly superAdminAuthService: SuperAdminAuthService,
    private readonly adminSchoolService: AdminSchoolService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.adminInfo = this.superAdminAuthService.getAdminInfo();
    this.loadSchools();
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

  goToSchoolDetail(id: number): void {
    this.router.navigate(["/admin/schools", id]);
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
        this.schoolError = error?.error?.message || "Scholen laden is mislukt. Probeer opnieuw.";
        this.isLoadingSchools = false;
      },
    });
  }

  statusLabel(status: SchoolStatus): string {
    switch (status) {
      case "ACTIVE": return "Actief";
      case "INACTIVE": return "Inactief";
      case "PENDING": return "In afwachting";
    }
  }

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
