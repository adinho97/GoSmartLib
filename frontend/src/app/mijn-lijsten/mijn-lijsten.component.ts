import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import { SchoolService } from "../services/school.service";
import { RecommendedBook } from "../services/recommendation.service";

type Tab =
  | "geleend"
  | "verlanglijst"
  | "klasleeslijst"
  | "kijker"
  | "historiek";

const VALID_TABS: Tab[] = [
  "geleend",
  "verlanglijst",
  "klasleeslijst",
  "kijker",
  "historiek",
];

interface Leeslijst {
  id: number;
  titel: string;
  description?: string;
  createdByName: string;
  createdAt: string;
  books: any[];
  klasNames: string[];
}

@Component({
  selector: "app-mijn-lijsten",
  templateUrl: "./mijn-lijsten.component.html",
  styleUrls: ["./mijn-lijsten.component.css"],
  standalone: false,
})
export class MijnLijstenComponent implements OnInit {
  activeTab: Tab = "geleend";

  loans: Loan[] = [];
  wishlistItems: any[] = [];
  classReadingBooks: RecommendedBook[] = [];
  leeslisten: Leeslijst[] = [];
  highlightedBooks: RecommendedBook[] = [];
  loanHistory: Loan[] = [];

  loansLoading = true;
  wishlistLoading = true;
  classReadingLoading = true;
  leeslistenLoading = true;
  highlightedLoading = true;
  historyLoading = true;

  today = new Date().toISOString().split("T")[0];

  get userSub(): string {
    return localStorage.getItem("sub") || "";
  }

  get userRole(): string {
    return localStorage.getItem("role") || "";
  }

  get canCreateLeeslijst(): boolean {
    return this.userRole === "leerkracht" || this.userRole === "bibbeheerder";
  }

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookService: BookService,
    private loanService: LoanService,
    private schoolService: SchoolService,
  ) {}

  ngOnInit(): void {
    const fragment = (this.route.snapshot.fragment || "") as Tab;
    if (VALID_TABS.includes(fragment)) {
      this.activeTab = fragment;
    }
    this.loadAll();
  }

  private async loadAll(): Promise<void> {
    await Promise.all([
      this.loadLoans(),
      this.loadWishlist(),
      this.loadClassReading(),
      this.loadHighlighted(),
      this.loadHistory(),
    ]);
  }

  private async loadLoans(): Promise<void> {
    this.loansLoading = true;
    try {
      if (this.userSub) {
        this.loans = await this.loanService.getActiveLoans(this.userSub);
      }
    } catch {
      this.loans = [];
    } finally {
      this.loansLoading = false;
    }
  }

  private async loadWishlist(): Promise<void> {
    this.wishlistLoading = true;
    try {
      this.wishlistItems = await this.bookService.getUserWishlist();
    } catch {
      this.wishlistItems = [];
    } finally {
      this.wishlistLoading = false;
    }
  }

  private async loadClassReading(): Promise<void> {
    this.classReadingLoading = true;
    this.leeslistenLoading = true;
    try {
      // Get user's klas ID
      const klasInfo = await this.bookService.getUserKlas();
      if (!klasInfo || !klasInfo.klasId) {
        this.leeslisten = [];
        this.classReadingBooks = [];
        return;
      }

      // Fetch leeslisten for the user's klas
      this.leeslisten = await this.bookService.getLeeslistenForKlas(
        klasInfo.klasId,
      );
    } catch {
      this.leeslisten = [];
      this.classReadingBooks = [];
    } finally {
      this.classReadingLoading = false;
      this.leeslistenLoading = false;
    }
  }

  private async loadHighlighted(): Promise<void> {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.highlightedLoading = false;
      return;
    }
    this.highlightedLoading = true;
    try {
      const ids = await this.bookService.getHighlightedBookIds(schoolId);
      if (ids?.length) {
        const enriched = await this.bookService.enrichBooksWithDetails(
          ids.map((id: number) => ({ bookId: id })),
        );
        this.highlightedBooks = enriched.map(
          (b: any) =>
            ({
              bookId: b.bookId,
              titel: b.titel,
              auteur: b.auteur,
              cover: b.cover || "",
              genre: b.genre,
              paginas: b.paginas,
              taal: b.taal,
            }) as RecommendedBook,
        );
      }
    } catch {
      this.highlightedBooks = [];
    } finally {
      this.highlightedLoading = false;
    }
  }

  private async loadHistory(): Promise<void> {
    this.historyLoading = true;
    try {
      this.loanHistory = await this.loanService.getMyLoanHistory();
    } catch {
      this.loanHistory = [];
    } finally {
      this.historyLoading = false;
    }
  }

  private readonly PAGE_SIZE = 12;
  visibleCount = this.PAGE_SIZE;

  selectTab(tab: Tab): void {
    this.activeTab = tab;
    this.visibleCount = this.PAGE_SIZE;
    this.router.navigate([], { fragment: tab, replaceUrl: true });
  }

  showMore(): void {
    this.visibleCount += this.PAGE_SIZE;
  }

  daysLeft(dueDate: string): number {
    const due = new Date(dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / 86400000);
  }

  isUrgent(dueDate: string): boolean {
    return this.daysLeft(dueDate) <= 14;
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  goToDetail(bookId: number): void {
    this.router.navigate(["/detail", bookId]);
  }

  goToLeeslijst(leeslijstId: number): void {
    this.router.navigate(["/leeslijst", leeslijstId]);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return "";
    return new Date(dateStr).toLocaleDateString("nl-BE", {
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  }

  loanToBook(loan: Loan): any {
    return {
      bookId: loan.bookId,
      titel: loan.bookTitel,
      cover: loan.bookCover,
      deadline: loan.dueDate,
    };
  }

  wishlistToBook(item: any): any {
    return {
      bookId: item.bookId,
      titel: item.titel,
      auteur: item.auteur,
      cover: item.cover,
      notificationEnabled: item.notificationEnabled,
    };
  }

  historyToBook(loan: Loan): any {
    return {
      bookId: loan.bookId,
      titel: loan.bookTitel,
      cover: loan.bookCover,
      deadline: loan.returnedAt,
    };
  }

  async onRemoveFromWishlist(item: any): Promise<void> {
    try {
      await this.bookService.removeFromWishlist(item.bookId);
      this.wishlistItems = this.wishlistItems.filter(
        (w) => w.bookId !== item.bookId,
      );
    } catch {
      // keep current state on error
    }
  }

  async onToggleWishlistBell(item: any): Promise<void> {
    try {
      await this.bookService.updateWishlistNotification(
        item.id,
        !item.notificationEnabled,
      );
      item.notificationEnabled = !item.notificationEnabled;
    } catch {
      // keep current state on error
    }
  }
}
