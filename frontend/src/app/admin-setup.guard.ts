import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { Observable, map, catchError, of } from 'rxjs';
import { SuperAdminAuthService } from './services/super-admin-auth.service';

@Injectable({
  providedIn: 'root'
})
export class AdminSetupGuard implements CanActivate {
  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean> {
    return this.superAdminAuthService.getSetupStatus().pipe(
      map(status => {
        if (status && status.allowed) {
          return true;
        }
        this.router.navigate(['/super-admin-login']);
        return false;
      }),
      catchError(() => {
        this.router.navigate(['/super-admin-login']);
        return of(false);
      })
    );
  }
}
