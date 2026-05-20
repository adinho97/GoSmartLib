import { Component, OnInit, HostListener, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookService } from '../services/book.service';
import { LoanService, Loan } from '../services/loan.service';
import { RecommendationService, RecommendedBook } from '../services/recommendation.service';
import { SchoolService } from '../services/school.service';
import { UserPreferencesService } from '../services/user-preferences.service';
import {
  DashboardConfig,
  DashboardConfigService,
  DEFAULT_DASHBOARD_CONFIG,
} from '../services/dashboard-config.service';
import { inferNameParts } from '../utils/name-utils';
import { CarouselPageDef } from '../dashboard/carousel-tile.component';
import { SpotlightBook, SpotlightState } from '../spotlight-manage/spotlight-manage.component';

interface TileDef {
  id: string;
  label: string;
  dot: string;
  carousel: boolean;
  pages?: { id: string; label: string }[];
}

interface ShortcutDef {
  id: string;
  label: string;
  route: string;
  roles: string[];
  iconPaths: string[];
}

const SHORTCUT_CATALOG: ShortcutDef[] = [
  // Both roles — actions not reachable from the nav bar
  {
    id: 'uitleen',
    label: 'Boek uitlenen',
    route: '/uitleen',
    roles: ['leerkracht', 'bibbeheerder'],
    iconPaths: ['M12 5v14M5 12h14', 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20', 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z'],
  },
  {
    id: 'boek-terugbrengen',
    label: 'Boek terugbrengen',
    route: '/boek-terugbrengen',
    roles: ['leerkracht', 'bibbeheerder'],
    iconPaths: ['M9 14l-4-4 4-4', 'M5 10h11a4 4 0 0 1 0 8h-1'],
  },
  {
    id: 'leeslijst-create',
    label: 'Leeslijst aanmaken',
    route: '/leeslijst-create',
    roles: ['leerkracht', 'bibbeheerder'],
    iconPaths: [
      'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7',
      'M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z',
    ],
  },
  // Bibbeheerder only
  {
    id: 'add-general',
    label: 'Boek toevoegen',
    route: '/add-general',
    roles: ['bibbeheerder'],
    iconPaths: ['M12 5v14M5 12h14'],
  },
  {
    id: 'uitleen-overzicht',
    label: 'Uitleen overzicht',
    route: '/uitleen-overzicht',
    roles: ['bibbeheerder'],
    iconPaths: ['M3 3h18v18H3zM3 9h18M9 3v18'],
  },
  {
    id: 'teacher-promotion',
    label: 'Leerkracht promoten',
    route: '/teacher-promotion',
    roles: ['bibbeheerder'],
    iconPaths: [
      'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2',
      'M9 7a4 4 0 1 0 8 0 4 4 0 0 0-8 0',
      'M23 21v-2a4 4 0 0 0-3-3.87',
      'M16 3.13a4 4 0 0 1 0 7.75',
    ],
  },
];

const DEFAULT_SHORTCUTS_LEERKRACHT = ['uitleen', 'boek-terugbrengen', 'leeslijst-create'];
const DEFAULT_SHORTCUTS_BIBBEHEERDER = ['uitleen', 'boek-terugbrengen', 'add-general', 'uitleen-overzicht', 'teacher-promotion'];

const TILES: TileDef[] = [
  {
    id: 'mijn-boeken',
    label: 'Mijn boeken',
    dot: 'var(--brand)',
    carousel: true,
    pages: [
      { id: 'verder-lezen', label: 'Verder lezen' },
      { id: 'laatst-ingeleverd', label: 'Laatst ingeleverd' },
    ],
  },
  {
    id: 'bibliotheek',
    label: 'Bibliotheek',
    dot: '#b86a17',
    carousel: true,
    pages: [
      { id: 'in-de-kijker', label: 'In de kijker' },
      { id: 'boek-vd-maand', label: 'Boek van de maand' },
      { id: 'themaboek', label: 'Themaboek' },
    ],
  },
  {
    id: 'snelkoppelingen',
    label: 'Snelkoppelingen',
    dot: 'var(--ink-3)',
    carousel: false,
  },
];

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

  // Tile config
  readonly TILES = TILES;
  readonly DEFAULT_CONFIG = DEFAULT_DASHBOARD_CONFIG;
  config: DashboardConfig = DEFAULT_DASHBOARD_CONFIG;

  // Config popover state
  configOpen = false;
  dragId: string | null = null;
  dragOverId: string | null = null;
  @ViewChild('configWrap') configWrapRef?: ElementRef<HTMLElement>;

  // Shortcut customization
  shortcutPopOpen = false;
  @ViewChild('shortcutPopWrap') shortcutPopWrapRef?: ElementRef<HTMLElement>;

  // Spotlight data (read-only for the dashboard; managed by <app-spotlight-manage>)
  spotlight: SpotlightState = { maand: null, thema: null };
  spotlightLoading = true;
  ownSchoolId: number | null = null;

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

  get availableShortcuts(): ShortcutDef[] {
    const role = localStorage.getItem('role') || '';
    return SHORTCUT_CATALOG.filter(s => s.roles.includes(role));
  }

  private get defaultShortcuts(): string[] {
    return this.isLibrarian ? DEFAULT_SHORTCUTS_BIBBEHEERDER : DEFAULT_SHORTCUTS_LEERKRACHT;
  }

  get activeShortcutIds(): string[] {
    return this.config.shortcuts ?? this.defaultShortcuts;
  }

  get activeShortcuts(): ShortcutDef[] {
    const available = this.availableShortcuts;
    return this.activeShortcutIds
      .map(id => available.find(s => s.id === id))
      .filter((s): s is ShortcutDef => !!s);
  }

  get inactiveShortcuts(): ShortcutDef[] {
    const activeIds = new Set(this.activeShortcutIds);
    return this.availableShortcuts.filter(s => !activeIds.has(s.id));
  }

  toggleShortcutPop(): void {
    this.shortcutPopOpen = !this.shortcutPopOpen;
  }

  readonly maxShortcuts = 5;

  get shortcutsAtMax(): boolean {
    return this.activeShortcuts.length >= this.maxShortcuts;
  }

  addShortcut(id: string): void {
    if (this.shortcutsAtMax) return;
    this.updateConfig({ ...this.config, shortcuts: [...this.activeShortcutIds, id] });
  }

  removeShortcut(id: string): void {
    this.updateConfig({ ...this.config, shortcuts: this.activeShortcutIds.filter(s => s !== id) });
  }

  resetShortcuts(): void {
    this.updateConfig({ ...this.config, shortcuts: [...this.defaultShortcuts] });
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

  constructor(
    private router: Router,
    private http: HttpClient,
    private bookService: BookService,
    private loanService: LoanService,
    private recommendationService: RecommendationService,
    private schoolService: SchoolService,
    private userPreferencesService: UserPreferencesService,
    private dashboardConfigService: DashboardConfigService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.fetchAllLoans();
    this.fetchMyLoans();
    this.fetchLoanHistory();
    this.userPreferencesService.preferences$.subscribe(prefs => {
      this.fetchRecommendations(
        prefs['recommendationExcludeRead_trending'] ?? true,
        prefs['recommendationExcludeRead_newArrivals'] ?? true,
      );
    });
    this.dashboardConfigService.config$.subscribe(cfg => {
      this.config = cfg;
    });
    await this.ensureOwnSchoolId();
    this.fetchHighlightedBooks();
    this.fetchSpotlights();
  }

  // ── Config management ──

  updateConfig(next: DashboardConfig): void {
    this.config = next;
    this.dashboardConfigService.save(next);
  }

  resetConfig(): void {
    this.updateConfig({
      ...DEFAULT_DASHBOARD_CONFIG,
      pages: { ...DEFAULT_DASHBOARD_CONFIG.pages },
    });
  }

  toggleTile(id: string): void {
    const tiles = this.config.tiles.includes(id)
      ? this.config.tiles.filter(t => t !== id)
      : [...this.config.tiles, id];
    this.updateConfig({ ...this.config, tiles });
  }

  togglePage(tileId: string, pageId: string): void {
    const cur = this.config.pages[tileId] ?? [];
    const next = cur.includes(pageId)
      ? cur.filter(p => p !== pageId)
      : [...cur, pageId];
    this.updateConfig({ ...this.config, pages: { ...this.config.pages, [tileId]: next } });
  }

  isTileEnabled(id: string): boolean {
    return this.config.tiles.includes(id);
  }

  isPageEnabled(tileId: string, pageId: string): boolean {
    return (this.config.pages[tileId] ?? []).includes(pageId);
  }

  get orderedTiles(): TileDef[] {
    return [
      ...this.config.tiles.map(id => TILES.find(t => t.id === id)).filter((t): t is TileDef => !!t),
      ...TILES.filter(t => !this.config.tiles.includes(t.id)),
    ];
  }

  toggleConfigOpen(): void {
    this.configOpen = !this.configOpen;
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMousedown(event: MouseEvent): void {
    if (
      this.configOpen &&
      this.configWrapRef &&
      !this.configWrapRef.nativeElement.contains(event.target as Node)
    ) {
      this.configOpen = false;
    }
    if (
      this.shortcutPopOpen &&
      this.shortcutPopWrapRef &&
      !this.shortcutPopWrapRef.nativeElement.contains(event.target as Node)
    ) {
      this.shortcutPopOpen = false;
    }
  }

  // ── Drag-to-reorder ──

  onDragStart(id: string): void {
    this.dragId = id;
  }

  onDragOver(event: DragEvent, id: string): void {
    if (!this.dragId || this.dragId === id || !this.config.tiles.includes(id)) return;
    event.preventDefault();
    this.dragOverId = id;
  }

  onDrop(event: DragEvent, targetId: string): void {
    event.preventDefault();
    if (!this.dragId || this.dragId === targetId) { this.dragId = null; this.dragOverId = null; return; }
    if (!this.config.tiles.includes(this.dragId) || !this.config.tiles.includes(targetId)) {
      this.dragId = null; this.dragOverId = null; return;
    }
    const next = [...this.config.tiles];
    const from = next.indexOf(this.dragId);
    const to = next.indexOf(targetId);
    next.splice(from, 1);
    next.splice(to, 0, this.dragId);
    this.updateConfig({ ...this.config, tiles: next });
    this.dragId = null;
    this.dragOverId = null;
  }

  onDragEnd(): void {
    this.dragId = null;
    this.dragOverId = null;
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    this.configOpen = false;
    this.shortcutPopOpen = false;
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
    const schoolId = this.ownSchoolId;
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
    const enabledIds = this.config.pages['mijn-boeken'] ?? [];
    const pages: CarouselPageDef[] = [];

    if (enabledIds.includes('verder-lezen') && this.firstOwnLoan) {
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
    } else if (enabledIds.includes('verder-lezen') && !this.loansLoading) {
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

    if (enabledIds.includes('laatst-ingeleverd') && this.lastReturnedLoan) {
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
    } else if (enabledIds.includes('laatst-ingeleverd') && !this.loansLoading) {
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
    const enabledIds = this.config.pages['bibliotheek'] ?? [];
    const pages: CarouselPageDef[] = [];

    if (enabledIds.includes('in-de-kijker') && this.booksLoading) {
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
    } else if (enabledIds.includes('in-de-kijker') && this.highlightedBooks.length > 0) {
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
    } else if (enabledIds.includes('in-de-kijker')) {
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
    if (enabledIds.includes('boek-vd-maand') && this.spotlightLoading) {
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
    } else if (enabledIds.includes('boek-vd-maand')) {
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
    if (enabledIds.includes('themaboek') && this.spotlightLoading) {
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
    } else if (enabledIds.includes('themaboek')) {
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
    const schoolId = this.ownSchoolId;
    if (!schoolId) {
      this.spotlightLoading = false;
      return;
    }
    this.spotlightLoading = true;
    this.http.get<SpotlightState>(`/api/spotlight/${schoolId}`).subscribe({
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

  onSpotlightSaved({ type, book }: { type: 'MAAND' | 'THEMA'; book: SpotlightBook }): void {
    this.spotlight = {
      ...this.spotlight,
      [type === 'MAAND' ? 'maand' : 'thema']: book,
    };
  }

  onSpotlightCleared(type: 'MAAND' | 'THEMA'): void {
    this.spotlight = {
      ...this.spotlight,
      [type === 'MAAND' ? 'maand' : 'thema']: null,
    };
  }

  onHighlightsAdded(): void {
    this.fetchHighlightedBooks();
  }

  private async ensureOwnSchoolId(): Promise<void> {
    if (this.ownSchoolId) return;
    this.ownSchoolId = this.schoolService.getUserOwnSchoolId();
    if (this.ownSchoolId) return;
    await this.schoolService.selectUserDefaultSchool();
    this.ownSchoolId = this.schoolService.getUserOwnSchoolId();
  }

  goToBooks(): void {
    this.router.navigate(['/books']);
  }

  goToAddBook(): void {
    this.router.navigate(['/add-general']);
  }
}
