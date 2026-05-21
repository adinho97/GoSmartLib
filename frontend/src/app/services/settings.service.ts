import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthContextService } from './auth-context.service';

export interface SchoolSettings {
  messages: any[];
  hours: any;
  levels: any[];
}

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private readonly apiUrl = '/api/settings';

  constructor(
    private http: HttpClient,
    private authContext: AuthContextService
  ) {}

  private getHeaders() {
    return {
      headers: {
        'X-User-Role': this.authContext.getEffectiveRole(),
        'X-User-Sub': this.authContext.getEffectiveSub(),
        'Authorization': `Bearer ${this.authContext.getEffectiveBearerToken()}`
      },
    };
  }

  getSettings(schoolId: number): Observable<SchoolSettings> {
    return this.http.get<SchoolSettings>(`${this.apiUrl}/school/${schoolId}`, this.getHeaders());
  }

  saveSettings(schoolId: number, settings: SchoolSettings): Observable<SchoolSettings> {
    return this.http.put<SchoolSettings>(`${this.apiUrl}/school/${schoolId}`, settings, this.getHeaders());
  }

  /**
   * Fetches the name of the school for the settings header.
   */
  getSchoolName(schoolId: number): Observable<{ name: string }> {
    return this.http.get<{ name: string }>(`/api/scholen/${schoolId}/name`, this.getHeaders());
  }
}