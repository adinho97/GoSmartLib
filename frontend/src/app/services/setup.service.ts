import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InviteValidationResponse {
  valid: boolean;
  schoolId: string;
  message?: string;
}

export interface TeacherDto {
  id: number;
  sub: string;
  naam: string;
}

export interface ConfirmInviteRequest {
  selectedTeacherId: number;
}

export interface ConfirmInviteResponse {
  success: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class SetupService {
  private apiUrl = '/setup';

  constructor(private http: HttpClient) {}

  /**
   * Validate invite token and store in session
   */
  validateInvite(token: string): Observable<InviteValidationResponse> {
    return this.http.get<InviteValidationResponse>(`${this.apiUrl}/invite/${token}`, {
      withCredentials: true
    });
  }

  /**
   * Get list of teachers for the school (requires valid token in session)
   */
  getTeachers(token: string): Observable<TeacherDto[]> {
    return this.http.get<TeacherDto[]>(`${this.apiUrl}/invite/${token}/teachers`, {
      withCredentials: true
    });
  }

  /**
   * Confirm invite: mark token used + promote selected teacher
   */
  confirmInvite(token: string, request: ConfirmInviteRequest): Observable<ConfirmInviteResponse> {
    return this.http.post<ConfirmInviteResponse>(`${this.apiUrl}/invite/${token}/confirm`, request, {
      withCredentials: true
    });
  }
}
