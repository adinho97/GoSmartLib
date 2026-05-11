import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { forkJoin } from "rxjs";
import axios from "axios";
import { AdminSchoolService } from "../services/admin-school.service";
import { AuthContextService } from "../services/auth-context.service";
import {
  AdminUserListItem,
  KlasListItem,
  SchoolDetail,
  SchoolStatus,
} from "../models/admin-school";
import { formatUserInfoDisplayName } from "../utils/name-utils";

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

  // Modal for class users
  selectedKlasForPopup: KlasListItem | null = null;
  klasPopupFilter = "";
  klasPopupPage = 1;
  readonly klasPopupPageSize = 5;

  isLoadingDetail = true;
  isLoadingUsers = true;
  isLoadingKlassen = true;
  loadError = "";

  readonly libraryCards = [
    { label: "Boekencatalogus",    sub: "Boeken bekijken & beheren",       route: "/books",             icon: "catalog"    },
    { label: "Boek toevoegen",     sub: "Boek aan catalogus toevoegen",     route: "/add-general",       icon: "add"        },
    { label: "Boek uitlenen",      sub: "Uitlening registreren",            route: "/uitleen",           icon: "loan"       },
    { label: "Actieve uitleningen",sub: "Lopende uitleningen bekijken",     route: "/uitleen-overzicht", icon: "active"     },
    { label: "Uitleenhistoriek",   sub: "Alle voorbije uitleningen",        route: "/uitleen-catalogus", icon: "history"    },
    { label: "Conditieoverzicht",  sub: "Staat van de collectie",           route: "/uitleen-conditie",  icon: "condition"  },
  ];

  // Info edit form
  infoExpanded = false;
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

  toggleInfo(): void {
    this.infoExpanded = !this.infoExpanded;
  }

  // Status toggle
  isTogglingStatus = false;
  statusError = "";

  // Display names — fetched live from Smartschool, never stored
  userDisplayNames: Record<string, string> = {};

  getDisplayName(sub: string): string {
    return this.userDisplayNames[sub] ?? "";
  }

  private enrichUserNames(users: AdminUserListItem[]): void {
    users.forEach(async (user) => {
      if (!user.sub) return;
      // Retrieve the authentication token and user sub from AuthContextService
      const token = this.authContext.getEffectiveBearerToken();
      const userSub = this.authContext.getEffectiveSub();
      try {
        const profile = await axios.get(`/api/users/${encodeURIComponent(user.sub)}/profile`, {
          headers: {
            "X-User-Sub": userSub,
            Authorization: token ? `Bearer ${token}` : "",
          },
        });
        this.userDisplayNames[user.sub] = formatUserInfoDisplayName(profile.data, user.sub);
      } catch {
        // leave blank — sub is already shown in its own column
      }
    });
  }

  // User actions
  togglingUserId: number | null = null;
  promotingUserId: number | null = null;
  userActionError = "";

  // User filter + pagination
  userFilter = "";
  roleFilter = "";
  klasFilter = "";
  activeFilter: "" | "active" | "inactive" = "";
  userPage = 1;
  readonly userPageSize = 5;

  setActiveFilter(value: "" | "active" | "inactive"): void {
    this.activeFilter = value;
    this.onUserFilterChange();
  }

  // Klassen pagination
  klasPage = 1;
  readonly klasPageSize = 5;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly adminSchoolService: AdminSchoolService,
    private readonly authContext: AuthContextService, // Inject AuthContextService
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
        this.enrichUserNames(users);
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

  openKlasUsersPopup(klas: KlasListItem): void {
    this.selectedKlasForPopup = klas;
    this.klasPopupFilter = "";
    this.klasPopupPage = 1;
  }

  closeKlasPopup(): void {
    this.selectedKlasForPopup = null;
  }

  get usersInSelectedKlas(): AdminUserListItem[] {
    if (!this.selectedKlasForPopup) return [];
    return this.users.filter(u => u.klasNaam === this.selectedKlasForPopup?.naam);
  }

  get filteredUsersInKlas(): AdminUserListItem[] {
    const q = this.klasPopupFilter.trim().toLowerCase();
    return this.usersInSelectedKlas.filter(u => {
      const displayName = (this.userDisplayNames[u.sub] ?? "").toLowerCase();
      return !q || displayName.includes(q) || u.sub.toLowerCase().includes(q);
    });
  }

  get klasPopupTotalPages(): number {
    return Math.max(1, Math.ceil(this.filteredUsersInKlas.length / this.klasPopupPageSize));
  }

  get pagedUsersInKlas(): AdminUserListItem[] {
    const start = (this.klasPopupPage - 1) * this.klasPopupPageSize;
    return this.filteredUsersInKlas.slice(start, start + this.klasPopupPageSize);
  }

  onKlasPopupFilterChange(): void {
    this.klasPopupPage = 1;
  }

  prevKlasPopupPage(): void {
    if (this.klasPopupPage > 1) this.klasPopupPage--;
  }

  nextKlasPopupPage(): void {
    if (this.klasPopupPage < this.klasPopupTotalPages) this.klasPopupPage++;
  }

  // User filtering
  get filteredUsers(): AdminUserListItem[] {
    const q = this.userFilter.trim().toLowerCase();
    const r = this.roleFilter;
    const k = this.klasFilter;
    const a = this.activeFilter;
    return this.users.filter((u) => {
      const displayName = (this.userDisplayNames[u.sub] ?? "").toLowerCase();
      const matchesText =
        !q ||
        displayName.includes(q) ||
        u.klasNaam?.toLowerCase().includes(q);
      const matchesRole   = !r || u.role === r;
      const matchesKlas   = !k || u.klasNaam === k;
      const matchesActive = !a || (a === "active" ? u.active : !u.active);
      return matchesText && matchesRole && matchesKlas && matchesActive;
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

  navigateToLibrary(route: string): void {
    if (this.detail) {
      localStorage.setItem("selectedSchoolId", String(this.detail.id));
      localStorage.setItem("adminLibrarySchoolId", String(this.detail.id));
      localStorage.setItem("adminLibrarySchoolName", this.detail.naam || this.detail.subdomain);
    }
    this.router.navigate([route]);
  }

  goBack(): void {
    this.router.navigate(["/admin/dashboard"]);
  }
}
