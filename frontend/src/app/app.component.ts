import {
  Component,
  HostListener,
  NgZone,
  ChangeDetectorRef,
  OnInit,
} from "@angular/core";
import { Router, NavigationEnd } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { filter } from "rxjs/operators";
import { Observable } from "rxjs";
import { ExperienceService, LevelInfo } from "./services/experience.service";
import { SchoolService } from "./services/school.service";
import { UserPreferencesService } from "./services/user-preferences.service";
import { RecommendationService } from "./services/recommendation.service";
import { BookService } from "./services/book.service";
import { inferNameParts, composeFullName } from "./utils/name-utils";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  standalone: false,
})
export class AppComponent implements OnInit {
  profileMenuOpen = false;
  overviewMenuOpen = false;
  levelInfo$: Observable<LevelInfo>;
  private readonly roleLikeValues = new Set([
    "leerling", // student
    "leerkracht", // teacher
    "bibbeheerder", // librarian
    "gebruiker", // generic user
  ]);

  // Inject SchoolService

  constructor(
    private router: Router,
    private http: HttpClient,
    private changeDetectorRef: ChangeDetectorRef,
    private experienceService: ExperienceService,
    private userPreferencesService: UserPreferencesService,
    private recommendationService: RecommendationService,
    private schoolService: SchoolService, // Inject SchoolService
    private ngZone: NgZone, // Inject NgZone
    private bookService: BookService,
  ) {
    this.levelInfo$ = this.experienceService.levelInfo$;
  }

  ngOnInit(): void {
    // Initialize preferences from cache (localStorage seed is synchronous)
    this.userPreferencesService.init();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.changeDetectorRef.detectChanges();
      });
  }

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

  get isStudent(): boolean {
    return this.userRole === "leerling";
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
    const firstName = (localStorage.getItem("firstName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();
    const composed = composeFullName(firstName, lastName);

    if (composed) {
      return composed;
    }

    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const { firstName: inferredFirst, lastName: inferredLast } = inferNameParts(
      firstName || null,
      lastName || null,
      nameCandidates,
    );
    const inferredComposed = composeFullName(inferredFirst, inferredLast);

    if (inferredComposed) {
      return inferredComposed;
    }

    const candidates = [
      firstName || lastName,
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("username"),
      localStorage.getItem("name"),
    ];

    for (const candidate of candidates) {
      const normalized = this.normalizeDisplayName(candidate);
      if (normalized) {
        return normalized;
      }
    }

    return "Gebruiker";
  }

  private normalizeDisplayName(raw: string | null): string {
    const value = (raw || "").trim();
    if (!value) {
      return "";
    }

    return this.roleLikeValues.has(value.toLowerCase()) ? "" : value;
  }

  get userRoleLabel(): string {
    const role = this.userRole.toLowerCase();
    if (role === "leerkracht") return "Leerkracht";
    if (role === "bibbeheerder") return "Bibliotheekbeheerder";
    return role
      ? role.charAt(0).toUpperCase() + role.slice(1)
      : "Onbekende rol";
  }

  getLevelTier(level: number): number {
    if (level >= 25) return 5;
    if (level >= 20) return 4;
    if (level >= 15) return 3;
    if (level >= 10) return 2;
    if (level >= 5) return 1;
    return 0;
  }

  getLevelTierLabel(level: number): string {
    const tier = this.getLevelTier(level);
    if (tier === 5) return "Legende";
    if (tier === 4) return "Diamant";
    if (tier === 3) return "Goud";
    if (tier === 2) return "Zilver";
    if (tier === 1) return "Brons";
    return "Beginner";
  }

  toggleProfileMenu(event: Event): void {
    event.stopPropagation();
    this.profileMenuOpen = !this.profileMenuOpen;
  }

  toggleOverviewMenu(event: Event): void {
    event.stopPropagation();
    this.overviewMenuOpen = !this.overviewMenuOpen;
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
    this.userPreferencesService.clearCache();
    this.recommendationService.clearCache();
    this.bookService.clearCache();
    localStorage.clear();
    // Prevent back button access
    window.history.replaceState(null, "", "/login");
    this.router.navigate(["/login"]);
  }

  async navigateToMySchoolBooks(): Promise<void> {
    // Use ngZone.run to ensure Angular change detection runs after async operation
    this.ngZone.run(async () => {
      await this.schoolService.selectUserDefaultSchool();
      this.router.navigate(["/books"]);
    });
  }

  @HostListener("document:click")
  onDocumentClick(): void {
    this.profileMenuOpen = false;
    this.overviewMenuOpen = false;
  }
}
