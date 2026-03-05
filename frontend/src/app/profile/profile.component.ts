import { Component } from '@angular/core';
import { AuthGuard } from '../auth.guard';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {

  role = "User";

  favoriteBooks = [
    { title: "The Hobbit", author: "J.R.R. Tolkien", cover: "https://covers.openlibrary.org/b/id/6979861-L.jpg" },
    { title: "1984", author: "George Orwell", cover: "https://covers.openlibrary.org/b/id/6979862-L.jpg" }
  ];

  readingHistory = [
    { title: "Dune", finishedDate: new Date("2025-12-12"), cover: "https://covers.openlibrary.org/b/id/6979863-L.jpg" }
  ];

  borrowedBooks = [
    { title: "Harry Potter", deadline: new Date("2026-03-20"), cover: "https://covers.openlibrary.org/b/id/6979864-L.jpg" }
  ];

  settings = {
    showFavorites: true,
    showReadingHistory: true,
    showDeadlines: true
  };

  saveSettings() {
    console.log("Saved settings:", this.settings);
  }
}
