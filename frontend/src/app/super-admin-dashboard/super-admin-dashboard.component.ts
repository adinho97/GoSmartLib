import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { AdminSchoolService } from "../services/admin-school.service";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";
import { AdminSchoolDashboardItem } from "../models/admin-school";

interface ActivityItem {
  id: number;
  type: "add" | "edit" | "warn" | "del";
  text: string;
  school: string;
  time: string;
}

interface TaskItem {
  id: number;
  title: string;
  sub: string;
  action: string;
  urgent?: boolean;
}

@Component({
  selector: "app-super-admin-dashboard",
  templateUrl: "./super-admin-dashboard.component.html",
  styleUrls: ["./super-admin-dashboard.component.css"],
  standalone: false,
})
export class SuperAdminDashboardComponent implements OnInit {
  schools: AdminSchoolDashboardItem[] = [];
  isLoading = true;
  error = "";

  readonly activity: ActivityItem[] = [
    { id: 1, type: "add", text: "Nieuwe leerling Adrian Dyszczak toegevoegd", school: "GO! Atheneum Antwerpen", time: "5 min" },
    { id: 2, type: "warn", text: "School heeft 3+ dagen geen sync", school: "Aphogeschool", time: "12 min" },
    { id: 3, type: "add", text: "47 boeken bulk-geïmporteerd uit ISBN-lijst", school: "GO! Middenschool Centrum", time: "38 min" },
    { id: 4, type: "edit", text: "Rol van gebruiker gewijzigd: Leerling → Leerkracht", school: "GO! Basisschool De Brug", time: "1 u" },
    { id: 5, type: "add", text: "Nieuwe school ingediend voor goedkeuring", school: "Nieuwe School", time: "2 u" },
    { id: 6, type: "del", text: "Account van gebruiker gedeactiveerd", school: "GO! Atheneum Antwerpen", time: "3 u" },
    { id: 7, type: "edit", text: "Klas hernoemd", school: "GO! Atheneum Antwerpen", time: "5 u" },
    { id: 8, type: "add", text: "Boek toegevoegd aan klasleeslijst", school: "GO! Middenschool Centrum", time: "6 u" },
  ];

  readonly tasks: TaskItem[] = [
    { id: 1, title: "School goedkeuren", sub: "Wacht sinds 2 dagen", action: "Bekijk", urgent: true },
    { id: 2, title: "3 gebruikers met password reset request", sub: "Meerdere scholen", action: "Behandel" },
    { id: 3, title: "School met lage activiteit", sub: "Geen sync sinds 3 dagen — admin contacteren?", action: "Bekijk" },
    { id: 4, title: "12 dubbele ISBN's gedetecteerd", sub: "Verspreid over meerdere scholen — review nodig", action: "Open lijst" },
  ];

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
      },
      error: () => {
        this.error = "Scholen konden niet worden geladen.";
        this.isLoading = false;
      },
    });
  }

  get adminUsername(): string {
    return this.superAdminAuthService.getAdminInfo()?.username || "Beheerder";
  }

  get totalUsers(): number {
    return this.schools.reduce((sum, s) => sum + (s.userCount ?? 0), 0);
  }

  get totalKlassen(): number {
    return this.schools.reduce((sum, s) => sum + (s.klasCount ?? 0), 0);
  }

  get activeSchoolCount(): number {
    return this.schools.filter((s) => s.status === "ACTIVE").length;
  }

  get pendingSchoolCount(): number {
    return this.schools.filter((s) => s.status === "PENDING").length;
  }

  get issueCount(): number {
    return this.pendingSchoolCount + this.tasks.filter((t) => t.urgent).length;
  }

  get topSchools(): AdminSchoolDashboardItem[] {
    return [...this.schools]
      .sort((a, b) => (b.userCount ?? 0) - (a.userCount ?? 0))
      .slice(0, 4);
  }

  get maxUsers(): number {
    return Math.max(1, ...this.topSchools.map((s) => s.userCount ?? 0));
  }

  activityIcon(type: string): string {
    const map: Record<string, string> = { add: "+", edit: "✎", warn: "!", del: "×" };
    return map[type] ?? "•";
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
}
