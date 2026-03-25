import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import { SchoolService } from "../services/school.service";
import axios from "axios";

type BookOption = {
  id: number;
  titel: string;
  auteur: string;
  cover: string;
  availableCopies: number;
  totalCopies: number;
};

type Leerling = {
  sub: string;
  username: string;
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
  leerlingen: Leerling[] = [];
  filteredLeerlingen: Leerling[] = [];
  leerlingSearch = "";
  selectedLeerling: Leerling | null = null;
  leerlingenLoading = false;
  leerlingError = "";

  // Stap 2
  books: BookOption[] = [];
  filteredBooks: BookOption[] = [];
  searchQuery = "";
  selectedBooks: BookOption[] = [];
  activeLoans: Loan[] = [];

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
    await Promise.all([this.loadBooks(), this.loadLeerlingen()]);
    this.dueDate = this.defaultDueDate;
  }

  async loadLeerlingen() {
    this.leerlingenLoading = true;
    try {
      const res = await axios.get("/api/gebruikers/leerlingen");
      this.leerlingen = res.data;
      this.filteredLeerlingen = [...this.leerlingen];
    } catch {
      this.leerlingError = "Leerlingen laden mislukt.";
    } finally {
      this.leerlingenLoading = false;
    }
  }

  onLeerlingSearch() {
    const q = this.leerlingSearch.trim().toLowerCase();
    this.filteredLeerlingen = q
      ? this.leerlingen.filter((l) =>
          l.username.toLowerCase().includes(q)
        )
      : [...this.leerlingen];
  }

  selectLeerling(leerling: Leerling) {
    this.selectedLeerling = leerling;
    this.leerlingError = "";
  }

  confirmLeerling() {
    if (!this.selectedLeerling) {
      this.leerlingError = "Selecteer een leerling.";
      return;
    }
    this.step = "boeken";
    this.loadActiveLoansForUser();
  }

  async loadActiveLoansForUser() {
    try {
      this.activeLoans = await this.loanService.getActiveLoans(
        this.selectedLeerling!.username
      );
    } catch {
      this.activeLoans = [];
    }
  }

  async loadBooks() {
    this.isLoading = true;
    try {
      const schoolId = this.schoolService.getSelectedSchoolId() ?? undefined;
      const data = await this.bookService.getBooks(schoolId);
      this.books = data
        .filter(
          (b: any) =>
            (b.genre || "").toLowerCase() !== "didactiek" ||
            this.role !== "leerling"
        )
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

  onSearch() {
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredBooks = q
      ? this.books.filter(
          (b) =>
            b.titel.toLowerCase().includes(q) ||
            b.auteur.toLowerCase().includes(q)
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

  async loanBooks() {
    if (!this.dueDate || !this.selectedLeerling) return;
    this.isLoaning = true;
    this.errorMessage = "";
    this.successMessage = "";
    try {
      for (const book of this.selectedBooks) {
        await this.loanService.createLoan(
          book.id,
          this.selectedLeerling.username,
          this.dueDate
        );
      }
      this.successMessage = `${this.selectedBooks.length} boek(en) uitgeleend aan ${this.selectedLeerling.username}.`;
      this.selectedBooks = [];
      this.step = "leerling";
      this.selectedLeerling = null;
      this.leerlingSearch = "";
      this.filteredLeerlingen = [...this.leerlingen];
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
}