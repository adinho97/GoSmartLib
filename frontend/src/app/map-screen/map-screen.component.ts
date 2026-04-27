import { Component, OnInit, AfterViewInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import * as L from "leaflet";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

@Component({
  selector: "app-map-screen",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./map-screen.component.html",
  styleUrls: ["./map-screen.component.css"],
})
export class MapScreenComponent implements OnInit, AfterViewInit, OnDestroy {
  map!: L.Map;
  schools: School[] = [];
  selectedSchool: School | null = null;
  isLoading: boolean = false;

  // Search and Pagination
  searchQuery: string = "";
  currentPage: number = 0;
  pageSize: number = 10;
  totalSchools: number = 0;

  private schoolMarkers: L.Marker[] = [];

  // Default coordinates for Belgium (roughly center)
  private defaultLat = 50.8503;
  private defaultLng = 4.3517;
  private defaultZoom = 8;

  // Define a custom icon to avoid the common "missing marker" issue in Angular builds
  private readonly defaultIcon = L.icon({
    iconUrl:
      "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
    iconRetinaUrl:
      "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
    shadowUrl:
      "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
  });

  constructor(private schoolService: SchoolService) {}

  ngOnInit(): void {
    // Any initial data loading that doesn't depend on the DOM
  }

  ngAfterViewInit(): void {
    this.initMap();
    this.loadSchools();
  }

  private initMap(): void {
    if (this.map) {
      this.map.remove(); // Remove existing map if it was already initialized
    }
    this.map = L.map("map", {
      center: [this.defaultLat, this.defaultLng],
      zoom: this.defaultZoom,
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.map);

    // Ensure the map container size is correctly calculated after the view settles
    setTimeout(() => {
      this.map.invalidateSize();
    }, 100);
  }

  loadSchools(): void {
    this.isLoading = true;
    this.schoolService
      .getPagedSchools(this.currentPage, this.pageSize, this.searchQuery)
      .subscribe({
        next: (data) => {
          this.schools = data.items;
          this.totalSchools = data.total;
          this.addSchoolMarkers();
          this.isLoading = false;
        },
        error: (err) => {
          console.error("Failed to load schools:", err);
          this.isLoading = false;
        },
      });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadSchools();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadSchools();
  }

  get totalPages(): number {
    return Math.ceil(this.totalSchools / this.pageSize);
  }

  /**
   * Generates an array of page numbers for the pagination UI.
   */
  getPageNumbers(): number[] {
    // For simplicity, return all page numbers. For very large numbers of pages,
    // a more sophisticated approach (e.g., showing a range around the current page) might be needed.
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  private addSchoolMarkers(): void {
    // Clear existing markers before adding new ones
    this.schoolMarkers.forEach((marker) => marker.remove());
    this.schoolMarkers = [];

    this.schools.forEach((school) => {
      if (school.latitude !== undefined && school.longitude !== undefined) {
        const marker = L.marker(
          [school.latitude as number, school.longitude as number],
          { icon: this.defaultIcon },
        )
          .addTo(this.map)
          .bindPopup(`<b>${school.naam}</b><br>${school.adres}`);

        marker.on("click", () => {
          this.selectedSchool = school;
          this.map.flyTo(
            [school.latitude as number, school.longitude as number],
            15,
          ); // Zoom in on click
        });

        this.schoolMarkers.push(marker);
      }
    });
  }

  zoomToSchool(school: School): void {
    this.selectedSchool = school;
    if (
      this.map &&
      school.latitude !== undefined &&
      school.longitude !== undefined
    ) {
      this.map.flyTo([school.latitude, school.longitude], 15); // Zoom to level 15
      // Find the corresponding marker and open its popup
      const marker = this.schoolMarkers.find(
        (m) =>
          this.map &&
          school.latitude !== undefined &&
          school.longitude !== undefined &&
          m.getLatLng().equals(L.latLng(school.latitude, school.longitude)),
      );
      if (marker) {
        marker.openPopup();
      }
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }
}
