import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SetupService, TeacherDto, ConfirmInviteRequest } from '../../services/setup.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-select-bibbeheerder',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './select-bibbeheerder.component.html',
  styleUrl: './select-bibbeheerder.component.css'
})
export class SelectBibbeheerderComponent implements OnInit {
  token: string = '';
  teachers: TeacherDto[] = [];
  selectedTeacherId: number | null = null;
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  error: string = '';
  success: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private setupService: SetupService
  ) {}

  ngOnInit(): void {
    // Get token from session storage or route params
    const sessionToken = sessionStorage.getItem('inviteToken');
    if (sessionToken) {
      this.token = sessionToken;
      this.loadTeachers();
    } else {
      this.error = 'Geen geldige invite sessie. Probeer opnieuw.';
    }
  }

  loadTeachers(): void {
    this.isLoading = true;
    this.setupService.getTeachers(this.token).subscribe({
      next: (teachers: TeacherDto[]) => {
        this.isLoading = false;
        this.teachers = teachers;
        if (teachers.length === 0) {
          this.error = 'Geen leerkrachten gevonden voor deze school';
        }
      },
      error: (err: any) => {
        this.isLoading = false;
        this.error = 'Fout bij ophalen leerkrachten: ' + (err?.error?.message || 'Onbekende fout');
      }
    });
  }

  confirmTeacher(): void {
    if (!this.selectedTeacherId) {
      this.error = 'Selecteer alstublieft een leerkracht';
      return;
    }

    this.isSubmitting = true;
    const request: ConfirmInviteRequest = {
      selectedTeacherId: this.selectedTeacherId
    };

    this.setupService.confirmInvite(this.token, request).subscribe({
      next: (response: any) => {
        this.isSubmitting = false;
        if (response.success) {
          this.success = response.message;
          sessionStorage.removeItem('inviteToken');
          // Redirect to dashboard after 2 seconds
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 2000);
        } else {
          this.error = response.message;
        }
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.error = 'Fout bij bevestigen: ' + (err?.error?.message || 'Onbekende fout');
      }
    });
  }
}
