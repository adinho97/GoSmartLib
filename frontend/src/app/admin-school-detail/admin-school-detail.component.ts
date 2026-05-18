import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { firstValueFrom, forkJoin, from } from "rxjs";
import { AdminSchoolService } from "../services/admin-school.service";
import { BookService } from "../services/book.service";
import {
  AdminUserListItem,
  KlasListItem,
  SchoolDetail,
  SchoolStatus,
} from "../models/admin-school";
import { formatUserInfoDisplayName } from "../utils/name-utils";

type PaginationItem = number | "...";

interface SpotlightBook {
  bookId: number;
  titel: string;
  auteur: string;
  cover: string;
}

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
  leeslijsten: any[] = [];

  // Modal for class users
  selectedKlasForPopup: KlasListItem | null = null;
  klasPopupFilter = "";
  klasPopupPage = 1;
  readonly klasPopupPageSize = 5;

  isLoadingDetail = true;
  isLoadingUsers = true;
  isLoadingKlassen = true;
  isLoadingLeeslijsten = true;
  loadError = "";

  readonly libraryCards = [
    {
      label: "Boekencatalogus",
      sub: "Boeken bekijken & beheren",
      route: "/books",
      icon: "catalog",
    },
    {
      label: "Boek toevoegen",
      sub: "Boek aan catalogus toevoegen",
      route: "/add-general",
      icon: "add",
    },
    {
      label: "Boek uitlenen",
      sub: "Uitlening registreren",
      route: "/uitleen",
      icon: "loan",
    },
    {
      label: "Boek terugbrengen",
      sub: "Boek inname registreren",
      route: "/boek-terugbrengen",
      icon: "return",
    },
    {
      label: "Actieve uitleningen",
      sub: "Lopende uitleningen bekijken",
      route: "/uitleen-overzicht",
      icon: "active",
    },
    {
      label: "Uitleenhistoriek",
      sub: "Alle voorbije uitleningen",
      route: "/uitleen-catalogus",
      icon: "history",
    },
    {
      label: "Conditieoverzicht",
      sub: "Staat van de collectie",
      route: "/uitleen-conditie",
      icon: "condition",
    },
    {
      label: "Klasleeslijsten",
      sub: "Leeslijsten beheren",
      route: "/klasleeslijst-beheer",
      icon: "list",
    },
    {
      label: "FAQ & Inhoud",
      sub: "Inhoud & veelgestelde vragen",
      route: "/info",
      icon: "faq",
    },
    {
      label: "Schoolstatistieken",
      sub: "School-brede cijfers",
      route: "/statistieken/school",
      icon: "stats",
    },
  ];

  // Spotlight management
  spotlight: { maand: SpotlightBook | null; thema: SpotlightBook | null } = { maand: null, thema: null };
  spotlightLoading = false;
  spotlightSaving: 'MAAND' | 'THEMA' | null = null;
  spotlightClearing: 'MAAND' | 'THEMA' | null = null;
  spotlightError = "";
  spotlightSuccess = "";
  maandBookIdInput: number | null = null;
  themaBookIdInput: number | null = null;

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

  // Delete school
  showDeleteConfirmation = false;
  isDeletingSchool = false;
  deleteError = "";

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
  private readonly userNameCache = new Map<string, string>();

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
    private readonly bookService: BookService,
  ) {}

  ngOnInit(): void {
    this.schoolId = Number(this.route.snapshot.paramMap.get("id"));
    this.loadAll();
    this.loadSpotlights();
  }

  loadAll(): void {
    this.isLoadingDetail = true;
    this.isLoadingUsers = true;
    this.isLoadingKlassen = true;
    this.isLoadingLeeslijsten = true;
    this.loadError = "";

    forkJoin({
      detail: this.adminSchoolService.getSchoolDetail(this.schoolId),
      users: this.adminSchoolService.getSchoolUsers(this.schoolId),
      klassen: this.adminSchoolService.getSchoolKlassen(this.schoolId),
      leeslijsten: from(this.bookService.getLeeslisten(this.schoolId)),
    }).subscribe({
      next: ({ detail, users, klassen, leeslijsten }) => {
        this.detail = detail;
        this.users = users;
        this.klassen = klassen;
        this.leeslijsten = leeslijsten;
        this.leeslijsten = Array.isArray(leeslijsten) ? leeslijsten : (leeslijsten as any).data || [];
        void this.enrichUserNames(users);

        this.resetForm();
        this.isLoadingDetail = false;
        this.isLoadingUsers = false;
        this.isLoadingKlassen = false;
        this.isLoadingLeeslijsten = false;
      },
      error: (err) => {
        this.loadError = err?.error?.message || "Gegevens laden mislukt.";
        this.isLoadingDetail = false;
        this.isLoadingUsers = false;
        this.isLoadingKlassen = false;
        this.isLoadingLeeslijsten = false;
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

    this.adminSchoolService
      .updateSchoolInfo(this.schoolId, {
        naam: this.editNaam.trim() || null,
        adres: this.editAdres.trim() || null,
        latitude: this.editLat,
        longitude: this.editLng,
      })
      .subscribe({
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

  openDeleteConfirmation(): void {
    this.showDeleteConfirmation = true;
    this.deleteError = "";
  }

  cancelDelete(): void {
    this.showDeleteConfirmation = false;
    this.deleteError = "";
  }

  confirmDelete(): void {
    if (!this.detail || this.isDeletingSchool) return;
    this.isDeletingSchool = true;
    this.deleteError = "";

    this.adminSchoolService.deleteSchool(this.detail.id).subscribe({
      next: () => {
        // School deleted successfully, navigate back to dashboard
        this.router.navigate(["/admin/dashboard"]);
      },
      error: (err) => {
        this.deleteError = err?.error?.message || "School verwijderen mislukt.";
        this.isDeletingSchool = false;
      },
    });
  }

  toggleUserRole(user: AdminUserListItem): void {
    if (this.promotingUserId !== null) return;
    this.promotingUserId = user.id;
    this.userActionError = "";
    const newRole = user.role === "leerkracht" ? "bibbeheerder" : "leerkracht";

    this.adminSchoolService
      .setUserRole(this.schoolId, user.id, newRole)
      .subscribe({
        next: (updated) => {
          this.users = this.users.map((u) =>
            u.id === updated.id
              ? { ...updated, displayName: u.displayName }
              : u,
          );
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
        this.users = this.users.map((u) =>
          u.id === updated.id ? { ...updated, displayName: u.displayName } : u,
        );
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
    return this.users.filter(
      (u) => u.klasNaam === this.selectedKlasForPopup?.naam,
    );
  }

  get filteredUsersInKlas(): AdminUserListItem[] {
    const q = this.klasPopupFilter.trim().toLowerCase();
    return this.usersInSelectedKlas.filter((u) => {
      const displayName = (u.displayName ?? "").toLowerCase();
      return !q || displayName.includes(q) || u.sub.toLowerCase().includes(q);
    });
  }

  get klasPopupTotalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredUsersInKlas.length / this.klasPopupPageSize),
    );
  }

  get visibleKlasPopupPages(): PaginationItem[] {
    return this.buildVisiblePages(this.klasPopupTotalPages, this.klasPopupPage);
  }

  get pagedUsersInKlas(): AdminUserListItem[] {
    const start = (this.klasPopupPage - 1) * this.klasPopupPageSize;
    return this.filteredUsersInKlas.slice(
      start,
      start + this.klasPopupPageSize,
    );
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

  goToUserPage(page: number | string): void {
    if (typeof page === "number" && page >= 1 && page <= this.userTotalPages) {
      this.userPage = page;
    }
  }

  goToKlasPage(page: number | string): void {
    if (typeof page === "number" && page >= 1 && page <= this.klasTotalPages) {
      this.klasPage = page;
    }
  }

  goToKlasPopupPage(page: number | string): void {
    if (
      typeof page === "number" &&
      page >= 1 &&
      page <= this.klasPopupTotalPages
    ) {
      this.klasPopupPage = page;
    }
  }

  // User filtering
  get filteredUsers(): AdminUserListItem[] {
    const q = this.userFilter.trim().toLowerCase();
    const r = this.roleFilter;
    const k = this.klasFilter;
    const a = this.activeFilter;
    return this.users.filter((u) => {
      const displayName = (u.displayName ?? "").toLowerCase();
      const matchesText =
        !q || displayName.includes(q) || u.klasNaam?.toLowerCase().includes(q);
      const matchesRole = !r || u.role === r;
      const matchesKlas = !k || u.klasNaam === k;
      const matchesActive = !a || (a === "active" ? u.active : !u.active);
      return matchesText && matchesRole && matchesKlas && matchesActive;
    });
  }

  onUserFilterChange(): void {
    this.userPage = 1;
  }

  // User pagination
  get userTotalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredUsers.length / this.userPageSize),
    );
  }

  get visibleUserPages(): PaginationItem[] {
    return this.buildVisiblePages(this.userTotalPages, this.userPage);
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

  get visibleKlasPages(): PaginationItem[] {
    return this.buildVisiblePages(this.klasTotalPages, this.klasPage);
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
      case "leerling":
        return "Leerling";
      case "leerkracht":
        return "Leerkracht";
      case "bibbeheerder":
        return "Bibbeheerder";
      default:
        return role;
    }
  }

  private async enrichUserNames(users: AdminUserListItem[]): Promise<void> {
    const enriched = await Promise.all(
      users.map(async (user) => {
        if (user.displayName?.trim()) {
          return user;
        }
        const displayName = await this.getDisplayNameForSub(user.sub);
        return { ...user, displayName };
      }),
    );

    this.users = enriched;
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    if (this.userNameCache.has(sub)) {
      return this.userNameCache.get(sub)!;
    }

    try {
      const profile = await firstValueFrom(
        this.http.get<any>(`/api/users/${encodeURIComponent(sub)}/profile`),
      );
      const displayName = formatUserInfoDisplayName(profile, sub);
      this.userNameCache.set(sub, displayName);
      return displayName;
    } catch {
      return sub;
    }
  }

  private buildVisiblePages(
    totalPages: number,
    currentPage: number,
  ): PaginationItem[] {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    const candidates = new Set<number>([
      1,
      2,
      totalPages - 1,
      totalPages,
      currentPage - 1,
      currentPage,
      currentPage + 1,
    ]);

    const pages = Array.from(candidates)
      .filter((page) => page >= 1 && page <= totalPages)
      .sort((left, right) => left - right);

    const result: PaginationItem[] = [];
    for (let index = 0; index < pages.length; index++) {
      const page = pages[index];
      if (index > 0) {
        const previousPage = pages[index - 1];
        if (page - previousPage > 1) {
          result.push("...");
        }
      }
      result.push(page);
    }

    return result;
  }

  statusLabel(status: SchoolStatus): string {
    switch (status) {
      case "ACTIVE":
        return "Actief";
      case "INACTIVE":
        return "Inactief";
      case "PENDING":
        return "In afwachting";
    }
  }

  navigateToLibrary(route: string): void {
    if (this.detail) {
      localStorage.setItem("selectedSchoolId", String(this.detail.id));
      localStorage.setItem("adminLibrarySchoolId", String(this.detail.id));
      localStorage.setItem(
        "adminLibrarySchoolName",
        this.detail.naam || this.detail.subdomain,
      );
    }
    this.router.navigate([route]);
  }

  loadSpotlights(): void {
    this.spotlightLoading = true;
    this.http.get<{ maand: SpotlightBook | null; thema: SpotlightBook | null }>(
      `/api/spotlight/${this.schoolId}`
    ).subscribe({
      next: (data) => { this.spotlight = data; this.spotlightLoading = false; },
      error: () => { this.spotlightLoading = false; },
    });
  }

  setSpotlight(type: 'MAAND' | 'THEMA'): void {
    const bookId = type === 'MAAND' ? this.maandBookIdInput : this.themaBookIdInput;
    if (!bookId) return;
    this.spotlightSaving = type;
    this.spotlightError = "";
    this.spotlightSuccess = "";
    this.http.put<SpotlightBook>(`/api/spotlight/${this.schoolId}/${type}`, { bookId }).subscribe({
      next: (book) => {
        if (type === 'MAAND') { this.spotlight.maand = book; this.maandBookIdInput = null; }
        else { this.spotlight.thema = book; this.themaBookIdInput = null; }
        this.spotlightSaving = null;
        this.spotlightSuccess = "Opgeslagen.";
        setTimeout(() => (this.spotlightSuccess = ""), 3000);
      },
      error: (err) => {
        this.spotlightError = err?.error?.message || "Opslaan mislukt. Controleer het boek-ID.";
        this.spotlightSaving = null;
      },
    });
  }

  clearSpotlight(type: 'MAAND' | 'THEMA'): void {
    this.spotlightClearing = type;
    this.spotlightError = "";
    this.spotlightSuccess = "";
    this.http.delete(`/api/spotlight/${this.schoolId}/${type}`).subscribe({
      next: () => {
        if (type === 'MAAND') this.spotlight.maand = null;
        else this.spotlight.thema = null;
        this.spotlightClearing = null;
        this.spotlightSuccess = "Gewist.";
        setTimeout(() => (this.spotlightSuccess = ""), 3000);
      },
      error: (err) => {
        this.spotlightError = err?.error?.message || "Wissen mislukt.";
        this.spotlightClearing = null;
      },
    });
  }

  goBack(): void {
    this.router.navigate(["/admin/dashboard"]);
  }
}
