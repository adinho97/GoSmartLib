import { Component, OnInit } from "@angular/core";

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
  studentClassFilter: string = "";
  availableClasses: string[] = ["Klas A", "Klas B", "Klas C"]; // Example classes

  // Define a basic structure for a student
  // In a real application, this would likely be an interface or a class from a shared model.
  filteredStudents: { sub: string; displayName: string; klas: string }[] = [];
  allStudents: { sub: string; displayName: string; klas: string }[] = [
    { sub: "s1", displayName: "Jan Jansen", klas: "Klas A" },
    { sub: "s2", displayName: "Piet Pietersen", klas: "Klas B" },
    { sub: "s3", displayName: "Klaas Klaassen", klas: "Klas A" },
    { sub: "s4", displayName: "Marieke Meijer", klas: "Klas C" },
  ]; // Example student data

  selectedStudentForExtension: {
    sub: string;
    displayName: string;
    klas: string;
  } | null = null;

  // Define a basic structure for a loan
  // In a real application, this would likely be an interface or a class from a shared model.
  studentLoans: {
    id: string;
    bookTitel: string;
    dueDate: string;
    tempNewDueDate?: string;
  }[] = [];

  today: string;

  constructor() {
    // Initialize 'today' for the date input's min attribute
    this.today = new Date().toISOString().split("T")[0];
  }

  ngOnInit(): void {
    this.onSearchStudents(); // Initialize student list on component load
  }

  get isLibrarian(): boolean {
    return localStorage.getItem("role") === "bibbeheerder";
  }

  get eyebrow(): string {
    return this.isLibrarian ? "Bibliotheekbeheerder" : "Leerkracht";
  }

  // Methods for the "Leningen verlengen" modal
  openExtensionModal(): void {
    this.extensionModalOpen = true;
    this.selectedStudentForExtension = null; // Reset selected student when opening
    this.studentSearchQuery = ""; // Clear search query
    this.studentClassFilter = ""; // Clear class filter
    this.onSearchStudents(); // Refresh student list
  }

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
    // Simulate fetching loans for the selected student
    this.studentLoans = [
      { id: "l1", bookTitel: "De Hobbit", dueDate: "2024-05-20" },
      { id: "l2", bookTitel: "Lord of the Rings", dueDate: "2024-06-15" },
    ];
  }

  isOverdue(dueDate: string): boolean {
    return new Date(dueDate) < new Date(this.today);
  }

  confirmExtension(loan: {
    id: string;
    bookTitel: string;
    dueDate: string;
    tempNewDueDate?: string;
  }): void {
    if (loan.tempNewDueDate) {
      console.log(
        `Loan ${loan.id} for book "${loan.bookTitel}" extended to ${loan.tempNewDueDate}`,
      );
      // In a real application, you would send this update to a backend service.
      // After successful update, you might want to refresh the loans list or update the specific loan.
      loan.dueDate = loan.tempNewDueDate; // Update the display date
      delete loan.tempNewDueDate; // Clear the temporary date
    }
  }
}
