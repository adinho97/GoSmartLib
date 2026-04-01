import { Component, HostListener, Input } from "@angular/core";
import { Location } from "@angular/common";
import { Router } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { Subscription } from "rxjs";
import { SmartschoolService } from "../services/smartschool.service";
import { BookService } from "../services/book.service";

type ProfileBookCard = {
  title: string;
  author?: string;
  cover: string;
  id: number;
  deadline?: Date;
  loanedDate?: Date;
  wishlistId?: number;
  notificationEnabled?: boolean;
  availableCopies?: number;
  totalCopies?: number;
};

@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.css"],
  standalone: false,
})
export class ProfileComponent {
  @Input() embedded = false;
  @Input() showHero = true;
  @Input() showSections = true;

  role = localStorage.getItem("role") || "gebruiker";

  dashboardSettings = {
    showWishlist: true,
    showFavorites: true,
    showReadingHistory: true,
    showBorrowed: true,
    showHighlighted: true,
    showDeadline: true,
  };

  userName = localStorage.getItem("userName") || "Gebruiker";

  settingsOpen = false;

  wishlistBooks: ProfileBookCard[] = [];
  favoriteBooks: ProfileBookCard[] = [];
  readingHistory: ProfileBookCard[] = [
    { title: "Book Three", loanedDate: new Date(), cover: "", id: 3 },
  ];
  borrowedBooks: ProfileBookCard[] = [
    { title: "Book Four", deadline: new Date(), cover: "", id: 4 },
  ];
  readingList: ProfileBookCard[] = [
    { title: "Book Five", author: "Author C", cover: "", id: 5 },
  ];
  wishlistLoading = false;
  notificationToggleErrors: Record<number, string> = {};
  readonly wishlistPageSize = 6;
  readonly favoritePageSize = 6;
  currentWishlistPage = 1;
  currentFavoritePage = 1;
  private wishlistChangedSub?: Subscription;
  private favoriteChangedSub?: Subscription;

  constructor(
    private location: Location,
    private router: Router,
    private smartschoolService: SmartschoolService,
    private http: HttpClient,
    private bookService: BookService,
  ) {}

  async ngOnInit() {
    const saved = localStorage.getItem("dashboardSettings");
    if (saved) {
      this.dashboardSettings = JSON.parse(saved);
    }

    if (this.showSections) {
      await this.loadWishlistBooks();
      await this.loadFavoriteBooks();
      this.wishlistChangedSub = this.bookService.wishlistChanged$.subscribe(
        () => {
          this.loadWishlistBooks();
        },
      );
      this.favoriteChangedSub = this.bookService.favoriteChanged$.subscribe(
        () => {
          this.loadFavoriteBooks();
        },
      );
    }
  }

  ngOnDestroy() {
    this.wishlistChangedSub?.unsubscribe();
    this.favoriteChangedSub?.unsubscribe();
  }

  private async loadWishlistBooks() {
    this.wishlistLoading = true;
    try {
      const wishlist = await this.bookService.getUserWishlist();
      this.wishlistBooks = wishlist.map((item) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
        wishlistId: item.id,
        notificationEnabled: item.notificationEnabled ?? false,
        availableCopies: item.availableCopies ?? 0,
        totalCopies: item.totalCopies ?? 0,
      }));
      this.currentWishlistPage = 1;
    } catch {
      this.wishlistBooks = [];
    } finally {
      this.wishlistLoading = false;
    }
  }

  get totalWishlistPages(): number {
    return Math.max(
      1,
      Math.ceil(this.wishlistBooks.length / this.wishlistPageSize),
    );
  }

  get wishlistPageNumbers(): number[] {
    return Array.from({ length: this.totalWishlistPages }, (_, i) => i + 1);
  }

  get pagedWishlistBooks(): ProfileBookCard[] {
    const start = (this.currentWishlistPage - 1) * this.wishlistPageSize;
    return this.wishlistBooks.slice(start, start + this.wishlistPageSize);
  }

  goToWishlistPage(page: number) {
    this.currentWishlistPage = page;
  }

  get totalFavoritePages(): number {
    return Math.max(
      1,
      Math.ceil(this.favoriteBooks.length / this.favoritePageSize),
    );
  }

  get favoritePageNumbers(): number[] {
    return Array.from({ length: this.totalFavoritePages }, (_, i) => i + 1);
  }

  get pagedFavoriteBooks(): ProfileBookCard[] {
    const start = (this.currentFavoritePage - 1) * this.favoritePageSize;
    return this.favoriteBooks.slice(start, start + this.favoritePageSize);
  }

  get favoritesCount(): number {
    return this.favoriteBooks.length;
  }

  get wishlistCount(): number {
    return this.wishlistBooks.length;
  }

  get highlightedBooksCount(): number {
    return this.readingList.length;
  }

  goToFavoritePage(page: number) {
    this.currentFavoritePage = page;
  }

  async removeFromWishlist(event: MouseEvent, bookId: number) {
    event.stopPropagation();
    event.preventDefault();

    try {
      await this.bookService.removeFromWishlist(bookId);
      this.wishlistBooks = this.wishlistBooks.filter(
        (book) => book.id !== bookId,
      );

      if (this.currentWishlistPage > this.totalWishlistPages) {
        this.currentWishlistPage = this.totalWishlistPages;
      }
    } catch {
      // Keep silent here to avoid noisy alerts on dashboard profile cards.
    }
  }

  async removeFromFavorites(event: MouseEvent, bookId: number) {
    event.stopPropagation();
    event.preventDefault();

    try {
      await this.bookService.removeFromFavorites(bookId);
      this.favoriteBooks = this.favoriteBooks.filter(
        (book) => book.id !== bookId,
      );
    } catch {
      // Keep silent here as well.
    }
  }

  private async loadFavoriteBooks() {
    try {
      const favorites = await this.bookService.getUserFavorites();
      this.favoriteBooks = favorites.map((item) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || "",
      }));
    } catch {
      this.favoriteBooks = [];
    } finally {
      this.currentFavoritePage = 1;
    }
  }

  getAvailableCopiesCount(book: ProfileBookCard): number {
    return book.availableCopies ?? 0;
  }

  getTotalCopiesCount(book: ProfileBookCard): number {
    return book.totalCopies ?? 0;
  }

  isBookUnavailable(book: ProfileBookCard): boolean {
    return (
      this.getAvailableCopiesCount(book) === 0 &&
      this.getTotalCopiesCount(book) > 0
    );
  }

  async toggleNotification(event: MouseEvent, book: ProfileBookCard) {
    event.stopPropagation();
    event.preventDefault();

    if (!book.wishlistId) return;

    this.notificationToggleErrors[book.id] = "";

    const previousValue = book.notificationEnabled ?? false;
    const nextValue = !previousValue;
    book.notificationEnabled = nextValue;

    try {
      const updatedWishlist = await this.bookService.updateWishlistNotification(
        book.wishlistId,
        nextValue,
      );
      book.notificationEnabled =
        updatedWishlist.notificationEnabled ?? nextValue;
      this.notificationToggleErrors[book.id] = "";
    } catch (error) {
      book.notificationEnabled = previousValue;
      console.error("Failed to toggle notification:", error);
      const status = (error as any)?.response?.status;
      if (status === 400) {
        this.notificationToggleErrors[book.id] =
          "Kan niet aanzetten: boek is momenteel beschikbaar.";
      } else if (status === 401) {
        this.notificationToggleErrors[book.id] =
          "Niet ingelogd. Herlaad en probeer opnieuw.";
      } else {
        this.notificationToggleErrors[book.id] =
          "Melding aanpassen mislukt. Probeer opnieuw.";
      }
    }
  }

  goBack() {
    this.location.back();
  }

  toggleSettings() {
    this.settingsOpen = !this.settingsOpen;
  }

  saveDashboardSettings() {
    localStorage.setItem(
      "dashboardSettings",
      JSON.stringify(this.dashboardSettings),
    );
    this.settingsOpen = false;
  }

  @HostListener("document:click", ["$event"])
  clickOutside(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest(".settings-dropdown")) {
      this.settingsOpen = false;
    }
  }

  goToDetail(bookId: number, event?: MouseEvent) {
    const target = event?.target as HTMLElement | null;
    if (target?.closest("button")) {
      return;
    }
    this.router.navigate(["/detail", bookId]);
  }

  scrollToSection(sectionId: string) {
    const section = document.getElementById(sectionId);
    if (!section) {
      return;
    }

    section.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  testSmartschoolMessage(): void {
    this.smartschoolService
      .sendMessage(
        "Testbericht van GoSmartLib",
        "Dit is een testbericht verstuurd vanuit je profielpagina.",
      )
      .subscribe({
        next: () =>
          alert("Bericht succesvol verzonden! Check je Smartschool berichten."),
        error: (err) => {
          console.error(err);
          alert("Er ging iets mis bij het versturen van het bericht.");
        },
      });
  }
}
