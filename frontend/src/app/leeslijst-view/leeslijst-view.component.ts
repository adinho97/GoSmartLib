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
  createdBySub?: string;
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
  deleting = false;
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

  get canDeleteLeeslijst(): boolean {
    const raw = localStorage.getItem("role") || "";
    const roles = raw
      .split(/[;,|\s]+/)
      .map((r) => r.trim().toLowerCase())
      .filter(Boolean);
    return roles.includes("leerkracht") || roles.includes("bibbeheerder");
  }

  get canEditLeeslijst(): boolean {
    if (!this.leeslijst) {
      return false;
    }

    const raw = localStorage.getItem("role") || "";
    const roles = raw
      .split(/[;,|\s]+/)
      .map((r) => r.trim().toLowerCase())
      .filter(Boolean);
    if (roles.includes("bibbeheerder")) {
      return true;
    }

    if (!roles.includes("leerkracht")) {
      return false;
    }

    const currentSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    return !!currentSub && this.leeslijst.createdBySub === currentSub;
  }

  editLeeslijst(): void {
    if (!this.leeslijst) {
      return;
    }
    this.router.navigate(["/leeslijst-edit", this.leeslijst.id]);
  }

  async deleteLeeslijst(): Promise<void> {
    if (!this.leeslijst || this.deleting) {
      return;
    }

    const confirmed = window.confirm(
      "Ben je zeker dat je deze leeslijst wil verwijderen?",
    );
    if (!confirmed) {
      return;
    }

    this.deleting = true;
    try {
      await this.bookService.deleteLeeslijst(this.leeslijst.id);
      this.uiToastService.success("Leeslijst verwijderd.");
      this.goBack();
    } catch (err: any) {
      if (err?.response?.status === 403) {
        this.uiToastService.error(
          "Je kan enkel je eigen leeslijsten verwijderen.",
        );
      } else {
        this.uiToastService.error("Fout bij verwijderen van leeslijst.");
      }
    } finally {
      this.deleting = false;
    }
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
