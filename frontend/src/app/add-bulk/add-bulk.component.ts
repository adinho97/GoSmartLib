import { Component } from "@angular/core";
import {
  BookService,
  BulkImportResult,
  BulkImportRowResult,
  BulkImportStatus,
} from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { School } from "../models/school";

@Component({
  selector: "app-add-bulk",
  templateUrl: "./add-bulk.component.html",
  styleUrls: ["./add-bulk.component.css"],
  standalone: false,
})
export class AddBulkComponent {
  readonly statusFilters: Array<"" | BulkImportStatus> = [
    "",
    "ADDED",
    "ALREADY_EXISTS",
    "NOT_FOUND",
    "INVALID_ISBN",
    "ERROR",
  ];

  schools: School[] = [];
  selectedSchoolId: number | null = null;
  selectedFile: File | null = null;
  selectedStatusFilter: "" | BulkImportStatus = "";

  isUploading = false;
  errorMessage = "";
  successMessage = "";
  result: BulkImportResult | null = null;

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {
    void this.loadSchools();
  }

  async loadSchools() {
    try {
      this.schools = await this.schoolService.getSchools();
      const storedSchoolId = this.schoolService.getSelectedSchoolId();
      const hasStoredSchool =
        storedSchoolId !== null &&
        this.schools.some((school) => school.id === storedSchoolId);

      const fallbackSchoolId =
        this.schools.length > 0 ? this.schools[0].id : null;
      this.selectedSchoolId = hasStoredSchool
        ? storedSchoolId
        : fallbackSchoolId;

      if (this.selectedSchoolId !== null) {
        this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
      }
    } catch {
      this.schools = [];
      this.selectedSchoolId = null;
    }
  }

  onSchoolChange(value: string) {
    this.selectedSchoolId = value ? Number(value) : null;
    if (this.selectedSchoolId !== null) {
      this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.selectedFile = file;
    this.selectedStatusFilter = "";
    this.errorMessage = "";
    this.successMessage = "";
    this.result = null;
  }

  async uploadFile() {
    if (!this.selectedFile) {
      this.errorMessage = "Kies eerst een bestand (.csv, .xls, .xlsx).";
      return;
    }

    this.isUploading = true;
    this.errorMessage = "";
    this.successMessage = "";
    this.result = null;
    this.selectedStatusFilter = "";

    try {
      this.result = await this.bookService.importBooksByUpload(
        this.selectedFile,
        this.selectedSchoolId ?? undefined,
      );

      const addedCount = this.getStatusCount("ADDED");
      this.successMessage = `Upload verwerkt. ${addedCount} boeken toegevoegd.`;
    } catch (err: any) {
      this.errorMessage =
        err?.response?.data?.message ||
        "Er ging iets mis bij het verwerken van het bestand.";
    } finally {
      this.isUploading = false;
    }
  }

  getStatusCount(status: BulkImportStatus): number {
    if (!this.result) {
      return 0;
    }
    return this.result.results.filter((row) => row.status === status).length;
  }

  get unresolvedRows(): BulkImportRowResult[] {
    if (!this.result) {
      return [];
    }
    return this.result.results.filter(
      (row) =>
        row.status === "NOT_FOUND" ||
        row.status === "INVALID_ISBN" ||
        row.status === "ERROR",
    );
  }

  get filteredRows(): BulkImportRowResult[] {
    if (!this.result) {
      return [];
    }

    if (!this.selectedStatusFilter) {
      return this.result.results;
    }

    return this.result.results.filter(
      (row) => row.status === this.selectedStatusFilter,
    );
  }

  downloadUnresolvedCsv() {
    const unresolved = this.unresolvedRows;
    if (unresolved.length === 0) {
      return;
    }

    const header = "isbn,status,message";
    const lines = unresolved.map((row) => {
      const safeIsbn = this.escapeCsv(row.isbn || "");
      const safeStatus = this.escapeCsv(row.status);
      const safeMessage = this.escapeCsv(row.message || "");
      return `${safeIsbn},${safeStatus},${safeMessage}`;
    });

    const blob = new Blob([`${header}\n${lines.join("\n")}`], {
      type: "text/csv;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = url;
    link.download = "isbn-unresolved.csv";
    link.click();

    URL.revokeObjectURL(url);
  }

  private escapeCsv(value: string): string {
    const escaped = value.replace(/"/g, '""');
    return `"${escaped}"`;
  }

}
