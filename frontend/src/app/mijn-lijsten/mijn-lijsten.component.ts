import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { BookService } from '../services/book.service';
import { LoanService, Loan } from '../services/loan.service';
import { SchoolService } from '../services/school.service';
import { RecommendedBook } from '../services/recommendation.service';

type Tab = 'geleend' | 'verlanglijst' | 'klasleeslijst' | 'kijker' | 'historiek';

const VALID_TABS: Tab[] = ['geleend', 'verlanglijst', 'klasleeslijst', 'kijker', 'historiek'];

@Component({
  selector: 'app-mijn-lijsten',
  templateUrl: './mijn-lijsten.component.html',
  styleUrls: ['./mijn-lijsten.component.css'],
  standalone: false,
})
export class MijnLijstenComponent implements OnInit {
  activeTab: Tab = 'geleend';

  loans: Loan[] = [];
  wishlistItems: any[] = [];
  classReadingBooks: RecommendedBook[] = [];
  highlightedBooks: RecommendedBook[] = [];
  loanHistory: Loan[] = [];

  loansLoading = true;
  wishlistLoading = true;
  classReadingLoading = true;
  highlightedLoading = true;
  historyLoading = true;

  today = new Date().toISOString().split('T')[0];

  get userSub(): string {
    return localStorage.getItem('sub') || '';
  }

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookService: BookService,
    private loanService: LoanService,
    private schoolService: SchoolService,
  ) {}

  ngOnInit(): void {
    const fragment = (this.route.snapshot.fragment || '') as Tab;
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
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.classReadingLoading = false;
      return;
    }
    this.classReadingLoading = true;
    try {
      const ids = await this.bookService.getClassReadingListItemIds(schoolId);
      if (ids?.length) {
        const enriched = await this.bookService.enrichBooksWithDetails(
          ids.map((id: number) => ({ bookId: id })),
        );
        this.classReadingBooks = enriched.map((b: any) => ({
          bookId: b.bookId,
          titel: b.titel,
          auteur: b.auteur,
          cover: b.cover || '',
          genre: b.genre,
          paginas: b.paginas,
          taal: b.taal,
        }) as RecommendedBook);
      }
    } catch {
      this.classReadingBooks = [];
    } finally {
      this.classReadingLoading = false;
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
        this.highlightedBooks = enriched.map((b: any) => ({
          bookId: b.bookId,
          titel: b.titel,
          auteur: b.auteur,
          cover: b.cover || '',
          genre: b.genre,
          paginas: b.paginas,
          taal: b.taal,
        }) as RecommendedBook);
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

  selectTab(tab: Tab): void {
    this.activeTab = tab;
    this.router.navigate([], { fragment: tab, replaceUrl: true });
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
    this.router.navigate(['/detail', bookId]);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('nl-BE', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  loanToBook(loan: Loan): any {
    return { bookId: loan.bookId, titel: loan.bookTitel, cover: loan.bookCover };
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

  async onToggleWishlistBell(item: any): Promise<void> {
    try {
      await this.bookService.updateWishlistNotification(
        item.bookId,
        !item.notificationEnabled,
      );
      item.notificationEnabled = !item.notificationEnabled;
    } catch {
      // keep current state on error
    }
  }
}
