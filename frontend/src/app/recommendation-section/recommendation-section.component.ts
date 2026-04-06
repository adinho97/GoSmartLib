import { Component, Input, Output, OnInit, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { BookService } from '../services/book.service';
import { RecommendedBook } from '../services/recommendation.service';
import { RecommendationCardComponent } from '../recommendation-card/recommendation-card.component';

@Component({
  selector: 'app-recommendation-section',
  standalone: true,
  imports: [CommonModule, RecommendationCardComponent],
  templateUrl: './recommendation-section.component.html',
  styleUrl: './recommendation-section.component.css',
})
export class RecommendationSectionComponent implements OnInit {
  @Input() books: RecommendedBook[] = [];
  @Input() title: string = 'Aanbevelingen';
  @Input() layout: 'shelf' | 'hero' = 'shelf';
  @Input() variant: 'normal' | 'didactic' | 'teacher' = 'normal';

  @Output() refreshRecommendations = new EventEmitter<boolean>();

  excludeRead = true;
  wishlistedBookIds = new Set<number>();
  favoritedBookIds = new Set<number>();

  get isTeacher(): boolean {
    const role = localStorage.getItem('role');
    return role === 'leerkracht' || role === 'bibbeheerder';
  }

  constructor(
    private bookService: BookService,
    private router: Router
  ) {}

  async ngOnInit() {
    await Promise.all([
      this.loadWishlistState(),
      this.loadFavoritesState(),
    ]);
  }

  private async loadWishlistState() {
    try {
      const wishlist = await this.bookService.getUserWishlist();
      this.wishlistedBookIds = new Set(wishlist.map((item) => item.bookId));
    } catch {
      this.wishlistedBookIds = new Set<number>();
    }
  }

  private async loadFavoritesState() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      this.favoritedBookIds = new Set(favorites.map((item) => item.bookId));
    } catch {
      this.favoritedBookIds = new Set<number>();
    }
  }

  isWishlisted(bookId: number): boolean {
    return this.wishlistedBookIds.has(bookId);
  }

  isFavorited(bookId: number): boolean {
    return this.favoritedBookIds.has(bookId);
  }

  async toggleWishlist(event: MouseEvent, bookId: number) {
    event.stopPropagation();

    try {
      if (this.wishlistedBookIds.has(bookId)) {
        await this.bookService.removeFromWishlist(bookId);
        this.wishlistedBookIds.delete(bookId);
      } else {
        await this.bookService.addToWishlist(bookId);
        this.wishlistedBookIds.add(bookId);
      }
    } catch {
      // Silent fail
    }
  }

  async toggleFavorite(event: MouseEvent, bookId: number) {
    event.stopPropagation();

    try {
      if (this.favoritedBookIds.has(bookId)) {
        await this.bookService.removeFromFavorites(bookId);
        this.favoritedBookIds.delete(bookId);
      } else {
        await this.bookService.addToFavorites(bookId);
        this.favoritedBookIds.add(bookId);
      }
    } catch {
      // Silent fail
    }
  }

  seeDetail(bookId: number) {
    this.router.navigate(['/detail', bookId]);
  }

  toggleExcludeRead() {
    this.excludeRead = !this.excludeRead;
    this.refreshRecommendations.emit(this.excludeRead);
  }
}
