import { Component, OnInit } from "@angular/core";
import { SchoolService } from "../services/school.service"; // Import the new service
import { LoanService, Loan } from "../services/loan.service";
import { UserService, StudentWithKlas } from "../services/user.service";

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

  selectedStudentForExtension: {
    sub: string;
    displayName: string;
    klas: string;
  } | null = null;

  today: string;

  constructor(
    private schoolService: SchoolService,
    private loanService: LoanService,
    private userService: UserService,
  ) {
    // Inject the SchoolService
    // Initialize 'today' for the date input's min attribute
    this.today = new Date().toISOString().split("T")[0];
  }

  ngOnInit(): void {
    this.loadAvailableClasses();
    // Load students from backend (includes klas)
    void this.loadStudents();
  }

  async openExtensionModal(): Promise<void> {
    this.extensionModalOpen = true;
    this.selectedStudentForExtension = null;
    this.studentSearchQuery = "";
    this.studentClassFilter = "";
    this.studentsError = "";

    this.loadAvailableClasses();
    await this.loadStudents();
    this.onSearchStudents();
  }

  private loadAvailableClasses(): void {
    this.schoolService.getClasses().subscribe({
      next: (classes) => {
        this.availableClasses = classes;
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
      const students = await this.userService.getAllStudentsWithKlas();
      // students: StudentWithKlas[] -> { sub, displayName, klas }
      // We still need to resolve display names via /api/users/{sub}/profile
      const resolved = await Promise.all(
        students.map(async (s) => {
          let display = s.sub;
          try {
            const profile: any = await fetch(
              `/api/users/${encodeURIComponent(s.sub)}/profile`,
            ).then((r) => r.json());
            display =
              profile?.fullName ||
              profile?.givenName ||
              profile?.familyName ||
              s.sub;
          } catch {
            // fallback to sub
          }
          return {
            sub: s.sub,
            displayName: display,
            klas: s.klas ?? "",
          };
        }),
      );
      this.allStudents = resolved;
      this.onSearchStudents();
    } catch (err) {
      console.error("Failed to load students for verlengen:", err);
      this.studentsError =
        "Er is iets misgegaan met het laden van de leerlingen.";
    } finally {
      this.studentsLoading = false;
    }
  }

  get isLibrarian(): boolean {
    return localStorage.getItem("role") === "bibbeheerder";
  }

  get eyebrow(): string {
    return this.isLibrarian ? "Bibliotheekbeheerder" : "Leerkracht";
  }

  // Methods for the "Leningen verlengen" modal
  closeExtensionModal(): void {
    this.extensionModalOpen = false;
  }

  onSearchStudents(): void {
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
      void this.loanService
        .updateLoanDueDate(Number(loan.id), loan.tempNewDueDate!)
        .then(() => {
          loan.dueDate = loan.tempNewDueDate!;
          delete loan.tempNewDueDate;
        })
        .catch((err) => {
          console.error("Failed to extend loan:", err);
          // Could show an error toast here
        });
    }
  }
}
