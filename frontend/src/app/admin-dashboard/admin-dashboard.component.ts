import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SuperAdminAuthService } from '../services/super-admin-auth.service';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
  standalone: false
})
export class AdminDashboardComponent implements OnInit {
  adminInfo: any = null;
  isLoading: boolean = false;

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

  /**
   * Logout admin and redirect to login
   */
  logout(): void {
    this.isLoading = true;
    this.superAdminAuthService.logout();
    this.router.navigate(['/super-admin-login']);
  }

}
