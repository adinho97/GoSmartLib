import { Component, OnInit } from "@angular/core";
import { LoanService, Loan } from "../services/loan.service";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { AdminSchoolService } from "../services/admin-school.service";
import { AuthContextService } from "../services/auth-context.service";
import { SchoolService } from "../services/school.service";
import { Observable } from "rxjs";
import { firstValueFrom } from "rxjs";
import {
  AdminUserListItem,
  KlasListItem,
} from "../models/admin-school";

type Student = { sub: string; displayName: string; klas: string };

@Component({
  selector: "app-ontleningen-verlengen",
  templateUrl: "./ontleningen-verlengen.component.html",
  styleUrls: ["./ontleningen-verlengen.component.css"],
  standalone: false,
})
export class OntleningenVerlengenComponent implements OnInit {
  studentSearchQuery = "";
  studentClassFilter = "";
  availableClasses: string[] = [];
  studentsLoading = false;
  studentsError = "";

  filteredStudents: Student[] = [];
  allStudents: Student[] = [];

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

  selectedStudentForExtension: Student | null = null;

  today: string;

  constructor(
    private loanService: LoanService,
    private bibbeheerderService: BibbeheerderService,
    private adminSchoolService: AdminSchoolService,
    private authContext: AuthContextService,
    private schoolService: SchoolService,
  ) {
    this.today = new Date().toISOString().split("T")[0];
  }

  ngOnInit(): void {
    this.loadAvailableClasses();
  }

  /**
   * Super-admins browse a specific school via the admin endpoints (no own
   * `sub`/role), so route through AdminSchoolService when in admin mode.
   * Regular bibbeheerders use the school-scoped bibbeheerder endpoints.
   */
  private klassenSource(): Observable<KlasListItem[]> {
    if (this.authContext.isAdminMode()) {
      const schoolId = this.schoolService.getSelectedSchoolId();
      if (schoolId) return this.adminSchoolService.getSchoolKlassen(schoolId);
    }
    return this.bibbeheerderService.getKlassen();
  }

  private usersSource(): Observable<AdminUserListItem[]> {
    if (this.authContext.isAdminMode()) {
      const schoolId = this.schoolService.getSelectedSchoolId();
      if (schoolId) return this.adminSchoolService.getSchoolUsers(schoolId);
    }
    return this.bibbeheerderService.getAllUsers();
  }

  private loadAvailableClasses(): void {
    this.klassenSource().subscribe({
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
      const users = await firstValueFrom(this.usersSource());

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
    return (
      this.studentSearchQuery.trim() !== "" || this.studentClassFilter !== ""
    );
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

  selectStudentForExtension(student: Student): void {
    this.selectedStudentForExtension = student;
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
        this.loansError = "Ontleningen laden mislukt.";
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
