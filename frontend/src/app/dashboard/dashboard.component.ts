import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ItemService } from '../item.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  featuredBooks: any[] = [];
  loading = true;

  constructor(private http: HttpClient, private router: Router, private itemService: ItemService) {}

  ngOnInit() {
    this.fetchBooks();
  }

  async fetchBooks() {
    this.loading = true;
    try {
      const data = await this.itemService.getBoeken();
      // We mappen de database velden naar ons visuele model
      this.featuredBooks = data.map((book: any) => ({
        titel: book.titel,
        auteur: book.auteur,
        genre: book.genre || "Algemeen",
        // Taal inkorten naar 2 letters (bijv. "Nederlands" -> "NE")
        taal: book.taal ? book.taal.substring(0, 2).toUpperCase() : "??",
        paginas: book.paginas || "?",
        // book.cover bevat de Base64 string van je @Lob uit Java
        coverUrl: book.cover || null,
        desc: book.beschrijving || "Geen beschrijving beschikbaar."
      }));
    } catch (error) {
      console.error("Fout bij ophalen boeken:", error);
    } finally {
      this.loading = false;
    }
  }
seeDetail(book: any) {
  this.router.navigate(['/detail', book.id]);
}
}
