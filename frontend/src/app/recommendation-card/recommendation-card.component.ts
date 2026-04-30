import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecommendedBook } from '../services/recommendation.service';

@Component({
  selector: 'app-recommendation-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recommendation-card.component.html',
  styleUrl: './recommendation-card.component.css',
})
export class RecommendationCardComponent {
  // Accept either RecommendedBook or any book object
  @Input() book!: RecommendedBook | any;
  @Input() isWishlisted: boolean = false;
  @Input() variant: 'normal' | 'didactic' | 'teacher' = 'normal';
  @Input() isTeacher: boolean = false;
  @Input() profileVariant: 'dashboard' | 'wishlist' | 'history' = 'dashboard';
  @Input() showRemoveBtn: boolean = false;
  @Input() isUnavailable: boolean = false;
  @Input() loanDate: string | null = null;

  @Output() toggleWishlist = new EventEmitter<MouseEvent>();
  @Output() viewDetails = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();
  @Output() toggleBell = new EventEmitter<void>();

  get isTeacherCard(): boolean {
    return this.variant === 'teacher' || (this.variant === 'didactic' && this.isTeacher);
  }

  // Handle both naming conventions (titel/title, auteur/author, etc.)
  get bookTitle(): string {
    return (this.book?.titel || this.book?.title) ?? '';
  }

  get bookAuthor(): string {
    return (this.book?.auteur || this.book?.author) ?? '';
  }

  get bookGenre(): string {
    return this.book?.genre ?? '';
  }

  get bookCover(): string | null | undefined {
    return this.book?.cover ?? null;
  }

  get bookLanguage(): string | null | undefined {
    return this.book?.taal ?? null;
  }

  get bookPages(): number | null | undefined {
    return this.book?.paginas ?? null;
  }

  get bookReason(): string {
    return this.book?.reason ?? '';
  }

  onToggleWishlist(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();
    this.toggleWishlist.emit(event);
  }

  onViewDetails() {
    this.viewDetails.emit();
  }

  onRemove(event: MouseEvent) {
    event.stopPropagation();
    this.remove.emit();
  }

  onToggleBell(event: MouseEvent) {
    event.stopPropagation();
    this.toggleBell.emit();
  }

  getLanguageAbbr(language: string): string {
    const languageMap: { [key: string]: string } = {
      'nl': 'NL',
      'dutch': 'NL',
      'nederlands': 'NL',
      'en': 'EN',
      'english': 'EN',
      'fr': 'FR',
      'french': 'FR',
      'français': 'FR',
      'de': 'DE',
      'german': 'DE',
      'deutsch': 'DE',
      'es': 'ES',
      'spanish': 'ES',
      'español': 'ES',
      'it': 'IT',
      'italian': 'IT',
      'italiano': 'IT',
      'pt': 'PT',
      'portuguese': 'PT',
      'português': 'PT',
    };
    return languageMap[language.toLowerCase()] || language.toUpperCase().slice(0, 2);
  }
}

