import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SuperAdminAuthService } from '../services/super-admin-auth.service';

@Component({
  selector: 'app-super-admin-login',
  templateUrl: './super-admin-login.component.html',
  styleUrls: ['./super-admin-login.component.css'],
  standalone: false
})
export class SuperAdminLoginComponent implements OnInit {
  username: string = '';
  password: string = '';
  isLoading: boolean = false;
  errorMessage: string = '';
  showPassword: boolean = false;

  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    [
      'smartschoolToken', 'role', 'firstName', 'lastName', 'userName',
      'fullname', 'username', 'userId', 'sub', 'selectedSchoolId',
      'smartschoolPlatform',
    ].forEach(key => localStorage.removeItem(key));

    if (this.superAdminAuthService.isAuthenticated()) {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  /**
   * Handle login form submission
   */
  login(): void {
    this.errorMessage = '';

    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage = 'E-mailadres en wachtwoord zijn verplicht';
      return;
    }

    this.isLoading = true;

    this.superAdminAuthService.login(this.username, this.password).subscribe({
      next: (response) => {
        console.log('Super admin login successful:', response);
        this.router.navigate(['/admin/dashboard']);
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Super admin login failed:', error);
        
        if (error.status === 401) {
          this.errorMessage = 'Ongeldig e-mailadres of wachtwoord';
        } else if (error.error && typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else {
          this.errorMessage = 'Inloggen mislukt. Probeer het opnieuw.';
        }
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  /**
   * Toggle password visibility
   */
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  /**
   * Handle Enter key press
   */
  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.login();
    }
  }

  /**
   * Navigate to regular user login (Smartschool)
   */
  goToUserLogin(): void {
    this.router.navigate(['/login']);
  }
}
