import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, RouterModule } from "@angular/router"; // Voeg RouterModule toe als dit standalone is
import { BookService } from "../services/book.service";
import { Book } from "../models/book";
import { Location } from "@angular/common";

@Component({
  selector: "app-detail",
  templateUrl: "./detail.component.html",
  styleUrls: ["./detail.component.css"],
  standalone: false,
})
export class DetailComponent implements OnInit {
  book!: Book;
  isLibrarian: boolean = false;

  constructor(
    private location: Location,
    private route: ActivatedRoute,
    private bookService: BookService,
  ) {}

  ngOnInit(): void {
    const role = localStorage.getItem("role");
    this.isLibrarian = role === "bibbeheerder";

    console.log("Huidige rol:", role);
    console.log("Is beheerder:", this.isLibrarian);

    const id = Number(this.route.snapshot.paramMap.get("id"));
    this.bookService.getBookById(id).subscribe((data) => {
      this.book = data;
    });
  }

  goBack() {
    this.location.back();
  }
}
