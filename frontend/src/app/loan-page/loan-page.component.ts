import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import { SchoolService } from "../services/school.service";

type BookOption = {
  id: number;
  titel: string;
  auteur: string;
  cover: string;
  availableCopies: number;
  totalCopies: number;
};

type Step = "leerling" | "boeken" | "bevestiging";

@Component({
  selector: "app-loan-page",
  templateUrl: "./loan-page.component.html",
  styleUrls: ["./loan-page.component.css"],
  standalone: false,
})
export class LoanPageComponent implements OnInit {
  step: Step = "leerling";

  // Stap 1
  username = "";
  usernameError = "";

  // Stap 2
  books: BookOption[] = [];
  filteredBooks: BookOption[] = [];
  searchQuery = "";
  selectedBooks: BookOption[] = [];
  activeLoans: Loan[] = [];
  selectedBookForReturn: BookOption | null = null;

  // Stap 3
  dueDate = "";
  today = new Date().toISOString().split("T")[0];
  defaultDueDate = (() => {
    const d = new Date();
    d.setDate(d.getDate() + 14);
    return d.toISOString().split("T")[0];
  })();

  isLoading = true;
  isLoaning = false;
  successMessage = "";
  errorMessage = "";

  readonly role = localStorage.getItem("role") || "";

  constructor(
    private bookService: BookService,
    private loanService: LoanService,
    private schoolService: SchoolService,
    private router: Router,
  ) {}

  async ngOnInit() {
    await this.loadBooks();
    this.dueDate = this.defaultDueDate;
  }

  async loadBooks() {
    this.isLoading = true;
    try {
      const schoolId = this.schoolService.getSelectedSchoolId() ?? undefined;
      const data = await this.bookService.getBooks(schoolId);
      this.books = data
        .filter((b: any) => (b.genre || "").toLowerCase() !== "didactiek" || this.role !== "leerling")
        .map((b: any) => ({
          id: b.id,
          titel: b.titel,
          auteur: b.auteur,
          cover: b.cover || "",
          availableCopies: b.availableCopies ?? 0,
          totalCopies: b.totalCopies ?? 0,
        }));
      this.filteredBooks = [...this.books];
    } catch {
      this.errorMessage = "Boeken laden mislukt.";
    } finally {
      this.isLoading = false;
    }
  }

  // Stap 1 — leerling bevestigen
  confirmUsername() {
    if (!this.username.trim()) {
      this.usernameError = "Voer een gebruikersnaam in.";
      return;
    }
    this.usernameError = "";
    this.step = "boeken";
    this.loadActiveLoansForUser();
  }

  async loadActiveLoansForUser() {
    try {
      this.activeLoans = await this.loanService.getActiveLoans(this.username.trim());
    } catch {
      this.activeLoans = [];
    }
  }

  // Stap 2 — boeken zoeken en selecteren
  onSearch() {
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredBooks = q
      ? this.books.filter(
          (b) =>
            b.titel.toLowerCase().includes(q) ||
            b.auteur.toLowerCase().includes(q),
        )
      : [...this.books];
  }

  isSelected(book: BookOption): boolean {
    return this.selectedBooks.some((b) => b.id === book.id);
  }

  toggleBook(book: BookOption) {
    if (book.availableCopies === 0) return;
    if (this.isSelected(book)) {
      this.selectedBooks = this.selectedBooks.filter((b) => b.id !== book.id);
    } else {
      this.selectedBooks = [...this.selectedBooks, book];
    }
  }

  proceedToConfirm() {
    if (this.selectedBooks.length === 0) {
      this.errorMessage = "Selecteer minstens één boek.";
      return;
    }
    this.errorMessage = "";
    this.step = "bevestiging";
  }

  // Stap 3 — bevestigen en uitlenen
  async loanBooks() {
    if (!this.dueDate) return;
    this.isLoaning = true;
    this.errorMessage = "";
    this.successMessage = "";
    try {
      for (const book of this.selectedBooks) {
        await this.loanService.createLoan(
          book.id,
          this.username.trim(),
          this.dueDate,
        );
      }
      this.successMessage = `${this.selectedBooks.length} boek(en) uitgeleend aan ${this.username.trim()}.`;
      this.selectedBooks = [];
      this.step = "leerling";
      this.username = "";
      this.dueDate = this.defaultDueDate;
      await this.loadBooks();
    } catch (e: any) {
      this.errorMessage =
        e?.response?.status === 409
          ? "Een of meer boeken zijn niet meer beschikbaar."
          : "Uitlenen mislukt. Probeer opnieuw.";
    } finally {
      this.isLoaning = false;
    }
  }

  // Terugbrengen
  async returnLoan(loan: Loan) {
    try {
      await this.loanService.returnLoan(loan.id);
      this.activeLoans = this.activeLoans.filter((l) => l.id !== loan.id);
      this.successMessage = `Boek "${loan.bookTitel}" teruggebracht.`;
      await this.loadBooks();
    } catch {
      this.errorMessage = "Terugbrengen mislukt.";
    }
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  // Navigatie
  goBack() {
    if (this.step === "boeken") {
      this.step = "leerling";
      this.selectedBooks = [];
      this.errorMessage = "";
    } else if (this.step === "bevestiging") {
      this.step = "boeken";
      this.errorMessage = "";
    }
  }

  resetAll() {
    this.step = "leerling";
    this.username = "";
    this.selectedBooks = [];
    this.activeLoans = [];
    this.searchQuery = "";
    this.filteredBooks = [...this.books];
    this.dueDate = this.defaultDueDate;
    this.successMessage = "";
    this.errorMessage = "";
  }
}