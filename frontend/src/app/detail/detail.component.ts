import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BookService } from "../services/book.service";
import { Location } from "@angular/common";

// Assuming BookDetail type is defined elsewhere or can be inferred from BookService
type BookDetail = {
  id?: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  lestip?: string;
  genre: string;
  uitgaveDatum: string;
  paginas: number | null;
  taal: string;
  uitgeverij: string;
  reviewCount?: number;
  averageRating?: number;
};

@Component({
  selector: "app-detail",
  templateUrl: "./detail.component.html",
  styleUrls: ["./detail.component.css"],
  standalone: false, // Or true, depending on your project setup
})
export class DetailComponent implements OnInit {
  book: BookDetail | undefined;
  currentBookId: number | null = null;
  isLoading = true;
  error = "";
  favoritedBookIds = new Set<number>();

  constructor(
    private route: ActivatedRoute,
    private bookService: BookService,
    private location: Location, // Inject Location for goBack
  ) {}

  async ngOnInit(): Promise<void> {
    this.route.paramMap.subscribe(async (params) => {
      const id = params.get("id");
      if (id) {
        this.currentBookId = +id;
        await this.loadBookDetails(this.currentBookId);
        await this.loadFavoriteState();
      }
    });
  }

  private async loadBookDetails(bookId: number): Promise<void> {
    this.isLoading = true;
    try {
      // Assuming getBookById returns an Observable, convert to Promise
      this.book = await this.bookService.getBookById(bookId).toPromise();
    } catch (err) {
      this.error = "Boekdetails laden mislukt.";
      console.error(err);
    } finally {
      this.isLoading = false;
    }
  }

  private async loadFavoriteState() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      this.favoritedBookIds = new Set(
        favorites.map((item: any) => item.bookId),
      );
    } catch (err) {
      console.error("Fout bij laden favorieten:", err);
      this.favoritedBookIds = new Set<number>();
    }
  }

  async toggleFavorite(event: MouseEvent, bookId?: number) {
    event.stopPropagation();
    event.preventDefault();
    if (!bookId) return;

    try {
      if (this.isFavorited(bookId)) {
        await this.bookService.removeFromFavorites(bookId);
        this.favoritedBookIds.delete(bookId);
      } else {
        await this.bookService.addToFavorites(bookId);
        this.favoritedBookIds.add(bookId);
      }
    } catch (err) {
      this.error = "Favorieten bijwerken mislukt.";
      console.error("Fout bij bijwerken favorieten:", err);
    }
  }

  isFavorited(bookId?: number): boolean {
    return !!bookId && this.favoritedBookIds.has(bookId);
  }

  goBack(): void {
    this.location.back();
  }
}
