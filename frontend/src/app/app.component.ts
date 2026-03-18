import { Component } from "@angular/core";
import { Router } from "@angular/router";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  standalone: false,
})
export class AppComponent {
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

  get isAddBookActive(): boolean {
    return (
      this.currentUrl.startsWith("/add") || this.currentUrl.startsWith("/isbn")
    );
  }
}
