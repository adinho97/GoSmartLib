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

  addingSubgenreForId: number | null = null;
  newSubgenreNaam = '';

  editingSubgenreParentId: number | null = null;
  editingSubgenreId: number | null = null;
  editSubgenreNaam = '';

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
    this.cancelAllEdits();
    this.editingId = genre.id;
    this.editNaam = genre.naam;
  }

  saveEdit(id: number): void {
    const naam = this.editNaam.trim();
    if (!naam) return;
    this.genreService.update(id, naam).subscribe({
      next: (updated) => {
        const idx = this.genres.findIndex(g => g.id === id);
        if (idx !== -1) this.genres[idx] = updated;
        this.cancelAllEdits();
        this.showSuccess('Genre bijgewerkt.');
      },
      error: (e) => { this.error = e?.error?.message || 'Opslaan mislukt.'; },
    });
  }

  delete(id: number): void {
    if (!confirm('Genre en alle subgenres verwijderen?')) return;
    this.genreService.delete(id).subscribe({
      next: () => {
        this.genres = this.genres.filter(g => g.id !== id);
        this.showSuccess('Genre verwijderd.');
      },
      error: () => { this.error = 'Verwijderen mislukt.'; },
    });
  }


  startAddSubgenre(genreId: number): void {
    this.cancelAllEdits();
    this.addingSubgenreForId = genreId;
    this.newSubgenreNaam = '';
  }

  createSubgenre(parentId: number): void {
    const naam = this.newSubgenreNaam.trim();
    if (!naam) return;
    this.genreService.createSubgenre(parentId, naam).subscribe({
      next: (updated) => {
        const idx = this.genres.findIndex(g => g.id === parentId);
        if (idx !== -1) this.genres[idx] = updated;
        this.cancelAllEdits();
        this.showSuccess('Subgenre toegevoegd.');
      },
      error: (e) => { this.error = e?.error?.message || 'Toevoegen mislukt.'; },
    });
  }

  startEditSubgenre(parentId: number, subId: number, naam: string): void {
    this.cancelAllEdits();
    this.editingSubgenreParentId = parentId;
    this.editingSubgenreId = subId;
    this.editSubgenreNaam = naam;
  }

  saveSubgenreEdit(parentId: number, subId: number): void {
    const naam = this.editSubgenreNaam.trim();
    if (!naam) return;
    this.genreService.updateSubgenre(parentId, subId, naam).subscribe({
      next: (updated) => {
        const idx = this.genres.findIndex(g => g.id === parentId);
        if (idx !== -1) this.genres[idx] = updated;
        this.cancelAllEdits();
        this.showSuccess('Subgenre bijgewerkt.');
      },
      error: (e) => { this.error = e?.error?.message || 'Opslaan mislukt.'; },
    });
  }

  deleteSubgenre(parentId: number, subId: number): void {
    if (!confirm('Subgenre verwijderen?')) return;
    this.genreService.deleteSubgenre(parentId, subId).subscribe({
      next: () => {
        const idx = this.genres.findIndex(g => g.id === parentId);
        if (idx !== -1) {
          this.genres[idx].subgenres = this.genres[idx].subgenres.filter(s => s.id !== subId);
        }
        this.showSuccess('Subgenre verwijderd.');
      },
      error: () => { this.error = 'Verwijderen mislukt.'; },
    });
  }


  cancelAllEdits(): void {
    this.editingId = null;
    this.editNaam = '';
    this.addingSubgenreForId = null;
    this.newSubgenreNaam = '';
    this.editingSubgenreParentId = null;
    this.editingSubgenreId = null;
    this.editSubgenreNaam = '';
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    this.error = '';
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }
}