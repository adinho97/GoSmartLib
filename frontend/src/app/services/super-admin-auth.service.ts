import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class SuperAdminAuthService {
  private readonly TOKEN_KEY = 'admin_jwt_token';
  private readonly ADMIN_INFO_KEY = 'admin_info';
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {
    this.checkTokenExpiry();
  }

  /**
   * Login with username and password
   */
  login(username: string, password: string): Observable<any> {
    return this.http.post<any>('/api/admin/login', { username, password }).pipe(
      tap(response => {
        this.setToken(response.token);
        this.setAdminInfo(response);
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  /**
   * Get current admin info
   */
  getCurrentAdmin(): Observable<any> {
    return this.http.get<any>('/api/admin/me', {
      headers: this.getAuthHeaders()
    });
  }

  /**
   * Change admin password
   */
  changePassword(oldPassword: string, newPassword: string): Observable<any> {
    return this.http.post<any>('/api/admin/change-password', 
      { oldPassword, newPassword },
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Logout
   */
  logout(): void {
    this.removeToken();
    this.removeAdminInfo();
    this.isAuthenticatedSubject.next(false);
  }

  /**
   * Check if admin is authenticated
   */
  isAuthenticated(): boolean {
    return this.hasToken() && !this.isTokenExpired();
  }

  /**
   * Get JWT token
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Get admin info from local storage
   */
  getAdminInfo(): any {
    const info = localStorage.getItem(this.ADMIN_INFO_KEY);
    return info ? JSON.parse(info) : null;
  }

  /**
   * Validate token with backend
   */
  validateToken(): Observable<any> {
    return this.http.get<any>('/api/admin/validate-token', {
      headers: this.getAuthHeaders()
    });
  }

  /**
   * Get Authorization headers for protected requests
   */
  getAuthHeaders(): any {
    const token = this.getToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }

  /**
   * Private helper methods
   */
  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  private removeToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  private setAdminInfo(info: any): void {
    localStorage.setItem(this.ADMIN_INFO_KEY, JSON.stringify(info));
  }

  private removeAdminInfo(): void {
    localStorage.removeItem(this.ADMIN_INFO_KEY);
  }

  private hasToken(): boolean {
    return localStorage.getItem(this.TOKEN_KEY) !== null;
  }

  private isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    try {
      // Decode JWT (split by '.')
      const parts = token.split('.');
      if (parts.length !== 3) return true;

      // Decode payload (second part)
      const payload = JSON.parse(atob(parts[1]));
      const expiryTime = payload.exp * 1000; // Convert to milliseconds
      return Date.now() >= expiryTime;
    } catch (error) {
      return true;
    }
  }

  private checkTokenExpiry(): void {
    // Check token validity every minute
    setInterval(() => {
      if (this.hasToken() && this.isTokenExpired()) {
        this.logout();
      }
    }, 60000);
  }
}
