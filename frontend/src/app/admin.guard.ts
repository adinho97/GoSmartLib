import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { SuperAdminAuthService } from './services/super-admin-auth.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  
  constructor(
    private superAdminAuthService: SuperAdminAuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    if (this.superAdminAuthService.isAuthenticated()) {
      return true;
    }

    // Not authenticated, redirect to super admin login
    this.router.navigate(['/super-admin-login']);
    return false;
  }
}
