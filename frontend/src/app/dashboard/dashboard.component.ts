import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  featuredBooks: any[] = [];
  loading: boolean = true;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.fetchBooks();
  }

  fetchBooks() {
  const apiUrl = 'https://openlibrary.org/search.json?q=popular&limit=55';

  this.http.get(apiUrl).subscribe((response: any) => {
    this.featuredBooks = response.docs.map((book: any) => ({
      titel: book.title,
      auteur: book.author_name ? book.author_name[0] : 'Onbekende Auteur',
      // Genre: eerste item uit subject, anders 'Populair'
      genre: book.subject ? book.subject[0] : 'Populair',
      taal: book.language ? book.language[0].toUpperCase() : 'EN',
      
      // Pagina's: We proberen eerst median_pages, anders tonen we het aantal edities
      paginas: book.number_of_pages_median ? book.number_of_pages_median : (book.edition_count || '?'), 
      
      coverUrl: book.cover_i ? `https://covers.openlibrary.org/b/id/${book.cover_i}-M.jpg` : null,
      
      // Beschrijving: We gebruiken de 'first_publish_year' of een standaard tekst
      beschrijving: book.first_publish_year 
        ? `Dit populaire werk verscheen voor het eerst in ${book.first_publish_year} en heeft inmiddels ${book.edition_count} edities.` 
        : 'Een veelgelezen favoriet uit onze collectie.'
    }));
    this.loading = false;
  });
}
}


