import { Component } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Router } from "@angular/router";
import { AdminSchoolService } from "../services/admin-school.service";
import { CreateAdminSchoolResponse } from "../models/admin-school";

interface NominatimResult {
  lat: string;
  lon: string;
  display_name: string;
}

@Component({
  selector: "app-admin-school-wizard",
  templateUrl: "./admin-school-wizard.component.html",
  styleUrls: ["./admin-school-wizard.component.css"],
  standalone: false,
})
export class AdminSchoolWizardComponent {
  step = 1;
  isSubmitting = false;
  errorMessage = "";
  subdomain = "";
  naam = "";
  adres = "";
  latitude: number | null = null;
  longitude: number | null = null;
  isGeocoding = false;
  geocodeError = "";
  geocodedDisplay = "";
  createdSchool: CreateAdminSchoolResponse | null = null;

  constructor(
    private readonly adminSchoolService: AdminSchoolService,
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  get normalizedSubdomain(): string {
    return this.subdomain.trim().toLowerCase();
  }

  get previewUrl(): string {
    return this.normalizedSubdomain
      ? `https://${this.normalizedSubdomain}.smartschool.be`
      : "";
  }

  validateSubdomain(): boolean {
    const value = this.normalizedSubdomain;
    return /^[a-z0-9-]+$/.test(value);
  }

  geocodeAddress(): void {
    const query = this.adres.trim();
    if (!query) {
      return;
    }
    this.isGeocoding = true;
    this.geocodeError = "";
    this.geocodedDisplay = "";
    this.latitude = null;
    this.longitude = null;

    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`;
    this.http.get<NominatimResult[]>(url).subscribe({
      next: (results) => {
        this.isGeocoding = false;
        if (results.length === 0) {
          this.geocodeError = "Adres niet gevonden. Controleer het adres en probeer opnieuw.";
          return;
        }
        this.latitude = parseFloat(results[0].lat);
        this.longitude = parseFloat(results[0].lon);
        this.geocodedDisplay = results[0].display_name;
      },
      error: () => {
        this.isGeocoding = false;
        this.geocodeError = "Geocoding mislukt. Controleer je verbinding.";
      },
    });
  }

  goToStep2(): void {
    this.errorMessage = "";
    if (!this.validateSubdomain()) {
      this.errorMessage =
        "Ongeldig subdomein. Gebruik enkel a-z, 0-9 en een koppelteken.";
      return;
    }
    this.step = 2;
  }

  backToStep1(): void {
    this.errorMessage = "";
    this.step = 1;
  }

  confirmCreate(): void {
    if (this.isSubmitting) {
      return;
    }
    this.errorMessage = "";
    this.isSubmitting = true;

    this.adminSchoolService
      .createSchool({
        subdomain: this.normalizedSubdomain,
        naam: this.naam.trim() || undefined,
        adres: this.adres.trim() || undefined,
        latitude: this.latitude ?? undefined,
        longitude: this.longitude ?? undefined,
      })
      .subscribe({
        next: (created) => {
          this.createdSchool = created;
          this.step = 3;
          this.isSubmitting = false;
        },
        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            "School aanmaken is mislukt. Probeer opnieuw.";
          this.isSubmitting = false;
        },
      });
  }

  goToDashboard(): void {
    this.router.navigate(["/admin/dashboard"]);
  }
}
