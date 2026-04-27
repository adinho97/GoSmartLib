import { Component, OnInit, AfterViewInit, OnDestroy } from "@angular/core";
import * as L from "leaflet";
import { MaptilerLayer, MapStyle } from "@maptiler/leaflet-maptilersdk";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

@Component({
  selector: "app-map-screen",
  templateUrl: "./map-screen.component.html",
  styleUrls: ["./map-screen.component.css"],
})
export class MapScreenComponent implements OnInit, AfterViewInit, OnDestroy {
  map!: L.Map;
  schools: School[] = [];
  selectedSchool: School | null = null;
  isLoading: boolean = false;
  private schoolMarkers: L.Marker[] = [];

  // Default coordinates for Belgium (roughly center)
  private defaultLat = 50.8503;
  private defaultLng = 4.3517;
  private defaultZoom = 8;

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

    new MaptilerLayer({
      apiKey: "YOUR_MAPTILER_API_KEY_HERE",
      style: MapStyle.BASIC,
    }).addTo(this.map);
  }

  private loadSchools(): void {
    this.isLoading = true;
    this.schoolService.getAllSchools().subscribe({
      next: (schools) => {
        this.schools = schools;
        this.addSchoolMarkers();
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Failed to load schools:", err);
        this.isLoading = false;
        // Optionally, display an error message to the user
      },
    });
  }

  private addSchoolMarkers(): void {
    // Clear existing markers before adding new ones
    this.schoolMarkers.forEach((marker) => marker.remove());
    this.schoolMarkers = [];

    this.schools.forEach((school) => {
      if (school.latitude !== undefined && school.longitude !== undefined) {
        const marker = L.marker([
          school.latitude as number,
          school.longitude as number,
        ])
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
      const marker = this.schoolMarkers.find((m) => {
        const latLng = m.getLatLng();
        return (
          latLng.lat === school.latitude && latLng.lng === school.longitude
        );
      });
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
