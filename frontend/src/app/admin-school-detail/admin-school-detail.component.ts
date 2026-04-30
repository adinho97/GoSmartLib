import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { forkJoin } from "rxjs";
import { AdminSchoolService } from "../services/admin-school.service";
import {
  AdminUserListItem,
  KlasListItem,
  SchoolDetail,
  SchoolStatus,
} from "../models/admin-school";

interface NominatimResult {
  lat: string;
  lon: string;
  display_name: string;
}

@Component({
  selector: "app-admin-school-detail",
  templateUrl: "./admin-school-detail.component.html",
  styleUrls: ["./admin-school-detail.component.css"],
  standalone: false,
})
export class AdminSchoolDetailComponent implements OnInit {
  schoolId!: number;
  detail: SchoolDetail | null = null;
  users: AdminUserListItem[] = [];
  klassen: KlasListItem[] = [];

  isLoadingDetail = true;
  isLoadingUsers = true;
  isLoadingKlassen = true;
  loadError = "";

  // Info edit form
  editNaam = "";
  editAdres = "";
  editLat: number | null = null;
  editLng: number | null = null;
  isSaving = false;
  saveSuccess = false;
  saveError = "";
  isGeocoding = false;
  geocodeError = "";
  geocodedDisplay = "";

  // Status toggle
  isTogglingStatus = false;
  statusError = "";

  // User actions
  togglingUserId: number | null = null;
  promotingUserId: number | null = null;
  userActionError = "";

  // User filter + pagination
  userFilter = "";
  roleFilter = "";
  userPage = 1;
  readonly userPageSize = 5;

  // Klassen pagination
  klasPage = 1;
  readonly klasPageSize = 5;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly adminSchoolService: AdminSchoolService,
  ) {}

  ngOnInit(): void {
    this.schoolId = Number(this.route.snapshot.paramMap.get("id"));
    this.loadAll();
  }

  loadAll(): void {
    this.isLoadingDetail = true;
    this.isLoadingUsers = true;
    this.isLoadingKlassen = true;
    this.loadError = "";

    forkJoin({
      detail: this.adminSchoolService.getSchoolDetail(this.schoolId),
      users: this.adminSchoolService.getSchoolUsers(this.schoolId),
      klassen: this.adminSchoolService.getSchoolKlassen(this.schoolId),
    }).subscribe({
      next: ({ detail, users, klassen }) => {
        this.detail = detail;
        this.users = users;
        this.klassen = klassen;
        this.resetForm();
        this.isLoadingDetail = false;
        this.isLoadingUsers = false;
        this.isLoadingKlassen = false;
      },
      error: (err) => {
        this.loadError = err?.error?.message || "Gegevens laden mislukt.";
        this.isLoadingDetail = false;
        this.isLoadingUsers = false;
        this.isLoadingKlassen = false;
      },
    });
  }

  resetForm(): void {
    if (!this.detail) return;
    this.editNaam = this.detail.naam ?? "";
    this.editAdres = this.detail.adres ?? "";
    this.editLat = this.detail.latitude;
    this.editLng = this.detail.longitude;
    this.geocodedDisplay = "";
    this.geocodeError = "";
    this.saveError = "";
    this.saveSuccess = false;
  }

  geocodeAddress(): void {
    const query = this.editAdres.trim();
    if (!query) return;
    this.isGeocoding = true;
    this.geocodeError = "";
    this.geocodedDisplay = "";
    this.editLat = null;
    this.editLng = null;

    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`;
    this.http.get<NominatimResult[]>(url).subscribe({
      next: (results) => {
        this.isGeocoding = false;
        if (results.length === 0) {
          this.geocodeError = "Adres niet gevonden.";
          return;
        }
        this.editLat = parseFloat(results[0].lat);
        this.editLng = parseFloat(results[0].lon);
        this.geocodedDisplay = results[0].display_name;
      },
      error: () => {
        this.isGeocoding = false;
        this.geocodeError = "Geocoding mislukt. Controleer je verbinding.";
      },
    });
  }

  saveInfo(): void {
    if (this.isSaving) return;
    this.isSaving = true;
    this.saveError = "";
    this.saveSuccess = false;

    this.adminSchoolService.updateSchoolInfo(this.schoolId, {
      naam: this.editNaam.trim() || null,
      adres: this.editAdres.trim() || null,
      latitude: this.editLat,
      longitude: this.editLng,
    }).subscribe({
      next: (updated) => {
        this.detail = updated;
        this.isSaving = false;
        this.saveSuccess = true;
        setTimeout(() => (this.saveSuccess = false), 3000);
      },
      error: (err) => {
        this.saveError = err?.error?.message || "Opslaan mislukt.";
        this.isSaving = false;
      },
    });
  }

  toggleStatus(): void {
    if (!this.detail || this.isTogglingStatus) return;
    const next: Exclude<SchoolStatus, "PENDING"> =
      this.detail.status === "INACTIVE" ? "ACTIVE" : "INACTIVE";
    this.isTogglingStatus = true;
    this.statusError = "";

    this.adminSchoolService.updateSchoolStatus(this.detail.id, next).subscribe({
      next: (updated) => {
        this.detail = updated;
        this.isTogglingStatus = false;
      },
      error: (err) => {
        this.statusError = err?.error?.message || "Status wijzigen mislukt.";
        this.isTogglingStatus = false;
      },
    });
  }

  toggleUserRole(user: AdminUserListItem): void {
    if (this.promotingUserId !== null) return;
    this.promotingUserId = user.id;
    this.userActionError = "";
    const newRole = user.role === "leerkracht" ? "bibbeheerder" : "leerkracht";

    this.adminSchoolService.setUserRole(this.schoolId, user.id, newRole).subscribe({
      next: (updated) => {
        this.users = this.users.map((u) => (u.id === updated.id ? updated : u));
        this.promotingUserId = null;
      },
      error: (err) => {
        this.userActionError = err?.error?.message || "Rol wijzigen mislukt.";
        this.promotingUserId = null;
      },
    });
  }

  toggleUserActive(user: AdminUserListItem): void {
    if (this.togglingUserId !== null) return;
    this.togglingUserId = user.id;
    this.userActionError = "";

    this.adminSchoolService.toggleUserActive(this.schoolId, user.id).subscribe({
      next: (updated) => {
        this.users = this.users.map((u) => (u.id === updated.id ? updated : u));
        this.togglingUserId = null;
      },
      error: (err) => {
        this.userActionError = err?.error?.message || "Actie mislukt.";
        this.togglingUserId = null;
      },
    });
  }

  // User filtering
  get filteredUsers(): AdminUserListItem[] {
    const q = this.userFilter.trim().toLowerCase();
    const r = this.roleFilter;
    return this.users.filter((u) => {
      const matchesText =
        !q ||
        u.sub?.toLowerCase().includes(q) ||
        u.klasNaam?.toLowerCase().includes(q);
      const matchesRole = !r || u.role === r;
      return matchesText && matchesRole;
    });
  }

  onUserFilterChange(): void {
    this.userPage = 1;
  }

  // User pagination
  get userTotalPages(): number {
    return Math.max(1, Math.ceil(this.filteredUsers.length / this.userPageSize));
  }

  get pagedUsers(): AdminUserListItem[] {
    const start = (this.userPage - 1) * this.userPageSize;
    return this.filteredUsers.slice(start, start + this.userPageSize);
  }

  prevUserPage(): void {
    if (this.userPage > 1) this.userPage--;
  }

  nextUserPage(): void {
    if (this.userPage < this.userTotalPages) this.userPage++;
  }

  // Klassen pagination
  get klasTotalPages(): number {
    return Math.max(1, Math.ceil(this.klassen.length / this.klasPageSize));
  }

  get pagedKlassen(): KlasListItem[] {
    const start = (this.klasPage - 1) * this.klasPageSize;
    return this.klassen.slice(start, start + this.klasPageSize);
  }

  prevKlasPage(): void {
    if (this.klasPage > 1) this.klasPage--;
  }

  nextKlasPage(): void {
    if (this.klasPage < this.klasTotalPages) this.klasPage++;
  }

  roleLabel(role: string): string {
    switch (role) {
      case "leerling": return "Leerling";
      case "leerkracht": return "Leerkracht";
      case "bibbeheerder": return "Bibbeheerder";
      default: return role;
    }
  }

  statusLabel(status: SchoolStatus): string {
    switch (status) {
      case "ACTIVE": return "Actief";
      case "INACTIVE": return "Inactief";
      case "PENDING": return "In afwachting";
    }
  }

  goBack(): void {
    this.router.navigate(["/admin/dashboard"]);
  }
}
