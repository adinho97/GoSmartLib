import { Component, HostListener } from "@angular/core";
import { Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  standalone: false,
})
export class AppComponent {
  profileMenuOpen = false;

  constructor(private router: Router, private http: HttpClient) {}

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

  get userRoleLabel(): string {
    const role = this.userRole.toLowerCase();
    if (role === "leerkracht") return "Leerkracht";
    if (role === "bibbeheerder") return "Bibliotheekbeheerder";
    return role ? role.charAt(0).toUpperCase() + role.slice(1) : "Onbekende rol";
  }

  toggleProfileMenu(event: Event): void {
    event.stopPropagation();
    this.profileMenuOpen = !this.profileMenuOpen;
  }

  closeProfileMenu(): void {
    this.profileMenuOpen = false;
  }

  logout(): void {
    const accessToken = localStorage.getItem("smartschoolToken");
    this.profileMenuOpen = false;

    if (accessToken) {
      this.http.post("/api/auth/logout", { accessToken }).subscribe({
        next: () => {
          console.log("Logged out successfully");
          this.completeLogout();
        },
        error: (err) => {
          console.warn("Error revoking token, but proceeding with logout", err);
          this.completeLogout();
        },
      });
    } else {
      this.completeLogout();
    }
  }

  private completeLogout(): void {
    localStorage.clear();
    // Prevent back button access
    window.history.replaceState(null, "", "/login");
    this.router.navigate(["/login"]);
  }

  @HostListener("document:click")
  onDocumentClick(): void {
    this.profileMenuOpen = false;
  }
}
