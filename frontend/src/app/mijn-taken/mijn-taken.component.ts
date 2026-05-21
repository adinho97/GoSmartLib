import { Component, OnInit } from "@angular/core";
import { SchoolService } from "../services/school.service";
import { LoanService, Loan } from "../services/loan.service";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { firstValueFrom } from "rxjs";

@Component({
  selector: "app-mijn-taken",
  templateUrl: "./mijn-taken.component.html",
  styleUrls: ["./mijn-taken.component.css"],
  standalone: false,
})
export class MijnTakenComponent implements OnInit {
  // Properties for the "Leningen verlengen" modal
  extensionModalOpen: boolean = false;
  studentSearchQuery: string = "";
  studentClassFilter: string = ""; // Holds the selected class filter
  availableClasses: string[] = []; // Will be populated from the service
  studentsLoading = false;
  studentsError = "";

  // Define a basic structure for a student
  filteredStudents: { sub: string; displayName: string; klas: string }[] = [];
  allStudents: { sub: string; displayName: string; klas: string }[] = [];
  // loans for the selected student
  studentLoans: Array<{
    id: number | string;
    bookTitel: string;
    dueDate: string;
    tempNewDueDate?: string;
  }> = [];

  loansLoading = false;
  loansError = "";

  extensionSuccessMessage = "";
  private successMessageTimeout: ReturnType<typeof setTimeout> | null = null;

  selectedStudentForExtension: {
    sub: string;
    displayName: string;
    klas: string;
  } | null = null;

  today: string;

  constructor(
    private schoolService: SchoolService,
    private loanService: LoanService,
    private bibbeheerderService: BibbeheerderService,
  ) {
    // Inject the SchoolService
    // Initialize 'today' for the date input's min attribute
    this.today = new Date().toISOString().split("T")[0];
  }

  ngOnInit(): void {
    this.loadAvailableClasses();
  }

  openExtensionModal(): void {
    this.extensionModalOpen = true;
    this.selectedStudentForExtension = null;
    this.studentSearchQuery = "";
    this.studentClassFilter = "";
    this.studentsError = "";
    this.filteredStudents = [];
    this.clearSuccessMessage();

    this.loadAvailableClasses();
  }

  private clearSuccessMessage(): void {
    this.extensionSuccessMessage = "";
    if (this.successMessageTimeout !== null) {
      clearTimeout(this.successMessageTimeout);
      this.successMessageTimeout = null;
    }
  }

  private loadAvailableClasses(): void {
    this.bibbeheerderService.getKlassen().subscribe({
      next: (klassen) => {
        this.availableClasses = klassen.map((k) => k.naam);
      },
      error: (err) => {
        console.error("Failed to load class filters:", err);
        this.availableClasses = [];
      },
    });
  }

  private async loadStudents(): Promise<void> {
    this.studentsLoading = true;
    this.studentsError = "";

    try {
      const users = await firstValueFrom(
        this.bibbeheerderService.getAllUsers(),
      );

      this.allStudents = users.map((u) => ({
        sub: u.sub,
        displayName: u.displayName || u.sub,
        klas: u.klasNaam || "",
      }));
    } catch (err) {
      console.error("Failed to load students for verlengen:", err);
      this.studentsError =
        "Er is iets misgegaan met het laden van de leerlingen.";
    } finally {
      this.studentsLoading = false;
    }
  }

  get hasSearchInput(): boolean {
    return this.studentSearchQuery.trim() !== "" || this.studentClassFilter !== "";
  }

  get isLibrarian(): boolean {
    return localStorage.getItem("role") === "bibbeheerder";
  }

  // Methods for the "Leningen verlengen" modal
  closeExtensionModal(): void {
    this.extensionModalOpen = false;
  }

  async onSearchStudents(): Promise<void> {
    if (!this.hasSearchInput) {
      this.filteredStudents = [];
      return;
    }

    if (this.allStudents.length === 0 && !this.studentsLoading) {
      await this.loadStudents();
    }

    this.filteredStudents = this.allStudents.filter((student) => {
      const matchesSearchQuery = student.displayName
        .toLowerCase()
        .includes(this.studentSearchQuery.toLowerCase());
      const matchesClassFilter =
        this.studentClassFilter === "" ||
        student.klas === this.studentClassFilter;
      return matchesSearchQuery && matchesClassFilter;
    });
  }

  selectStudentForExtension(student: {
    sub: string;
    displayName: string;
    klas: string;
  }): void {
    this.selectedStudentForExtension = student;
    // Fetch loans from backend for this student
    this.studentLoans = [];
    this.loansError = "";
    this.loansLoading = true;
    void this.loanService
      .getActiveLoans(student.sub)
      .then((loans: Loan[]) => {
        this.studentLoans = loans.map((l) => ({
          id: l.id,
          bookTitel: l.bookTitel,
          dueDate: l.dueDate,
        }));
        this.loansLoading = false;
      })
      .catch((err) => {
        console.error("Failed to load loans:", err);
        this.loansError = "Leningen laden mislukt.";
        this.loansLoading = false;
      });
  }

  isOverdue(dueDate: string): boolean {
    return new Date(dueDate) < new Date(this.today);
  }

  confirmExtension(loan: {
    id: string | number;
    bookTitel: string;
    dueDate: string;
    tempNewDueDate?: string;
  }): void {
    if (loan.tempNewDueDate) {
      const newDueDate = loan.tempNewDueDate;
      void this.loanService
        .updateLoanDueDate(Number(loan.id), newDueDate)
        .then(() => {
          loan.dueDate = newDueDate;
          delete loan.tempNewDueDate;
          this.showSuccessMessage(
            `"${loan.bookTitel}" verlengd tot ${this.formatDutchDate(newDueDate)}.`,
          );
        })
        .catch((err) => {
          console.error("Failed to extend loan:", err);
          this.loansError = "Verlengen mislukt. Probeer opnieuw.";
        });
    }
  }

  private showSuccessMessage(message: string): void {
    this.extensionSuccessMessage = message;
    if (this.successMessageTimeout !== null) {
      clearTimeout(this.successMessageTimeout);
    }
    this.successMessageTimeout = setTimeout(() => {
      this.extensionSuccessMessage = "";
      this.successMessageTimeout = null;
    }, 3500);
  }

  private formatDutchDate(iso: string): string {
    const [year, month, day] = iso.split("-");
    return `${day}/${month}/${year}`;
  }
}
