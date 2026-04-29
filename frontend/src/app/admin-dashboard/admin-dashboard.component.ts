import { Component, HostListener, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";

@Component({
  selector: "app-admin-dashboard",
  templateUrl: "./admin-dashboard.component.html",
  styleUrls: ["./admin-dashboard.component.css"],
  standalone: false,
})
export class AdminDashboardComponent implements OnInit {
  adminInfo: any = null;
  isLoading: boolean = false;
  accountMenuOpen = false;

  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAdminInfo();
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
