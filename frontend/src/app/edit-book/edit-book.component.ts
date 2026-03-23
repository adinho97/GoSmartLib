import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import { Book } from "../models/book";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-edit-book",
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: "./edit-book.component.html",
  styleUrls: ["./edit-book.component.css"],
})
export class EditBookComponent implements OnInit {
  bookId!: number;

  book: Book = {
    id: 0,
    titel: "",
    auteur: "",
    cover: "",
    beschrijving: "",
    genre: "",
    uitgaveDatum: "",
    paginas: 0,
    taal: "",
    uitgeverij: "",
  };

  isLoading = true;
  errorMessage = "";

  copySummary = { total: 0, available: 0 };
  isAddingCopy = false;
  isRemovingCopy = false;
  copyMessage = "";
  copyError = "";

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    if (idParam) {
      this.bookId = Number(idParam);
      this.loadBook();
      this.loadCopySummary();
    }
  }

  loadBook() {
    this.bookService.getBookById(this.bookId).subscribe({
      next: (data) => {
        this.book = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Kon het boek niet laden.";
        this.isLoading = false;
      },
    });
  }

  async loadCopySummary() {
    try {
      this.copySummary = await this.loanService.getCopySummary(this.bookId);
    } catch {
      this.copySummary = { total: 0, available: 0 };
    }
  }

  async addCopy() {
    this.isAddingCopy = true;
    this.copyMessage = "";
    this.copyError = "";
    try {
      await this.loanService.addCopy(this.bookId);
      // Herlaad de summary vers van de server
      await this.loadCopySummary();
      this.copyMessage = "Exemplaar toegevoegd.";
    } catch {
      this.copyError = "Toevoegen mislukt.";
    } finally {
      this.isAddingCopy = false;
    }
  }

  async removeCopy() {
    if (this.copySummary.available === 0) {
      this.copyError = "Geen beschikbare exemplaren om te verwijderen.";
      return;
    }
    this.isRemovingCopy = true;
    this.copyMessage = "";
    this.copyError = "";
    try {
      await this.loanService.removeAvailableCopy(this.bookId);
      await this.loadCopySummary();
      this.copyMessage = "Exemplaar verwijderd.";
    } catch (err: any) {
      this.copyError = err?.message || "Verwijderen mislukt.";
    } finally {
      this.isRemovingCopy = false;
    }
  }

  async onSubmit() {
    try {
      await this.bookService.updateBook(this.bookId, this.book);
      this.router.navigate(["/detail", this.bookId]);
    } catch {
      this.errorMessage = "Fout bij het opslaan van wijzigingen.";
    }
  }

  cancel() {
    this.router.navigate(["/detail", this.bookId]);
  }
}
