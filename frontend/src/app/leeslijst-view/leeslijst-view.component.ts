import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";

interface LeeslijstBook {
  bookId: number;
  titel: string;
  auteur: string;
  cover: string;
  genre: string;
  paginas: number;
  isbn: string;
}

interface LeeslijstData {
  id: number;
  titel: string;
  description: string;
  schoolId: number;
  createdByName: string;
  createdAt: string;
  books: LeeslijstBook[];
  klasNames: string[];
}

@Component({
  selector: "app-leeslijst-view",
  templateUrl: "./leeslijst-view.component.html",
  styleUrls: ["./leeslijst-view.component.css"],
  standalone: false,
})
export class LeeslijstViewComponent implements OnInit {
  leeslijst: LeeslijstData | null = null;
  loading = true;
  error = "";

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private uiToastService: UiToastService,
  ) {}

  async ngOnInit() {
    const id = this.route.snapshot.paramMap.get("id");
    if (!id) {
      this.error = "Leeslijst ID niet gevonden.";
      this.loading = false;
      return;
    }

    await this.loadLeeslijst(parseInt(id, 10));
  }

  async loadLeeslijst(id: number) {
    this.loading = true;
    this.error = "";
    try {
      this.leeslijst = await this.bookService.getLeeslijst(id);
    } catch (error) {
      this.error = "Fout bij het laden van de leeslijst.";
      this.uiToastService.error(this.error);
      console.error(error);
    } finally {
      this.loading = false;
    }
  }

  goBack() {
    this.router.navigate(["/mijn-lijsten", { fragment: "klasleeslijst" }]);
  }

  goToDetail(bookId: number) {
    this.router.navigate(["/detail", bookId]);
  }

  displayCreatorName(createdByName: string): string {
    const currentSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    if (createdByName && createdByName !== currentSub) {
      return createdByName;
    }

    return (
      localStorage.getItem("userName") ||
      localStorage.getItem("fullname") ||
      [localStorage.getItem("firstName"), localStorage.getItem("lastName")]
        .filter(Boolean)
        .join(" ") ||
      createdByName ||
      currentSub
    );
  }

  printLeeslijst() {
    window.print();
  }
}
