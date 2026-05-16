import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookService } from '../services/book.service';
import { LoanService, Loan } from '../services/loan.service';
import { RecommendationService, RecommendedBook } from '../services/recommendation.service';
import { SchoolService } from '../services/school.service';
import { UserPreferencesService } from '../services/user-preferences.service';
import { inferNameParts } from '../utils/name-utils';

interface SpotlightBook {
  bookId: number;
  titel: string;
  auteur: string;
  cover: string;
}

interface BookSearchResult {
  id: number;
  titel: string;
  auteur: string;
  cover: string | null;
}

@Component({
  selector: 'app-leerkracht-dashboard',
  templateUrl: './leerkracht-dashboard.component.html',
  styleUrls: ['./leerkracht-dashboard.component.css'],
  standalone: false,
})
export class LeerkrachtDashboardComponent implements OnInit {
  allActiveLoans: Loan[] = [];
  myLoans: Loan[] = [];
  highlightedBooks: RecommendedBook[] = [];
  trendingBooks: RecommendedBook[] = [];
  newArrivalsBooks: RecommendedBook[] = [];

  loansLoading = true;
  booksLoading = true;
  recommendationsLoading = true;

  // Spotlight management (bibbeheerder only)
  spotlight: { maand: SpotlightBook | null; thema: SpotlightBook | null } = { maand: null, thema: null };
  spotlightLoading = false;
  spotlightSaving: 'MAAND' | 'THEMA' | null = null;
  spotlightClearing: 'MAAND' | 'THEMA' | null = null;

  // Book picker modal
  pickerOpen = false;
  pickerType: 'MAAND' | 'THEMA' = 'MAAND';
  pickerQuery = '';
  pickerResults: BookSearchResult[] = [];
  pickerSearching = false;
  private pickerDebounce: ReturnType<typeof setTimeout> | null = null;

  private readonly RECOMMENDATION_LIMIT = 20;
  today = new Date().toISOString().split('T')[0];

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Goeiemorgen';
    if (h < 18) return 'Goedemiddag';
    return 'Goeienavond';
  }

  get currentFirstName(): string {
    const firstName = (localStorage.getItem('firstName') || '').trim();
    if (firstName) return firstName;
    const lastName = (localStorage.getItem('lastName') || '').trim();
    const nameCandidates = [
      localStorage.getItem('userName'),
      localStorage.getItem('fullname'),
      localStorage.getItem('name'),
    ];
    const { firstName: inferredFirst } = inferNameParts(firstName || null, lastName || null, nameCandidates);
    return inferredFirst || lastName || localStorage.getItem('userName') || 'Leerkracht';
  }

  get currentUserSub(): string {
    return localStorage.getItem('sub') || '';
  }

  get isLibrarian(): boolean {
    return localStorage.getItem('role') === 'bibbeheerder';
  }

  get currentMonthLabel(): string {
    return new Date().toLocaleDateString('nl-BE', { month: 'long', year: 'numeric' });
  }

  get overdueLoans(): Loan[] {
    return this.allActiveLoans.filter(l => l.dueDate < this.today);
  }

  get overdueCount(): number {
    return this.overdueLoans.length;
  }

  get totalActiveCount(): number {
    return this.allActiveLoans.length;
  }

  get firstOwnLoan(): Loan | null {
    return this.myLoans[0] ?? null;
  }

  get kijkerBook(): RecommendedBook | null {
    return this.highlightedBooks[0] ?? null;
  }

  constructor(
    private router: Router,
    private http: HttpClient,
    private bookService: BookService,
    private loanService: LoanService,
    private recommendationService: RecommendationService,
    private schoolService: SchoolService,
    private userPreferencesService: UserPreferencesService,
  ) {}

  ngOnInit(): void {
    this.fetchAllLoans();
    this.fetchMyLoans();
    this.fetchHighlightedBooks();
    this.userPreferencesService.preferences$.subscribe(prefs => {
      this.fetchRecommendations(
        prefs['recommendationExcludeRead_trending'] ?? true,
        prefs['recommendationExcludeRead_newArrivals'] ?? true,
      );
    });
    if (this.isLibrarian) {
      this.fetchSpotlights();
    }
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    this.closePicker();
  }

  private async fetchAllLoans(): Promise<void> {
    try {
      this.allActiveLoans = await this.loanService.getAllActiveLoans();
    } catch {
      this.allActiveLoans = [];
    } finally {
      this.loansLoading = false;
    }
  }

  private async fetchMyLoans(): Promise<void> {
    const userSub = this.currentUserSub;
    if (!userSub) return;
    try {
      this.myLoans = await this.loanService.getActiveLoans(userSub);
    } catch {
      this.myLoans = [];
    }
  }

  private async fetchHighlightedBooks(): Promise<void> {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.booksLoading = false;
      return;
    }
    this.booksLoading = true;
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
          score: 0,
          reason: '',
        }) as RecommendedBook);
      }
    } catch {
      this.highlightedBooks = [];
    } finally {
      this.booksLoading = false;
    }
  }

  private async fetchRecommendations(excludeTrending: boolean, excludeNewArrivals: boolean): Promise<void> {
    this.recommendationsLoading = true;
    try {
      const [trending, newArrivals] = await Promise.all([
        this.recommendationService.getTrending(this.RECOMMENDATION_LIMIT, excludeTrending),
        this.recommendationService.getNewArrivals(this.RECOMMENDATION_LIMIT, excludeNewArrivals),
      ]);
      const enriched = await this.bookService.enrichMultipleBooksWithDetails({ trending, newArrivals });
      this.trendingBooks = enriched['trending'];
      this.newArrivalsBooks = enriched['newArrivals'];
    } catch {
      this.trendingBooks = [];
      this.newArrivalsBooks = [];
    } finally {
      this.recommendationsLoading = false;
    }
  }

  getDaysOverdue(dueDate: string): number {
    const due = new Date(dueDate);
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    return Math.floor((now.getTime() - due.getTime()) / 86400000);
  }

  daysLeft(dueDate: string): number {
    const due = new Date(dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / 86400000);
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  isUrgent(dueDate: string): boolean {
    return this.daysLeft(dueDate) <= 14;
  }

  formatDueDate(dueDate: string): string {
    return new Date(dueDate).toLocaleDateString('nl-BE', { day: 'numeric', month: 'long' });
  }

  goToDetail(bookId: number): void {
    this.router.navigate(['/detail', bookId]);
  }

  goToMijnLijsten(): void {
    this.router.navigate(['/mijn-lijsten']);
  }

  onTrendingRefresh(excludeRead: boolean): void {
    this.fetchRecommendations(excludeRead, true);
  }

  onNewArrivalsRefresh(excludeRead: boolean): void {
    this.fetchRecommendations(true, excludeRead);
  }

  private fetchSpotlights(): void {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;
    this.spotlightLoading = true;
    this.http.get<{ maand: SpotlightBook | null; thema: SpotlightBook | null }>(
      `/api/spotlight/${schoolId}`
    ).subscribe({
      next: (data) => { this.spotlight = data; this.spotlightLoading = false; },
      error: () => { this.spotlightLoading = false; },
    });
  }

  openPicker(type: 'MAAND' | 'THEMA'): void {
    this.pickerType = type;
    this.pickerQuery = '';
    this.pickerResults = [];
    this.pickerSearching = false;
    this.pickerOpen = true;
    setTimeout(() => document.getElementById('sp-search-input')?.focus(), 50);
  }

  closePicker(): void {
    this.pickerOpen = false;
    if (this.pickerDebounce !== null) {
      clearTimeout(this.pickerDebounce);
      this.pickerDebounce = null;
    }
  }

  onPickerSearch(): void {
    if (this.pickerDebounce !== null) clearTimeout(this.pickerDebounce);
    if (!this.pickerQuery.trim()) { this.pickerResults = []; return; }
    this.pickerDebounce = setTimeout(() => this.searchBooks(), 300);
  }

  private searchBooks(): void {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId || !this.pickerQuery.trim()) return;
    this.pickerSearching = true;
    this.http.get<{ items: BookSearchResult[]; total: number }>(
      `/api/books/paged?schoolId=${schoolId}&query=${encodeURIComponent(this.pickerQuery.trim())}&size=8&page=0`
    ).subscribe({
      next: (res) => { this.pickerResults = res.items; this.pickerSearching = false; },
      error: () => { this.pickerResults = []; this.pickerSearching = false; },
    });
  }

  selectBook(book: BookSearchResult): void {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;
    this.spotlightSaving = this.pickerType;
    this.http.put<SpotlightBook>(`/api/spotlight/${schoolId}/${this.pickerType}`, { bookId: book.id }).subscribe({
      next: (saved) => {
        if (this.pickerType === 'MAAND') this.spotlight.maand = saved;
        else this.spotlight.thema = saved;
        this.spotlightSaving = null;
        this.closePicker();
      },
      error: () => { this.spotlightSaving = null; },
    });
  }

  clearSpotlight(type: 'MAAND' | 'THEMA'): void {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;
    this.spotlightClearing = type;
    this.http.delete(`/api/spotlight/${schoolId}/${type}`).subscribe({
      next: () => {
        if (type === 'MAAND') this.spotlight.maand = null;
        else this.spotlight.thema = null;
        this.spotlightClearing = null;
      },
      error: () => { this.spotlightClearing = null; },
    });
  }

  goToBooks(): void {
    this.router.navigate(['/books']);
  }

  goToAddBook(): void {
    this.router.navigate(['/add-general']);
  }
}
