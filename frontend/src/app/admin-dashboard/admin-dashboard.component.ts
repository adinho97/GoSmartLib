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

  mapError = "";
  mapSchoolCount = 0;
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
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.adminMap) {
      this.adminMap.remove();
      this.adminMap = null;
    }
  }

  private initMap(): void {
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
