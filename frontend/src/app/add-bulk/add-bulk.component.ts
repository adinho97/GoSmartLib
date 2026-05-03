import { Component } from "@angular/core";
import {
  BookService,
  BulkImportResult,
  BulkImportRowResult,
  BulkImportStatus,
} from "../services/book.service";
import { SchoolService } from "../services/school.service";
import * as XLSX from 'xlsx';

@Component({
  selector: "app-add-bulk",
  templateUrl: "./add-bulk.component.html",
  styleUrls: ["./add-bulk.component.css"],
  standalone: false,
})
export class AddBulkComponent {
  readonly pageSize = 5;
  readonly statusFilters: Array<"" | BulkImportStatus> = [
    "",
    "ADDED",
    "NOT_FOUND",
    "INVALID_ISBN",
    "ERROR",
  ];

  selectedSchoolId: number | null = null;
  selectedFile: File | null = null;
  selectedStatusFilter: "" | BulkImportStatus = "";
  currentPage = 1;

  isUploading = false;
  errorMessage = "";
  successMessage = "";
  result: BulkImportResult | null = null;

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {
    void this.loadUserSchool();
  }

  private async loadUserSchool() {
    await this.schoolService.selectUserDefaultSchool();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.selectedFile = file;
    this.selectedStatusFilter = "";
    this.currentPage = 1;
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
    this.currentPage = 1;

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

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  get currentPageSafe(): number {
    return Math.min(this.currentPage, this.totalPages);
  }

  get pagedRows(): BulkImportRowResult[] {
    const start = (this.currentPageSafe - 1) * this.pageSize;
    return this.filteredRows.slice(start, start + this.pageSize);
  }

  get canGoPrevious(): boolean {
    return this.currentPageSafe > 1;
  }

  get canGoNext(): boolean {
    return this.currentPageSafe < this.totalPages;
  }

  onStatusFilterChange(value: "" | BulkImportStatus) {
    this.selectedStatusFilter = value;
    this.currentPage = 1;
  }

  goToPreviousPage() {
    if (!this.canGoPrevious) {
      return;
    }
    this.currentPage = this.currentPageSafe - 1;
  }

  goToNextPage() {
    if (!this.canGoNext) {
      return;
    }
    this.currentPage = this.currentPageSafe + 1;
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

  downloadImportTemplate(): void {
  const headers = [['ISBN', 'Aantal']];

  const worksheet = XLSX.utils.aoa_to_sheet(headers);
  worksheet['!cols'] = [
    { wch: 20 }, 
    { wch: 10 }, 
  ];

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Template');

  XLSX.writeFile(workbook, 'isbn-import-template.xlsx');
}

}
