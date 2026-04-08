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
  @Input() book!: RecommendedBook;
  @Input() isFavorited: boolean = false;
  @Input() isWishlisted: boolean = false;
  @Input() variant: 'normal' | 'didactic' | 'teacher' = 'normal';
  @Input() isTeacher: boolean = false;
  @Input() profileVariant: 'dashboard' | 'wishlist' | 'favorites' | 'history' = 'dashboard';
  @Input() showRemoveBtn: boolean = false;
  @Input() isUnavailable: boolean = false;
  @Input() loanDate: string | null = null;

  @Output() toggleFavorite = new EventEmitter<MouseEvent>();
  @Output() toggleWishlist = new EventEmitter<MouseEvent>();
  @Output() viewDetails = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();
  @Output() toggleBell = new EventEmitter<void>();

  get isTeacherCard(): boolean {
    return this.variant === 'teacher' || (this.variant === 'didactic' && this.isTeacher);
  }

  onToggleFavorite(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();
    this.toggleFavorite.emit(event);
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

