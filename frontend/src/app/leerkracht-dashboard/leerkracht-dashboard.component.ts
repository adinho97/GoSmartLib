import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookService } from '../services/book.service';
import { LoanService, Loan } from '../services/loan.service';
import { RecommendationService, RecommendedBook } from '../services/recommendation.service';
import { SchoolService } from '../services/school.service';
import { UserPreferencesService } from '../services/user-preferences.service';
import { inferNameParts } from '../utils/name-utils';
import { CarouselPageDef } from '../dashboard/carousel-tile.component';

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
  loanHistory: Loan[] = [];
  highlightedBooks: RecommendedBook[] = [];
  highlightedBookIds = new Set<number>();
  trendingBooks: RecommendedBook[] = [];
  newArrivalsBooks: RecommendedBook[] = [];

  loansLoading = true;
  booksLoading = true;
  recommendationsLoading = true;

  // Spotlight management (bibbeheerder only)
  spotlight: { maand: SpotlightBook | null; thema: SpotlightBook | null } = { maand: null, thema: null };
  spotlightLoading = true;
  spotlightSaving: 'MAAND' | 'THEMA' | null = null;
  spotlightClearing: 'MAAND' | 'THEMA' | null = null;

  // Highlight (In de kijker) picker
  highlightPickerOpen = false;
  highlightQuery = '';
  highlightResults: BookSearchResult[] = [];
  highlightSearching = false;
  highlightSaving = false;
  highlightError = '';
  highlightSelectedIds = new Set<number>();
  highlightSelectedBooks: BookSearchResult[] = [];
  private highlightDebounce: ReturnType<typeof setTimeout> | null = null;

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

  get lastReturnedLoan(): Loan | null {
    return this.loanHistory[0] ?? null;
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
    this.fetchLoanHistory();
    this.fetchHighlightedBooks();
    this.userPreferencesService.preferences$.subscribe(prefs => {
      this.fetchRecommendations(
        prefs['recommendationExcludeRead_trending'] ?? true,
        prefs['recommendationExcludeRead_newArrivals'] ?? true,
      );
    });
    this.fetchSpotlights();
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    this.closePicker();
    this.closeHighlightPicker();
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
      this.highlightedBookIds = new Set(ids || []);
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
      this.highlightedBookIds = new Set();
    } finally {
      this.booksLoading = false;
    }
  }

  private async fetchLoanHistory(): Promise<void> {
    try {
      this.loanHistory = await this.loanService.getMyLoanHistory();
    } catch {
      this.loanHistory = [];
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

  formatReturnedDate(returnedAt: string): string {
    return new Date(returnedAt).toLocaleDateString('nl-BE', { day: 'numeric', month: 'long' });
  }

  getMijnBoekenPages(): CarouselPageDef[] {
    const pages: CarouselPageDef[] = [];

    if (this.firstOwnLoan) {
      const dl = this.daysLeft(this.firstOwnLoan.dueDate);
      pages.push({
        id: 'verder-lezen',
        label: 'Verder lezen',
        eyebrow: 'Verder lezen',
        pulse: true,
        infoTitle: 'Verder lezen',
        infoBody: 'Het boek dat je nu in huis hebt. Hier zie je wanneer je het moet inleveren.',
        linkLabel: `Geleend (${this.myLoans.length}) →`,
        linkFragment: 'geleend',
        book: {
          id: this.firstOwnLoan.bookId,
          title: this.firstOwnLoan.bookTitel,
          author: '',
          cover: this.firstOwnLoan.bookCover,
        },
        badge: {
          calendar: true,
          label: `Inleveren ${this.formatDueDate(this.firstOwnLoan.dueDate)} · ${dl}d`,
          tone: dl <= 3 ? 'urgent' : dl <= 7 ? 'warn' : '',
        },
      });
    } else if (!this.loansLoading) {
      pages.push({
        id: 'verder-lezen',
        label: 'Verder lezen',
        eyebrow: 'Verder lezen',
        pulse: true,
        infoTitle: 'Verder lezen',
        infoBody: 'Het boek dat je nu in huis hebt. Hier zie je wanneer je het moet inleveren.',
        linkLabel: `Geleend (${this.myLoans.length}) →`,
        linkFragment: 'geleend',
        book: { id: 0, title: '', author: '' },
        empty: true,
        emptyMessage: 'Je hebt momenteel geen geleende boeken.',
        emptyCta: { label: 'Ontdek boeken →', route: '/books' },
      });
    }

    if (this.lastReturnedLoan) {
      const returned = this.lastReturnedLoan;
      pages.push({
        id: 'laatst-ingeleverd',
        label: 'Laatst ingeleverd',
        eyebrow: '↩ Laatst ingeleverd',
        eyebrowColor: '#2d5a78',
        infoTitle: 'Laatst ingeleverd',
        infoBody: 'Het boek dat jij het meest recent terugbracht. Handig om een review achter te laten of een gelijkaardige titel te zoeken.',
        linkLabel: `Historiek (${this.loanHistory.length}) →`,
        linkFragment: 'historiek',
        book: {
          id: returned.bookId,
          title: returned.bookTitel,
          author: '',
          cover: returned.bookCover,
        },
        badge: returned.returnedAt
          ? { calendar: true, label: `Ingeleverd ${this.formatReturnedDate(returned.returnedAt)}` }
          : undefined,
        headerCta: { label: 'Schrijf review', bookId: returned.bookId },
      });
    } else if (!this.loansLoading) {
      pages.push({
        id: 'laatst-ingeleverd',
        label: 'Laatst ingeleverd',
        eyebrow: '↩ Laatst ingeleverd',
        eyebrowColor: '#2d5a78',
        infoTitle: 'Laatst ingeleverd',
        infoBody: 'Het boek dat jij het meest recent terugbracht. Handig om een review achter te laten of een gelijkaardige titel te zoeken.',
        book: { id: 0, title: '', author: '' },
        empty: true,
        emptyMessage: 'Je hebt nog geen boeken ingeleverd.',
      });
    }

    return pages;
  }

  getBibliotheekPages(): CarouselPageDef[] {
    const pages: CarouselPageDef[] = [];

    if (this.booksLoading) {
      pages.push({
        id: 'in-de-kijker',
        label: 'In de kijker',
        eyebrow: '★ In de kijker',
        eyebrowColor: '#b86a17',
        infoTitle: 'In de kijker',
        infoBody: 'Boeken die je bibbeheerder extra in de spotlight zet. Vaak gaat het om bijzondere aanwinsten of titels die ergens bij passen.',
        book: { id: 0, title: '', author: '' },
        empty: true,
        emptyMessage: 'Laden…',
      });
    } else if (this.highlightedBooks.length > 0) {
      const book = this.highlightedBooks[0];
      pages.push({
        id: 'in-de-kijker',
        label: 'In de kijker',
        eyebrow: '★ In de kijker',
        eyebrowColor: '#b86a17',
        infoTitle: 'In de kijker',
        infoBody: 'Boeken die je bibbeheerder extra in de spotlight zet. Vaak gaat het om bijzondere aanwinsten of titels die ergens bij passen.',
        linkLabel: `Alles (${this.highlightedBooks.length}) →`,
        linkFragment: 'kijker',
        book: {
          id: book.bookId,
          title: book.titel,
          author: '',
          cover: book.cover ?? undefined,
        },
        badge: { label: book.genre || 'Uitgelicht' },
      });
    } else {
      pages.push({
        id: 'in-de-kijker',
        label: 'In de kijker',
        eyebrow: '★ In de kijker',
        eyebrowColor: '#b86a17',
        infoTitle: 'In de kijker',
        infoBody: 'Boeken die je bibbeheerder extra in de spotlight zet. Vaak gaat het om bijzondere aanwinsten of titels die ergens bij passen.',
        linkLabel: `Alles (${this.highlightedBooks.length}) →`,
        linkFragment: 'kijker',
        book: { id: 0, title: '', author: '' },
        empty: true,
        emptyMessage: 'Geen uitgelichte boeken op dit moment.',
      });
    }

    const maand = this.spotlight.maand;
    if (this.spotlightLoading) {
      pages.push({
        id: 'boek-vd-maand',
        label: 'Boek van de maand',
        eyebrow: '◆ Boek van de maand',
        eyebrowColor: '#d4537e',
        infoTitle: 'Boek van de maand',
        infoBody: 'Elke maand kiest de bibbeheerder één titel die ze in de spotlight zetten. Een goed startpunt als je niet weet wat je wil lezen.',
        book: { id: 0, title: '', author: '' },
        badge: { label: this.currentMonthLabel },
        empty: true,
        emptyMessage: 'Laden…',
      });
    } else {
      pages.push(maand ? {
        id: 'boek-vd-maand',
        label: 'Boek van de maand',
        eyebrow: '◆ Boek van de maand',
        eyebrowColor: '#d4537e',
        infoTitle: 'Boek van de maand',
        infoBody: 'Elke maand kiest de bibbeheerder één titel die ze in de spotlight zetten. Een goed startpunt als je niet weet wat je wil lezen.',
        book: { id: maand.bookId, title: maand.titel, author: '', cover: maand.cover || undefined },
        badge: { label: this.currentMonthLabel },
      } : {
        id: 'boek-vd-maand',
        label: 'Boek van de maand',
        eyebrow: '◆ Boek van de maand',
        eyebrowColor: '#d4537e',
        infoTitle: 'Boek van de maand',
        infoBody: 'Elke maand kiest de bibbeheerder één titel die ze in de spotlight zetten. Een goed startpunt als je niet weet wat je wil lezen.',
        book: { id: 0, title: '', author: '' },
        badge: { label: this.currentMonthLabel },
        empty: true,
        emptyMessage: 'Nog niet ingesteld door de bibbeheerder.',
      });
    }

    const thema = this.spotlight.thema;
    if (this.spotlightLoading) {
      pages.push({
        id: 'themaboek',
        label: 'Themaboek',
        eyebrow: '♦ Themaboek',
        eyebrowColor: '#2e6b3f',
        infoTitle: 'Themaboek',
        infoBody: 'Een boek dat past bij het lopende thema in de klas of op school. Wisselt om de paar weken.',
        book: { id: 0, title: '', author: '' },
        empty: true,
        emptyMessage: 'Laden…',
      });
    } else {
      pages.push(thema ? {
        id: 'themaboek',
        label: 'Themaboek',
        eyebrow: '♦ Themaboek',
        eyebrowColor: '#2e6b3f',
        infoTitle: 'Themaboek',
        infoBody: 'Een boek dat past bij het lopende thema in de klas of op school. Wisselt om de paar weken.',
        book: { id: thema.bookId, title: thema.titel, author: '', cover: thema.cover || undefined },
      } : {
        id: 'themaboek',
        label: 'Themaboek',
        eyebrow: '♦ Themaboek',
        eyebrowColor: '#2e6b3f',
        infoTitle: 'Themaboek',
        infoBody: 'Een boek dat past bij het lopende thema in de klas of op school. Wisselt om de paar weken.',
        book: { id: 0, title: '', author: '' },
        empty: true,
        emptyMessage: 'Nog niet ingesteld door de bibbeheerder.',
      });
    }

    return pages;
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
    if (!schoolId) {
      this.spotlightLoading = false;
      return;
    }
    this.spotlightLoading = true;
    this.http.get<{ maand: SpotlightBook | null; thema: SpotlightBook | null }>(
      `/api/spotlight/${schoolId}`
    ).subscribe({
      next: (data) => {
        this.spotlight = data ?? { maand: null, thema: null };
        this.spotlightLoading = false;
      },
      error: () => {
        this.spotlight = { maand: null, thema: null };
        this.spotlightLoading = false;
      },
    });
  }

  openHighlightPicker(): void {
    this.highlightPickerOpen = true;
    this.highlightQuery = '';
    this.highlightResults = [];
    this.highlightSearching = false;
    this.highlightSaving = false;
    this.highlightError = '';
    this.highlightSelectedIds = new Set();
    this.highlightSelectedBooks = [];
    setTimeout(() => document.getElementById('hk-search-input')?.focus(), 50);
  }

  closeHighlightPicker(): void {
    this.highlightPickerOpen = false;
    if (this.highlightDebounce !== null) {
      clearTimeout(this.highlightDebounce);
      this.highlightDebounce = null;
    }
  }

  onHighlightSearch(): void {
    if (this.highlightDebounce !== null) clearTimeout(this.highlightDebounce);
    if (!this.highlightQuery.trim()) {
      this.highlightResults = [];
      return;
    }
    this.highlightDebounce = setTimeout(() => this.searchHighlightBooks(), 300);
  }

  private searchHighlightBooks(): void {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId || !this.highlightQuery.trim()) return;
    this.highlightSearching = true;
    this.http.get<{ items: BookSearchResult[]; total: number }>(
      `/api/books/paged?schoolId=${schoolId}&query=${encodeURIComponent(this.highlightQuery.trim())}&size=8&page=0`
    ).subscribe({
      next: (res) => {
        this.highlightResults = res.items;
        this.highlightSearching = false;
      },
      error: () => {
        this.highlightResults = [];
        this.highlightSearching = false;
      },
    });
  }

  isHighlightLocked(bookId?: number): boolean {
    return !!bookId && this.highlightedBookIds.has(bookId);
  }

  isHighlightSelected(bookId?: number): boolean {
    return !!bookId && this.highlightSelectedIds.has(bookId);
  }

  toggleHighlightSelection(book: BookSearchResult): void {
    if (!book?.id || this.isHighlightLocked(book.id)) return;
    if (this.highlightSelectedIds.has(book.id)) {
      this.highlightSelectedIds.delete(book.id);
      this.highlightSelectedBooks = this.highlightSelectedBooks.filter(
        (b) => b.id !== book.id,
      );
      return;
    }
    this.highlightSelectedIds.add(book.id);
    this.highlightSelectedBooks = [...this.highlightSelectedBooks, book];
  }

  removeHighlightSelection(bookId: number): void {
    this.highlightSelectedIds.delete(bookId);
    this.highlightSelectedBooks = this.highlightSelectedBooks.filter(
      (b) => b.id !== bookId,
    );
  }

  async confirmHighlightAdd(): Promise<void> {
    const ids = Array.from(this.highlightSelectedIds).filter(
      (id) => !this.highlightedBookIds.has(id),
    );
    if (ids.length === 0) return;
    this.highlightSaving = true;
    this.highlightError = '';
    try {
      for (const id of ids) {
        await this.bookService.toggleHighlight(id);
        this.highlightedBookIds.add(id);
      }
      await this.fetchHighlightedBooks();
      this.closeHighlightPicker();
    } catch {
      this.highlightError = "Toevoegen aan 'In de kijker' mislukt.";
    } finally {
      this.highlightSaving = false;
    }
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
