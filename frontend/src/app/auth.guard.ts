import { Injectable } from "@angular/core";
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
} from "@angular/router";
import { Observable } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): boolean | Observable<boolean> | Promise<boolean> {
    const userRole = this.getUserRole();
    const requiredRoles: string[] = route.data["roles"] || [];

    if (requiredRoles.includes(userRole)) {
      return true;
    }

    this.router.navigate(["/login"]);
    return false;
  }

  private getUserRole(): string {
    return localStorage.getItem("role") || "";
  }
}
