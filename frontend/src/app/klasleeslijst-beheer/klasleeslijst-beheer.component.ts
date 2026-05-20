import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { AuthContextService } from "../services/auth-context.service";

@Component({
  selector: "app-klasleeslijst-beheer",
  templateUrl: "./klasleeslijst-beheer.component.html",
  styleUrls: ["./klasleeslijst-beheer.component.css"],
})
export class KlasleeslijstBeheerComponent implements OnInit {
  leeslisten: any[] = [];
  klassen: any[] = [];

  titel: string = "";
  beschrijving: string = "";
  assignToEntireSchool: boolean = false;
  selectedKlasId: number | null = null;

  isLoading: boolean = false;
  error: string = "";

  constructor(
    private bookService: BookService,
    private schoolService: SchoolService,
    private authContextService: AuthContextService,
    private router: Router,
  ) {}

  async ngOnInit() {
    this.isLoading = true;
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (schoolId) {
      try {
        const [lists, classes] = await Promise.all([
          this.bookService.getLeeslisten(),
          this.schoolService.getKlassenBySchool(schoolId),
        ]);
        this.leeslisten = lists;
        this.klassen = classes;
      } catch (err) {
        this.error = "Laden van gegevens mislukt.";
      }
    }
    this.isLoading = false;
  }

  async saveLeeslijst() {
    if (!this.titel) {
      this.error = "Titel is verplicht.";
      return;
    }

    if (!this.assignToEntireSchool && !this.selectedKlasId) {
      this.error = "Selecteer een klas of kies voor de gehele school.";
      return;
    }

    const targetKlasIds = this.assignToEntireSchool
      ? null
      : [this.selectedKlasId!];
    const schoolId = this.schoolService.getSelectedSchoolId();

    try {
      await this.bookService.createLeeslijst(
        this.titel,
        this.beschrijving,
        [], // Currently no book selection in this view
        targetKlasIds,
        this.assignToEntireSchool,
        [], // No specific user sharing
        schoolId,
      );

      this.resetForm();
      this.leeslisten = await this.bookService.getLeeslisten();
    } catch (err) {
      this.error = "Opslaan mislukt.";
    }
  }

  async deleteLeeslijst(id: number) {
    if (confirm("Weet u zeker dat u deze leeslijst wilt verwijderen?")) {
      try {
        await this.bookService.deleteLeeslijst(id);
        this.leeslisten = await this.bookService.getLeeslisten();
      } catch (err) {
        this.error = "Verwijderen mislukt.";
      }
    }
  }

  private resetForm() {
    this.titel = "";
    this.beschrijving = "";
    this.assignToEntireSchool = false;
    this.selectedKlasId = null;
    this.error = "";
  }
}
