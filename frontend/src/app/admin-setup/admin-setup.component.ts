import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SuperAdminAuthService } from '../services/super-admin-auth.service';

@Component({
  selector: 'app-admin-setup',
  templateUrl: './admin-setup.component.html',
  styleUrls: ['./admin-setup.component.css'],
  standalone: false
})
export class AdminSetupComponent implements OnInit {
  token: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';
  isLoading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // If already logged in, redirect to admin dashboard
    if (this.superAdminAuthService.isAuthenticated()) {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  submitSetup(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.token.trim() || !this.email.trim() || !this.password.trim()) {
      this.errorMessage = 'Token, e-mailadres en wachtwoord zijn verplicht.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Wachtwoorden komen niet overeen.';
      return;
    }

    this.isLoading = true;

    this.superAdminAuthService.setupSuperAdmin(
      this.token,
      this.email,
      this.password
    ).subscribe({
      next: () => {
        this.successMessage = 'Super admin aangemaakt. U kan nu inloggen.';
        this.isLoading = false;
        setTimeout(() => {
          this.router.navigate(['/super-admin-login']);
        }, 1500);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.error && typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else {
          this.errorMessage = 'Setup mislukt. Probeer opnieuw.';
        }
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/super-admin-login']);
  }
}
