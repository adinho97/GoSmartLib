import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { forkJoin, of } from "rxjs";
import { catchError } from "rxjs/operators";
import { AdminSchoolService } from "../services/admin-school.service";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";
import { AdminSchoolDashboardItem, SchoolDetail } from "../models/admin-school";

type EnrichedSchool = AdminSchoolDashboardItem & {
  bookCount: number;
  activeLoansCount: number;
};

@Component({
  selector: "app-super-admin-dashboard",
  templateUrl: "./super-admin-dashboard.component.html",
  styleUrls: ["./super-admin-dashboard.component.css"],
  standalone: false,
})
export class SuperAdminDashboardComponent implements OnInit {
  schools: AdminSchoolDashboardItem[] = [];
  schoolDetails: SchoolDetail[] = [];
  isLoading = true;
  isLoadingDetails = false;
  error = "";
  topMetric: "users" | "loans" = "users";

  constructor(
    private readonly adminSchoolService: AdminSchoolService,
    private readonly superAdminAuthService: SuperAdminAuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.adminSchoolService.getSchools().subscribe({
      next: (schools) => {
        this.schools = schools;
        this.isLoading = false;
        this.loadDetails(schools);
      },
      error: () => {
        this.error = "Scholen konden niet worden geladen.";
        this.isLoading = false;
      },
    });
  }

  private loadDetails(schools: AdminSchoolDashboardItem[]): void {
    if (!schools.length) return;
    this.isLoadingDetails = true;
    forkJoin(
      schools.map((s) =>
        this.adminSchoolService.getSchoolDetail(s.id).pipe(catchError(() => of(null))),
      ),
    ).subscribe((details) => {
      this.schoolDetails = details.filter((d): d is SchoolDetail => d !== null);
      this.isLoadingDetails = false;
    });
  }

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return "Goedemorgen";
    if (h < 18) return "Goedemiddag";
    return "Goedenavond";
  }

  get adminUsername(): string {
    return this.superAdminAuthService.getAdminInfo()?.username || "Beheerder";
  }

  get activeSchoolCount(): number {
    return this.schools.filter((s) => s.status === "ACTIVE").length;
  }

  get pendingSchoolCount(): number {
    return this.schools.filter((s) => s.status === "PENDING").length;
  }

  get pendingSchools(): AdminSchoolDashboardItem[] {
    return this.schools.filter((s) => s.status === "PENDING");
  }

  get totalUsers(): number {
    return this.schools.reduce((sum, s) => sum + (s.userCount ?? 0), 0);
  }

  get totalBooks(): number {
    return this.schoolDetails.reduce((sum, s) => sum + (s.bookCount ?? 0), 0);
  }

  get totalActiveLoans(): number {
    return this.schoolDetails.reduce((sum, s) => sum + (s.activeLoansCount ?? 0), 0);
  }

  get enrichedSchools(): EnrichedSchool[] {
    const map = new Map(this.schoolDetails.map((d) => [d.id, d]));
    return this.schools.map((s) => ({
      ...s,
      bookCount: map.get(s.id)?.bookCount ?? 0,
      activeLoansCount: map.get(s.id)?.activeLoansCount ?? 0,
    }));
  }

  get sortedSchools(): EnrichedSchool[] {
    const order = (s: EnrichedSchool) =>
      s.status === "PENDING" ? 0 : s.status === "ACTIVE" ? 1 : 2;
    return [...this.enrichedSchools].sort((a, b) => {
      if (order(a) !== order(b)) return order(a) - order(b);
      return (b.userCount ?? 0) - (a.userCount ?? 0);
    });
  }

  get topSchools(): EnrichedSchool[] {
    const key = this.topMetric === "users" ? "userCount" : "activeLoansCount";
    return [...this.enrichedSchools]
      .sort((a, b) => (b[key] ?? 0) - (a[key] ?? 0))
      .slice(0, 5);
  }

  get topMetricMax(): number {
    const key = this.topMetric === "users" ? "userCount" : "activeLoansCount";
    return Math.max(1, ...this.topSchools.map((s) => s[key] ?? 0));
  }

  schoolDisplayName(s: AdminSchoolDashboardItem): string {
    return s.naam ?? s.subdomain;
  }

  goToNewSchool(): void {
    this.router.navigate(["/admin/schools/new"]);
  }

  goToScholen(): void {
    this.router.navigate(["/admin/scholen"]);
  }

  goToSchoolDetail(id: number): void {
    this.router.navigate(["/admin/schools", id]);
  }
}
