import { Component, HostListener } from "@angular/core";
import { Router } from "@angular/router";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  standalone: false,
})
export class AppComponent {
  profileMenuOpen = false;

  constructor(private router: Router) {}

  private get currentUrl(): string {
    return (this.router.url || "").toLowerCase();
  }

  get userRole(): string {
    return localStorage.getItem("role") || "";
  }

  get isTeacher(): boolean {
    return this.userRole === "leerkracht";
  }

  get isLibrarian(): boolean {
    return this.userRole === "bibbeheerder";
  }

  get canAccessLoans(): boolean {
    return this.isTeacher || this.isLibrarian;
  }

  get showNavbar(): boolean {
    const url = this.router.url || "";
    const isAuthPage =
      url === "/" ||
      url.startsWith("/login") ||
      url.startsWith("/auth/callback");
    return !!this.userRole && !isAuthPage;
  }

  get isDidacticCollectionActive(): boolean {
    return (
      this.currentUrl.startsWith("/books") &&
      this.currentUrl.includes("genre=didactiek")
    );
  }

  get isBooksListActive(): boolean {
    return (
      this.currentUrl.startsWith("/books") && !this.isDidacticCollectionActive
    );
  }

  get userName(): string {
    return localStorage.getItem("userName") || "Gebruiker";
  }

  toggleProfileMenu(event: Event): void {
    event.stopPropagation();
    this.profileMenuOpen = !this.profileMenuOpen;
  }

  closeProfileMenu(): void {
    this.profileMenuOpen = false;
  }

  logout(): void {
    localStorage.clear();
    this.profileMenuOpen = false;
    this.router.navigate(["/login"]);
  }

  @HostListener("document:click")
  onDocumentClick(): void {
    this.profileMenuOpen = false;
  }
}
