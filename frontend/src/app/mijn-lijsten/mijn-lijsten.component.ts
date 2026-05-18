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
  | "didactisch"
  | "kijker"
  | "historiek";

const VALID_TABS: Tab[] = [
  "geleend",
  "verlanglijst",
  "klasleeslijst",
  "didactisch",
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
  isGlobal?: boolean;
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
  didacticBooks: RecommendedBook[] = [];
  highlightedBooks: RecommendedBook[] = [];
  loanHistory: Loan[] = [];

  loansLoading = true;
  wishlistLoading = true;
  classReadingLoading = true;
  leeslistenLoading = true;
  didacticLoading = true;
  highlightedLoading = true;
  historyLoading = true;

  today = new Date().toISOString().split("T")[0];

  get userSub(): string {
    return localStorage.getItem("sub") || "";
  }

  get rawUserRoles(): string {
    return localStorage.getItem("role") || "";
  }

  hasRole(roleName: string): boolean {
    const raw = this.rawUserRoles || "";
    return raw
      .split(/[;,|\s]+/)
      .map((r) => r.trim().toLowerCase())
      .filter(Boolean)
      .includes(roleName.toLowerCase());
  }

  get canCreateLeeslijst(): boolean {
    return this.hasRole("leerkracht") || this.hasRole("bibbeheerder");
  }

  get canViewDidacticCollection(): boolean {
    return (
      this.hasRole("leerkracht") ||
      this.hasRole("bibbeheerder") ||
      this.hasRole("super_admin")
    );
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
    if (this.activeTab === "didactisch" && !this.canViewDidacticCollection) {
      this.activeTab = "geleend";
    }
    this.loadAll();
  }

  private async loadAll(): Promise<void> {
    if (!this.canViewDidacticCollection) {
      this.didacticBooks = [];
      this.didacticLoading = false;
    }
    await Promise.all([
      this.loadLoans(),
      this.loadWishlist(),
      this.loadClassReading(),
      this.canViewDidacticCollection
        ? this.loadDidacticCollection()
        : Promise.resolve(),
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
      let klasLists: any[] = [];
      let myLists: any[] = [];

      const schoolId = this.schoolService.getSelectedSchoolId();

      // Always fetch all leeslisten the user should see (including global ones)
      try {
        myLists = await this.bookService.getMyLeeslisten();
      } catch (error) {
        console.error("Failed to fetch user's leeslisten:", error);
        myLists = [];
      }

      // Merge all leeslisten by ID (deduplicate)
      const mergedMap = new Map<number, any>();
      (myLists || []).forEach((l: any) => {
        if (l && l.id) mergedMap.set(Number(l.id), l);
      });
      (myLists || []).forEach((l: any) => {
        if (l && l.id) mergedMap.set(Number(l.id), l);
      });

      this.leeslisten = Array.from(mergedMap.values());
      this.classReadingBooks = [];
    } catch (error) {
      console.error("Error loading class reading lists:", error);
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

  private async loadDidacticCollection(): Promise<void> {
    this.didacticLoading = true;
    try {
      const books = await this.bookService.getDidacticBooks();
      this.didacticBooks = (books || []).map(
        (book: any) =>
          ({
            bookId: book.id,
            titel: book.titel,
            auteur: book.auteur,
            cover: book.cover || "",
            genre: book.genre,
            paginas: book.paginas,
            taal: book.taal,
            score: 0,
            reason: "",
          }) as RecommendedBook,
      );
    } catch {
      this.didacticBooks = [];
    } finally {
      this.didacticLoading = false;
    }
  }

  private async loadHistory(): Promise<void> {
    this.historyLoading = true;
    try {
      const history = await this.loanService.getMyLoanHistory();
      this.loanHistory = history.sort((a, b) => {
        const dateA = new Date(a.returnedAt || a.dueDate).getTime();
        const dateB = new Date(b.returnedAt || b.dueDate).getTime();
        return dateB - dateA; // Sort descending: newest (higher timestamp) first
      });
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

  displayCreatorName(createdByName: string): string {
    const currentSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    if (createdByName && createdByName !== currentSub) {
      return createdByName;
    }

    const fallbackName =
      localStorage.getItem("userName") ||
      localStorage.getItem("fullname") ||
      [localStorage.getItem("firstName"), localStorage.getItem("lastName")]
        .filter(Boolean)
        .join(" ") ||
      createdByName ||
      currentSub;

    return fallbackName;
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
