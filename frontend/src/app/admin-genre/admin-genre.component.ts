// src/app/admin-genre/admin-genre.component.ts
import { Component, OnInit } from '@angular/core';
import { AdminGenreService, Genre } from '../services/admin-genre.service';

@Component({
  selector: 'app-admin-genre',
  templateUrl: './admin-genre.component.html',
  styleUrls: ['./admin-genre.component.css'],
  standalone: false,
})
export class AdminGenreComponent implements OnInit {
  genres: Genre[] = [];
  isLoading = false;
  error = '';
  successMessage = '';

  newNaam = '';
  editingId: number | null = null;
  editNaam = '';

  constructor(private genreService: AdminGenreService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.genreService.getAll().subscribe({
      next: (genres) => { this.genres = genres; this.isLoading = false; },
      error: () => { this.error = 'Genres laden mislukt.'; this.isLoading = false; },
    });
  }

  create(): void {
    const naam = this.newNaam.trim();
    if (!naam) return;
    this.genreService.create(naam).subscribe({
      next: (genre) => {
        this.genres.push(genre);
        this.newNaam = '';
        this.showSuccess('Genre toegevoegd.');
      },
      error: (e) => { this.error = e?.error?.message || 'Toevoegen mislukt.'; },
    });
  }

  startEdit(genre: Genre): void {
    this.editingId = genre.id;
    this.editNaam = genre.naam;
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editNaam = '';
  }

  saveEdit(id: number): void {
    const naam = this.editNaam.trim();
    if (!naam) return;
    this.genreService.update(id, naam).subscribe({
      next: (updated) => {
        const idx = this.genres.findIndex(g => g.id === id);
        if (idx !== -1) this.genres[idx] = updated;
        this.cancelEdit();
        this.showSuccess('Genre bijgewerkt.');
      },
      error: (e) => { this.error = e?.error?.message || 'Opslaan mislukt.'; },
    });
  }

  delete(id: number): void {
    if (!confirm('Genre verwijderen?')) return;
    this.genreService.delete(id).subscribe({
      next: () => {
        this.genres = this.genres.filter(g => g.id !== id);
        this.showSuccess('Genre verwijderd.');
      },
      error: () => { this.error = 'Verwijderen mislukt.'; },
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    this.error = '';
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }
}