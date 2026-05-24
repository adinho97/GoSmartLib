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
import { DashboardConfigService } from "./services/dashboard-config.service";
import { RecommendationService } from "./services/recommendation.service";
import { BookService } from "./services/book.service";
import { SuperAdminAuthService } from "./services/super-admin-auth.service";
import {
  inferNameParts,
  composeFullName,
  normalizeReviewAuthorName,
} from "./utils/name-utils";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  standalone: false,
})
export class AppComponent implements OnInit {
  profileMenuOpen = false;
  adminNavMenuOpen = false;
  mainNavOpen = false;
  adminMobileMenuOpen = false;
  levelInfo$: Observable<LevelInfo>;
  private readonly roleLikeValues = new Set([
    "leerling",
    "leerkracht",
    "bibbeheerder",
    "gebruiker",
  ]);

  constructor(
    private router: Router,
    private http: HttpClient,
    private changeDetectorRef: ChangeDetectorRef,
    private experienceService: ExperienceService,
    private userPreferencesService: UserPreferencesService,
    private recommendationService: RecommendationService,
    private schoolService: SchoolService,
    private ngZone: NgZone,
    private bookService: BookService,
    private superAdminAuthService: SuperAdminAuthService,
    private dashboardConfigService: DashboardConfigService,
  ) {
    this.levelInfo$ = this.experienceService.levelInfo$;
  }

  ngOnInit(): void {
    // Initialize preferences from cache (localStorage seed is synchronous)
    this.userPreferencesService.init();
    this.dashboardConfigService.init();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.profileMenuOpen = false;
        this.adminNavMenuOpen = false;
        this.mainNavOpen = false;
        this.adminMobileMenuOpen = false;
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
      url.startsWith("/auth/callback") ||
      url.startsWith("/super-admin-login");
    const isAdminSession = !!localStorage.getItem("admin_jwt_token");
    return !!this.userRole && !isAuthPage && !isAdminSession;
  }

  get showStudentNav(): boolean {
    return this.showNavbar && this.isStudent;
  }

  get showStaffNav(): boolean {
    return this.showNavbar && !this.isStudent && !this.showAdminNav;
  }

  get showFooter(): boolean {
    return this.showNavbar;
  }

  get userInitial(): string {
    const name = this.userName;
    return name ? name.charAt(0).toUpperCase() : "G";
  }

  get showAdminBar(): boolean {
    const url = this.router.url || "";
    const isExcluded =
      url === "/" ||
      url.startsWith("/login") ||
      url.startsWith("/auth/callback") ||
      url.startsWith("/admin") ||
      url.startsWith("/super-admin");
    return !!localStorage.getItem("admin_jwt_token") && !isExcluded;
  }

  get adminSchoolName(): string {
    return localStorage.getItem("adminLibrarySchoolName") || "School";
  }

  get adminSchoolId(): string {
    return localStorage.getItem("adminLibrarySchoolId") || "";
  }

  goBackToAdmin(): void {
    const schoolId = this.adminSchoolId;
    if (schoolId) {
      this.router.navigate(["/admin/schools", schoolId]);
    } else {
      this.router.navigate(["/admin/dashboard"]);
    }
  }

  get isDidacticCollectionActive(): boolean {
    return (
      this.currentUrl.startsWith("/books") &&
      this.currentUrl.includes("genre=didactiek")
    );
  }

  get isTakenMenuActive(): boolean {
    return this.currentUrl.startsWith("/uitleen");
  }

  get isBooksListActive(): boolean {
    return (
      this.currentUrl.startsWith("/books") && !this.isDidacticCollectionActive
    );
  }

  get userName(): string {
    const firstName = (localStorage.getItem("firstName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();
    const userNameKey = (localStorage.getItem("userName") || "").trim();

    // 1. If userName contains a full name (2+ words), use it and normalize the order
    if (userNameKey.split(/\s+/).filter(Boolean).length >= 2) {
      return normalizeReviewAuthorName(userNameKey);
    }

    // 2. If userName and lastName are distinct, combine them (User often contains Firstname)
    if (
      userNameKey &&
      lastName &&
      userNameKey.toLowerCase() !== lastName.toLowerCase()
    ) {
      return composeFullName(userNameKey, lastName);
    }

    const composed = composeFullName(firstName, lastName);
    if (composed) {
      return composed;
    }

    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("username"),
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

    // If still no composed name, prefer a multi-word string and normalize it
    const fullName = nameCandidates.find((c) => {
      const val = (c || "").trim();
      return val.split(/\s+/).filter(Boolean).length >= 2;
    });
    if (fullName) {
      return normalizeReviewAuthorName(fullName);
    }

    // 5. Fallback to single names if no full name string is available.
    // We prefer the 'Display Name' (userName) over just the last name.
    const fallbacks = [firstName || userNameKey || lastName, ...nameCandidates];
    for (const candidate of fallbacks) {
      const normalized = this.normalizeDisplayName(candidate);
      if (normalized) {
        return normalizeReviewAuthorName(normalized);
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
    if (this.profileMenuOpen) {
      this.mainNavOpen = false;
      this.adminMobileMenuOpen = false;
    }
  }

  toggleMainNav(event: Event): void {
    event.stopPropagation();
    this.mainNavOpen = !this.mainNavOpen;
    if (this.mainNavOpen) {
      this.profileMenuOpen = false;
      this.adminNavMenuOpen = false;
      this.adminMobileMenuOpen = false;
    }
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
    this.dashboardConfigService.clearCache();
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

  get showAdminNav(): boolean {
    const url = this.router.url || "";
    return (
      !!localStorage.getItem("admin_jwt_token") &&
      url.startsWith("/admin") &&
      !url.startsWith("/admin/setup")
    );
  }

  get adminUsername(): string {
    return this.superAdminAuthService.getAdminInfo()?.username || "Beheerder";
  }

  toggleAdminNavMenu(event: Event): void {
    event.stopPropagation();
    this.adminNavMenuOpen = !this.adminNavMenuOpen;
    if (this.adminNavMenuOpen) {
      this.mainNavOpen = false;
      this.profileMenuOpen = false;
      this.adminMobileMenuOpen = false;
    }
  }

  toggleAdminMobileNav(event: Event): void {
    event.stopPropagation();
    this.adminMobileMenuOpen = !this.adminMobileMenuOpen;
    if (this.adminMobileMenuOpen) {
      this.mainNavOpen = false;
      this.profileMenuOpen = false;
      this.adminNavMenuOpen = false;
    }
  }

  adminNavGoToNewSchool(): void {
    this.adminNavMenuOpen = false;
    this.router.navigate(["/admin/schools/new"]);
  }

  adminNavGoToChangePassword(): void {
    this.adminNavMenuOpen = false;
    this.router.navigate(["/admin/change-password"]);
  }

  adminNavLogout(): void {
    this.adminNavMenuOpen = false;
    this.superAdminAuthService.logout();
    this.router.navigate(["/super-admin-login"]);
  }

  @HostListener("document:click")
  onDocumentClick(): void {
    this.profileMenuOpen = false;
    this.adminNavMenuOpen = false;
    this.mainNavOpen = false;
    this.adminMobileMenuOpen = false;
  }

  get isSettingsActive(): boolean {
    return this.currentUrl.startsWith("/settings");
  }
}
