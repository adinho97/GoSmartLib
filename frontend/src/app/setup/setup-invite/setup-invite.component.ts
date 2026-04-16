import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SetupService, InviteValidationResponse } from '../../services/setup.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-setup-invite',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './setup-invite.component.html',
  styleUrl: './setup-invite.component.css'
})
export class SetupInviteComponent implements OnInit {
  token: string = '';
  isLoading: boolean = false;
  error: string = '';
  schoolName: string = '';
  isValid: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private setupService: SetupService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.token = params['token'];
      if (this.token) {
        this.validateToken();
      } else {
        this.error = 'Geen geldige token gevonden';
      }
    });
  }

  validateToken(): void {
    this.isLoading = true;
    this.setupService.validateInvite(this.token).subscribe({
      next: (response: InviteValidationResponse) => {
        this.isLoading = false;
        if (response.valid) {
          this.isValid = true;
          this.schoolName = response.schoolId;
        } else {
          this.error = response.message || 'Link niet geldig';
        }
      },
      error: (err: any) => {
        this.isLoading = false;
        this.error = 'Fout bij validatie van link: ' + (err?.error?.message || 'Onbekende fout');
      }
    });
  }

  loginWithSmartschool(): void {
    // Redirect to Smartschool OAuth
    // This will start the OAuth flow which ends at /auth/callback
    window.location.href = '/login?invite=' + this.token;
  }
}
