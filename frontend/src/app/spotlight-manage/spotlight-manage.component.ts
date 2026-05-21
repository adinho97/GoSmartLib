import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { BookService } from '../services/book.service';

export interface SpotlightBook {
  bookId: number;
  titel: string;
  auteur: string;
  cover: string;
}

export interface SpotlightState {
  maand: SpotlightBook | null;
  thema: SpotlightBook | null;
}

interface BookSearchResult {
  id: number;
  titel: string;
  auteur: string;
  cover: string | null;
}

interface HighlightDisplayBook {
  titel: string;
  auteur: string;
  cover?: string | null;
}

@Component({
  selector: 'app-spotlight-manage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './spotlight-manage.component.html',
  styleUrls: ['./spotlight-manage.component.css'],
})
export class SpotlightManageComponent {
  @Input() spotlight: SpotlightState = { maand: null, thema: null };
  @Input() spotlightLoading = false;
  @Input() highlightedBooks: HighlightDisplayBook[] = [];
  @Input() highlightedBookIds: Set<number> = new Set();
  @Input() highlightedCount = 0;
  @Input() booksLoading = false;
  @Input() schoolId: number | null = null;

  @Output() spotlightSaved = new EventEmitter<{ type: 'MAAND' | 'THEMA'; book: SpotlightBook }>();
  @Output() spotlightCleared = new EventEmitter<'MAAND' | 'THEMA'>();
  @Output() highlightsAdded = new EventEmitter<void>();

  spotlightSaving: 'MAAND' | 'THEMA' | null = null;
  spotlightClearing: 'MAAND' | 'THEMA' | null = null;

  pickerOpen = false;
  pickerType: 'MAAND' | 'THEMA' = 'MAAND';
  pickerQuery = '';
  pickerResults: BookSearchResult[] = [];
  pickerSearching = false;
  private pickerDebounce: ReturnType<typeof setTimeout> | null = null;

  highlightPickerOpen = false;
  highlightQuery = '';
  highlightResults: BookSearchResult[] = [];
  highlightSearching = false;
  highlightSaving = false;
  highlightError = '';
  highlightSelectedIds = new Set<number>();
  highlightSelectedBooks: BookSearchResult[] = [];
  private highlightDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor(private http: HttpClient, private bookService: BookService) {}

  get currentMonthLabel(): string {
    return new Date().toLocaleDateString('nl-BE', { month: 'long', year: 'numeric' });
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    this.closePicker();
    this.closeHighlightPicker();
  }

  openPicker(type: 'MAAND' | 'THEMA'): void {
    this.pickerType = type;
    this.pickerQuery = '';
    this.pickerResults = [];
    this.pickerSearching = false;
    this.pickerOpen = true;
    setTimeout(() => document.getElementById('sp-search-input')?.focus(), 50);
  }

  closePicker(): void {
    this.pickerOpen = false;
    if (this.pickerDebounce !== null) {
      clearTimeout(this.pickerDebounce);
      this.pickerDebounce = null;
    }
  }

  onPickerSearch(): void {
    if (this.pickerDebounce !== null) clearTimeout(this.pickerDebounce);
    if (!this.pickerQuery.trim()) { this.pickerResults = []; return; }
    this.pickerDebounce = setTimeout(() => this.searchBooks(), 300);
  }

  private async searchBooks(): Promise<void> {
    if (!this.schoolId || !this.pickerQuery.trim()) return;
    this.pickerSearching = true;
    try {
      const res = await this.bookService.getBooksPage(
        0,
        8,
        this.pickerQuery.trim(),
        this.schoolId,
      );
      this.pickerResults = (res.items || []).map((book) => ({
        id: book.id,
        titel: book.titel,
        auteur: book.auteur,
        cover: book.cover || null,
      }));
    } catch {
      this.pickerResults = [];
    } finally {
      this.pickerSearching = false;
    }
  }

  selectBook(book: BookSearchResult): void {
    if (!this.schoolId) return;
    const type = this.pickerType;
    this.spotlightSaving = type;
    this.http.put<SpotlightBook>(`/api/spotlight/${this.schoolId}/${type}`, { bookId: book.id }).subscribe({
      next: (saved) => {
        this.spotlightSaved.emit({ type, book: saved });
        this.spotlightSaving = null;
        this.closePicker();
      },
      error: () => { this.spotlightSaving = null; },
    });
  }

  clearSpotlight(type: 'MAAND' | 'THEMA'): void {
    if (!this.schoolId) return;
    this.spotlightClearing = type;
    this.http.delete(`/api/spotlight/${this.schoolId}/${type}`).subscribe({
      next: () => {
        this.spotlightCleared.emit(type);
        this.spotlightClearing = null;
      },
      error: () => { this.spotlightClearing = null; },
    });
  }

  openHighlightPicker(): void {
    this.highlightPickerOpen = true;
    this.highlightQuery = '';
    this.highlightResults = [];
    this.highlightSearching = false;
    this.highlightSaving = false;
    this.highlightError = '';
    this.highlightSelectedIds = new Set();
    this.highlightSelectedBooks = [];
    setTimeout(() => document.getElementById('hk-search-input')?.focus(), 50);
  }

  closeHighlightPicker(): void {
    this.highlightPickerOpen = false;
    if (this.highlightDebounce !== null) {
      clearTimeout(this.highlightDebounce);
      this.highlightDebounce = null;
    }
  }

  onHighlightSearch(): void {
    if (this.highlightDebounce !== null) clearTimeout(this.highlightDebounce);
    if (!this.highlightQuery.trim()) {
      this.highlightResults = [];
      return;
    }
    this.highlightDebounce = setTimeout(() => this.searchHighlightBooks(), 300);
  }

  private async searchHighlightBooks(): Promise<void> {
    if (!this.schoolId || !this.highlightQuery.trim()) return;
    this.highlightSearching = true;
    try {
      const res = await this.bookService.getBooksPage(
        0,
        8,
        this.highlightQuery.trim(),
        this.schoolId,
      );
      this.highlightResults = (res.items || []).map((book) => ({
        id: book.id,
        titel: book.titel,
        auteur: book.auteur,
        cover: book.cover || null,
      }));
    } catch {
      this.highlightResults = [];
    } finally {
      this.highlightSearching = false;
    }
  }

  isHighlightLocked(bookId?: number): boolean {
    return !!bookId && this.highlightedBookIds.has(bookId);
  }

  isHighlightSelected(bookId?: number): boolean {
    return !!bookId && this.highlightSelectedIds.has(bookId);
  }

  toggleHighlightSelection(book: BookSearchResult): void {
    if (!book?.id || this.isHighlightLocked(book.id)) return;
    if (this.highlightSelectedIds.has(book.id)) {
      this.highlightSelectedIds.delete(book.id);
      this.highlightSelectedBooks = this.highlightSelectedBooks.filter((b) => b.id !== book.id);
      return;
    }
    this.highlightSelectedIds.add(book.id);
    this.highlightSelectedBooks = [...this.highlightSelectedBooks, book];
  }

  removeHighlightSelection(bookId: number): void {
    this.highlightSelectedIds.delete(bookId);
    this.highlightSelectedBooks = this.highlightSelectedBooks.filter((b) => b.id !== bookId);
  }

  async confirmHighlightAdd(): Promise<void> {
    if (!this.schoolId) return;
    const ids = Array.from(this.highlightSelectedIds).filter((id) => !this.highlightedBookIds.has(id));
    if (ids.length === 0) return;
    this.highlightSaving = true;
    this.highlightError = '';
    try {
      for (const id of ids) {
        await this.bookService.toggleHighlight(id, this.schoolId);
        this.highlightedBookIds.add(id);
      }
      this.highlightsAdded.emit();
      this.closeHighlightPicker();
    } catch {
      this.highlightError = "Toevoegen aan 'In de kijker' mislukt.";
    } finally {
      this.highlightSaving = false;
    }
  }
}
