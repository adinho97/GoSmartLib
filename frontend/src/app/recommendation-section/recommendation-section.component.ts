import {
  Component,
  Input,
  Output,
  OnInit,
  OnDestroy,
  EventEmitter,
} from "@angular/core";
import { Router } from "@angular/router";
import { CommonModule } from "@angular/common";
import { Subscription, skip } from "rxjs";
import { BookService } from "../services/book.service";
import {
  UserPreferencesService,
  PreferenceKey,
} from "../services/user-preferences.service";
import { RecommendedBook } from "../services/recommendation.service";
import { RecommendationCardComponent } from "../recommendation-card/recommendation-card.component";

@Component({
  selector: "app-recommendation-section",
  standalone: true,
  imports: [CommonModule, RecommendationCardComponent],
  templateUrl: "./recommendation-section.component.html",
  styleUrl: "./recommendation-section.component.css",
})
export class RecommendationSectionComponent implements OnInit, OnDestroy {
  @Input() books: RecommendedBook[] = [];
  @Input() title: string = "Aanbevelingen";
  @Input() layout: "shelf" | "hero" = "shelf";
  @Input() variant: "normal" | "didactic" | "teacher" = "normal";
  @Input() section: string = "default";
  @Input() showInfoButton = false;
  @Input() infoAriaLabel = "Info over deze sectie";

  @Output() refreshRecommendations = new EventEmitter<boolean>();
  @Output() infoRequested = new EventEmitter<void>();

  excludeRead = true;
  wishlistedBookIds = new Set<number>();
  favoritedBookIds = new Set<number>();

  private readonly EXCLUDE_READ_STORAGE_KEY_PREFIX =
    "recommendationExcludeRead_";
  private subscription: Subscription | null = null;

  private get storageKey(): string {
    return `${this.EXCLUDE_READ_STORAGE_KEY_PREFIX}${this.section}`;
  }

  get isTeacher(): boolean {
    const role = localStorage.getItem("role");
    return role === "leerkracht" || role === "bibbeheerder";
  }

  constructor(
    private bookService: BookService,
    private userPreferencesService: UserPreferencesService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.initPreferences();
    this.loadWishlistState();
    this.loadFavoritesState();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  private initPreferences(): void {
    const prefsKey =
      `recommendationExcludeRead_${this.section}` as PreferenceKey;

    // Seed initial value without emitting refresh to avoid duplicate fetch on route load.
    const initialPrefs = this.userPreferencesService.getSnapshotForLegacyUse();
    this.excludeRead = initialPrefs[prefsKey] ?? true;

    this.subscription = this.userPreferencesService.preferences$
      .pipe(skip(1))
      .subscribe((prefs) => {
        const value = prefs[prefsKey];
        if (value !== undefined && value !== this.excludeRead) {
          this.excludeRead = value;
          this.refreshRecommendations.emit(this.excludeRead);
        }
      });
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

  get limitedBooks(): RecommendedBook[] {
    return this.books.slice(0, 25);
  }

  async toggleWishlist(event: MouseEvent, bookId: number) {
    event.stopPropagation();
    const wasWishlisted = this.wishlistedBookIds.has(bookId);

    wasWishlisted
      ? this.wishlistedBookIds.delete(bookId)
      : this.wishlistedBookIds.add(bookId);

    try {
      wasWishlisted
        ? await this.bookService.removeFromWishlist(bookId)
        : await this.bookService.addToWishlist(bookId);
    } catch (error) {
      wasWishlisted
        ? this.wishlistedBookIds.add(bookId)
        : this.wishlistedBookIds.delete(bookId);
      console.error("Failed to toggle wishlist for book", bookId, error);
      // TODO: Emit toast/error event for user feedback
    }
  }

  async toggleFavorite(event: MouseEvent, bookId: number) {
    event.stopPropagation();
    const wasFavorited = this.favoritedBookIds.has(bookId);

    wasFavorited
      ? this.favoritedBookIds.delete(bookId)
      : this.favoritedBookIds.add(bookId);

    try {
      wasFavorited
        ? await this.bookService.removeFromFavorites(bookId)
        : await this.bookService.addToFavorites(bookId);
    } catch (error) {
      wasFavorited
        ? this.favoritedBookIds.add(bookId)
        : this.favoritedBookIds.delete(bookId);
      console.error("Failed to toggle favorite for book", bookId, error);
      // TODO: Emit toast/error event for user feedback
    }
  }

  seeDetail(bookId: number) {
    this.router.navigate(["/detail", bookId]);
  }

  toggleExcludeRead() {
    this.excludeRead = !this.excludeRead;
    localStorage.setItem(this.storageKey, String(this.excludeRead));
    const prefsKey =
      `recommendationExcludeRead_${this.section}` as PreferenceKey;
    this.userPreferencesService.savePreference(prefsKey, this.excludeRead);
    this.refreshRecommendations.emit(this.excludeRead);
  }

  requestInfo(): void {
    this.infoRequested.emit();
  }
}
