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
  smartschoolUrl = "";
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

  get extractedSubdomain(): string {
    return this.extractSubdomainFromUrl(this.smartschoolUrl) || "";
  }

  get previewUrl(): string {
    return this.extractedSubdomain
      ? `https://${this.extractedSubdomain}.smartschool.be`
      : "";
  }

  private extractSubdomainFromUrl(rawUrl: string): string | null {
    const trimmed = rawUrl.trim();
    if (!trimmed) return null;

    const withScheme = /^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed)
      ? trimmed
      : `https://${trimmed}`;

    let hostname = "";
    try {
      hostname = new URL(withScheme).hostname.toLowerCase();
    } catch {
      return null;
    }

    hostname = hostname.replace(/^www\./, "");
    const suffix = ".smartschool.be";
    if (!hostname.endsWith(suffix)) return null;

    const subdomain = hostname.slice(0, -suffix.length);
    if (!subdomain || !/^[a-z0-9-]+$/.test(subdomain)) return null;

    return subdomain;
  }

  validateSmartschoolUrl(): boolean {
    return !!this.extractSubdomainFromUrl(this.smartschoolUrl);
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
    if (!this.naam.trim()) {
      this.errorMessage = "Schoolnaam is verplicht.";
      return;
    }
    if (!this.adres.trim()) {
      this.errorMessage = "Adres is verplicht.";
      return;
    }
    if (!this.validateSmartschoolUrl()) {
      this.errorMessage =
        "Vul een geldige Smartschool URL in (bv. https://aphogeschool.smartschool.be).";
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
    if (!this.naam.trim()) {
      this.errorMessage = "Schoolnaam is verplicht.";
      return;
    }
    if (!this.adres.trim()) {
      this.errorMessage = "Adres is verplicht.";
      return;
    }
    if (!this.validateSmartschoolUrl()) {
      this.errorMessage =
        "Vul een geldige Smartschool URL in (bv. https://aphogeschool.smartschool.be).";
      return;
    }
    this.isSubmitting = true;

    this.adminSchoolService
      .createSchool({
        subdomain: this.extractedSubdomain,
        naam: this.naam.trim(),
        adres: this.adres.trim(),
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
