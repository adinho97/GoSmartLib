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

  @Output() toggleFavorite = new EventEmitter<MouseEvent>();
  @Output() toggleWishlist = new EventEmitter<MouseEvent>();
  @Output() viewDetails = new EventEmitter<void>();

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
}

