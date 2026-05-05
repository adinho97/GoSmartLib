import { Component, OnInit, AfterViewInit, OnDestroy, NgZone } from "@angular/core";
import { Router } from "@angular/router";
import * as L from "leaflet";
import { AdminSchoolService } from "../services/admin-school.service";
import { SchoolService } from "../services/school.service";
import { AdminSchoolDashboardItem, SchoolStatus } from "../models/admin-school";

@Component({
  selector: "app-admin-dashboard",
  templateUrl: "./admin-dashboard.component.html",
  styleUrls: ["./admin-dashboard.component.css"],
  standalone: false,
})
export class AdminDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  isLoadingSchools = false;
  schoolError = "";
  schools: AdminSchoolDashboardItem[] = [];

  // Filter + pagination
  searchQuery = "";
  statusFilter = "";
  currentPage = 1;
  readonly pageSize = 12;

  mapError = "";
  mapSchoolCount = 0;
  mapExpanded = false;
  private adminMap: L.Map | null = null;

  constructor(
    private readonly adminSchoolService: AdminSchoolService,
    private readonly schoolService: SchoolService,
    private readonly router: Router,
    private readonly ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    this.loadSchools();
  }

  ngAfterViewInit(): void {
    // map initializes lazily on first expand
  }

  ngOnDestroy(): void {
    if (this.adminMap) {
      this.adminMap.remove();
      this.adminMap = null;
    }
  }

  private initMap(): void {
    if (this.adminMap) return;
    const el = document.getElementById("admin-map");
    if (!el) return;

    this.adminMap = L.map("admin-map", {
      center: [51.2194, 4.4025],
      zoom: 12,
      zoomControl: true,
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "© OpenStreetMap contributors",
    }).addTo(this.adminMap);

    this.adminMap.on("popupopen", (e: any) => {
      const container = e.popup.getElement();
      if (!container) return;
      const btn = container.querySelector(".admin-popup-btn");
      if (btn) {
        btn.addEventListener("click", () => {
          const id = btn.getAttribute("data-id");
          if (id) {
            this.ngZone.run(() => this.router.navigate(["/admin/schools", id]));
          }
        });
      }
    });

    this.schoolService.getAllSchools().subscribe({
      next: (schools) => {
        schools.forEach((school) => {
          if (school.latitude != null && school.longitude != null) {
            const marker = L.circleMarker([school.latitude, school.longitude], {
              radius: 9,
              fillColor: "#871f42",
              color: "#fff",
              weight: 2,
              opacity: 1,
              fillOpacity: 0.9,
            });

            marker.bindPopup(`
              <div class="admin-popup">
                <p class="admin-popup-name">${school.naam}</p>
                <p class="admin-popup-addr">${school.adres ?? ""}</p>
                <button class="admin-popup-btn" data-id="${school.id}">Beheer →</button>
              </div>
            `);

            marker.addTo(this.adminMap!);
            this.mapSchoolCount++;
          }
        });

        setTimeout(() => this.adminMap?.invalidateSize(), 100);
      },
      error: () => {
        this.mapError = "Scholenkaart kon niet worden geladen.";
      },
    });
  }

  get activeCount():  number { return this.schools.filter(s => s.status === "ACTIVE").length; }
  get inactiveCount(): number { return this.schools.filter(s => s.status === "INACTIVE").length; }
  get pendingCount():  number { return this.schools.filter(s => s.status === "PENDING").length; }

  get filteredSchools(): AdminSchoolDashboardItem[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.schools.filter(s => {
      const matchName   = !q || (s.naam ?? s.subdomain).toLowerCase().includes(q);
      const matchStatus = !this.statusFilter || s.status === this.statusFilter;
      return matchName && matchStatus;
    });
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredSchools.length / this.pageSize));
  }

  get pagedSchools(): AdminSchoolDashboardItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredSchools.slice(start, start + this.pageSize);
  }

  onFilterChange(): void {
    this.currentPage = 1;
  }

  prevPage(): void { if (this.currentPage > 1) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages) this.currentPage++; }

  toggleMap(): void {
    this.mapExpanded = !this.mapExpanded;
    if (this.mapExpanded) {
      setTimeout(() => this.initMap(), 0);
    }
  }

  goToSchoolWizard(): void {
    this.router.navigate(["/admin/schools/new"]);
  }

  goToSchoolDetail(id: number): void {
    this.router.navigate(["/admin/schools", id]);
  }

  loadSchools(): void {
    this.isLoadingSchools = true;
    this.schoolError = "";
    this.adminSchoolService.getSchools().subscribe({
      next: (schools) => {
        this.schools = schools;
        this.isLoadingSchools = false;
      },
      error: (error) => {
        this.schoolError = error?.error?.message || "Scholen laden is mislukt. Probeer opnieuw.";
        this.isLoadingSchools = false;
      },
    });
  }

  statusLabel(status: SchoolStatus): string {
    switch (status) {
      case "ACTIVE": return "Actief";
      case "INACTIVE": return "Inactief";
      case "PENDING": return "In afwachting";
    }
  }
}
