import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { Book } from "../models/book";
import { FormsModule } from "@angular/forms";

@Component({
  selector: "app-edit-book",
  standalone: true,
  imports: [FormsModule],
  templateUrl: "./edit-book.component.html",
  styleUrls: ["./edit-book.component.css"],
})
export class EditBookComponent implements OnInit {
  bookId!: number;
  readonly genres = [
    "Didactiek",
    "Fictie algemeen",
    "Literaire roman",
    "Spanning / thriller",
    "Detective / misdaad",
    "Fantasy",
    "Sciencefiction",
    "Dystopie",
    "Historische roman",
    "Romantiek",
    "Coming-of-age",
    "Avontuur",
    "Oorlog & conflict",
    "Horror",
    "Humor",
    "Graphic novel / strip",
    "Poëzie",
    "Non-fictie algemeen",
  ];

  // Geen ISBN meer hier, want het staat niet in je interface
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    if (idParam) {
      this.bookId = Number(idParam);
      this.loadBook();
    }
  }

  loadBook() {
    this.bookService.getBookById(this.bookId).subscribe({
      next: (data) => {
        this.book = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = "Kon het boek niet laden.";
        this.isLoading = false;
      },
    });
  }

  async onSubmit() {
    try {
      await this.bookService.updateBook(this.bookId, this.book);
      this.router.navigate(["/books"]);
    } catch (err) {
      this.errorMessage = "Fout bij het opslaan van wijzigingen.";
    }
  }

  cancel() {
    this.router.navigate(["/books"]);
  }
}
