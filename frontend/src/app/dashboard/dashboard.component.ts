import { Component, OnInit, HostListener, ViewChild, ElementRef } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../services/book.service";
import { LoanService, Loan } from "../services/loan.service";
import {
  RecommendationService,
  RecommendedBook,
} from "../services/recommendation.service";
import { HttpClient } from "@angular/common/http";
import { UserPreferencesService } from "../services/user-preferences.service";
import {
  DashboardConfig,
  DashboardConfigService,
  DEFAULT_DASHBOARD_CONFIG,
} from "../services/dashboard-config.service";
import { SettingsService, SchoolSettings, SchoolMessage, DayHours } from "../services/settings.service";
import { SchoolService } from "../services/school.service";
import { inferNameParts, composeFullName } from "../utils/name-utils";
import { CarouselPageDef } from "./carousel-tile.component";

export type { DashboardConfig };

interface SpotlightBook {
  bookId: number;
  titel: string;
  auteur: string;
  cover: string;
}

interface TileDef {
  id: string;
  label: string;
  dot: string;
  carousel: boolean;
  pages?: { id: string; label: string }[];
}

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
  selector: "app-dashboard",
  templateUrl: "./dashboard.component.html",
  styleUrls: ["./dashboard.component.css"],
  standalone: false,
})
export class DashboardComponent implements OnInit {
  trendingBooks: RecommendedBook[] = [];
  genreBooks: RecommendedBook[] = [];
  authorBooks: RecommendedBook[] = [];
  newArrivalsBooks: RecommendedBook[] = [];
  classReadingListBooks: RecommendedBook[] = [];
  highlightedBooks: RecommendedBook[] = [];
  myLoans: Loan[] = [];
  loanHistory: Loan[] = [];

  loansLoading = true;
  recommendationsLoading = true;
  highlightedLoading = false;
  spotlightLoading = true;
  wishlistCount = 0;
  klasleeslijstCount = 0;

  schoolSettings: SchoolSettings | null = null;
  activeMessages: SchoolMessage[] = [];
  currentMessageIdx = 0;
  messageAnimClass = '';
  private messageRotationTimer: any;
  private messageBusy = false;

  spotlight: { maand: SpotlightBook | null; thema: SpotlightBook | null } = { maand: null, thema: null };

  // Tile config
  readonly TILES = TILES;
  readonly DEFAULT_CONFIG = DEFAULT_DASHBOARD_CONFIG;
  config: DashboardConfig = DEFAULT_DASHBOARD_CONFIG;

  // Config popover state
  configOpen = false;
  hoursOpen = false;
  dragId: string | null = null;
  dragOverId: string | null = null;
  @ViewChild('configWrap') configWrapRef?: ElementRef<HTMLElement>;
  @ViewChild('hoursWrap') hoursWrapRef?: ElementRef<HTMLElement>;

  private readonly RECOMMENDATION_LIMIT = 25;
  today = new Date().toISOString().split("T")[0];

  get firstLoan(): Loan | null {
    return this.myLoans[0] ?? null;
  }

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Goeiemorgen';
    if (h < 18) return 'Goedemiddag';
    return 'Goeienavond';
  }

  get lastReturnedLoan(): Loan | null {
    return this.loanHistory[0] ?? null;
  }

  get currentFirstName(): string {
    const userName = (localStorage.getItem("userName") || "").trim();
    const firstName = (localStorage.getItem("firstName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();
    
    // Als userName 'Adrian' is en verschilt van de achternaam, is dit onze beste gok
    if (userName && userName.toLowerCase() !== lastName.toLowerCase()) return userName;
    if (firstName) return firstName;

    const nameCandidates = [
      userName,
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const { firstName: inferredFirst } = inferNameParts(
      firstName || null,
      lastName || null,
      nameCandidates,
    );
    return inferredFirst || userName || firstName || "Leerling";
  }

  get currentUsername(): string {
    const userName = (localStorage.getItem("userName") || "").trim();
    const lastName = (localStorage.getItem("lastName") || "").trim();

    // Als we Adrian en Dyszczak los hebben, plak ze dan direct aan elkaar
    if (userName && lastName && userName.toLowerCase() !== lastName.toLowerCase()) {
      return composeFullName(userName, lastName);
    }

    const firstName = (localStorage.getItem("firstName") || userName || "").trim();
    const composed = composeFullName(firstName, lastName);
    if (composed) return composed;

    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const { firstName: inferredFirst, lastName: inferredLast } = inferNameParts(
      firstName || null,
      lastName || null,
      nameCandidates,
    );
    return (
      composeFullName(inferredFirst, inferredLast) ||
      firstName ||
      lastName ||
      localStorage.getItem("userName") ||
      localStorage.getItem("fullname") ||
      ""
    );
  }

  get currentUserSub(): string {
    return localStorage.getItem("sub") || "";
  }

  constructor(
    private router: Router,
    private bookService: BookService,
    private loanService: LoanService,
    private recommendationService: RecommendationService,
    private http: HttpClient,
    private userPreferencesService: UserPreferencesService,
    private schoolService: SchoolService,
    private dashboardConfigService: DashboardConfigService,
    private settingsService: SettingsService,
  ) {}

  ngOnInit(): void {
    const role = localStorage.getItem('role');
    if (role === 'leerkracht' || role === 'bibbeheerder') {
      this.router.navigate(['/leerkracht-dashboard'], { replaceUrl: true });
      return;
    }

    this.userPreferencesService.preferences$.subscribe((prefs) => {
      this.fetchAllRecommendations({
        trending: prefs["recommendationExcludeRead_trending"] ?? true,
        genre: prefs["recommendationExcludeRead_genre"] ?? true,
        author: prefs["recommendationExcludeRead_author"] ?? true,
        newArrivals: prefs["recommendationExcludeRead_newArrivals"] ?? true,
      });
    });

    this.dashboardConfigService.config$.subscribe((cfg) => {
      this.config = cfg;
    });

    this.fetchMyLoans();
    this.fetchLoanHistory();
    this.fetchHighlightedBooks();
    this.fetchSpotlights();
    this.fetchWishlistCount();
    this.fetchKlasleeslijstCount();
    this.fetchSchoolSettings();
  }

  ngOnDestroy(): void {
    if (this.messageRotationTimer) {
      clearInterval(this.messageRotationTimer);
    }
  }

  // ── Config management ──

  updateConfig(next: DashboardConfig) {
    this.config = next;
    this.dashboardConfigService.save(next);
  }

  resetConfig() {
    this.updateConfig({
      ...DEFAULT_DASHBOARD_CONFIG,
      pages: { ...DEFAULT_DASHBOARD_CONFIG.pages },
    });
  }

  toggleTile(id: string) {
    const tiles = this.config.tiles.includes(id)
      ? this.config.tiles.filter(t => t !== id)
      : [...this.config.tiles, id];
    this.updateConfig({ ...this.config, tiles });
  }

  togglePage(tileId: string, pageId: string) {
    const cur = this.config.pages[tileId] ?? [];
    const next = cur.includes(pageId)
      ? cur.filter(p => p !== pageId)
      : [...cur, pageId];
    this.updateConfig({
      ...this.config,
      pages: { ...this.config.pages, [tileId]: next },
    });
  }

  isTileEnabled(id: string): boolean {
    return this.config.tiles.includes(id);
  }

  isPageEnabled(tileId: string, pageId: string): boolean {
    return (this.config.pages[tileId] ?? []).includes(pageId);
  }

  // Ordered tiles: enabled first (in config order), then disabled
  get orderedTiles(): TileDef[] {
    return [
      ...this.config.tiles.map(id => TILES.find(t => t.id === id)).filter((t): t is TileDef => !!t),
      ...TILES.filter(t => !this.config.tiles.includes(t.id)),
    ];
  }

  // ── Config popover ──

  toggleConfigOpen() {
    this.configOpen = !this.configOpen;
  }

  @HostListener('document:keydown.escape')
  closeConfig() {
    this.configOpen = false;
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMousedown(event: MouseEvent) {
    if (
      this.configOpen &&
      this.configWrapRef &&
      !this.configWrapRef.nativeElement.contains(event.target as Node)
    ) {
      this.configOpen = false;
    }
    if (
      this.hoursOpen &&
      this.hoursWrapRef &&
      !this.hoursWrapRef.nativeElement.contains(event.target as Node)
    ) {
      this.hoursOpen = false;
    }
  }

  // ── Drag-to-reorder ──

  onDragStart(id: string) {
    this.dragId = id;
  }

  onDragOver(event: DragEvent, id: string) {
    if (!this.dragId || this.dragId === id || !this.config.tiles.includes(id)) return;
    event.preventDefault();
    this.dragOverId = id;
  }

  onDrop(event: DragEvent, targetId: string) {
    event.preventDefault();
    if (!this.dragId || this.dragId === targetId) {
      this.dragId = null;
      this.dragOverId = null;
      return;
    }
    if (!this.config.tiles.includes(this.dragId) || !this.config.tiles.includes(targetId)) {
      this.dragId = null;
      this.dragOverId = null;
      return;
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

  onDragEnd() {
    this.dragId = null;
    this.dragOverId = null;
  }

  // ── Opening hours ──

  toggleHoursOpen() {
    this.hoursOpen = !this.hoursOpen;
  }

  get todayKey(): string {
    const days = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
    return days[new Date().getDay()];
  }

  get weekHours(): Array<{ label: string; key: string; hours: DayHours | null; isToday: boolean }> {
    const days = [
      { label: 'Maandag', key: 'mon' },
      { label: 'Dinsdag', key: 'tue' },
      { label: 'Woensdag', key: 'wed' },
      { label: 'Donderdag', key: 'thu' },
      { label: 'Vrijdag', key: 'fri' },
      { label: 'Zaterdag', key: 'sat' },
      { label: 'Zondag', key: 'sun' },
    ];
    const key = this.todayKey;
    return days.map(d => ({
      label: d.label,
      key: d.key,
      hours: this.schoolSettings?.hours?.[d.key as keyof typeof this.schoolSettings.hours] ?? null,
      isToday: d.key === key,
    }));
  }

  // ── Message carousel ──

  prevMessage() {
    if (this.activeMessages.length < 2 || this.messageBusy) return;
    this.messageBusy = true;
    this.messageAnimClass = 'slide-out-right';
    setTimeout(() => {
      this.currentMessageIdx = (this.currentMessageIdx - 1 + this.activeMessages.length) % this.activeMessages.length;
      this.messageAnimClass = 'slide-in-left';
      setTimeout(() => {
        this.messageAnimClass = '';
        this.messageBusy = false;
      }, 300);
    }, 180);
  }

  nextMessage() {
    if (this.activeMessages.length < 2 || this.messageBusy) return;
    this.messageBusy = true;
    this.messageAnimClass = 'slide-out-left';
    setTimeout(() => {
      this.currentMessageIdx = (this.currentMessageIdx + 1) % this.activeMessages.length;
      this.messageAnimClass = 'slide-in-right';
      setTimeout(() => {
        this.messageAnimClass = '';
        this.messageBusy = false;
      }, 300);
    }, 180);
  }

  // ── Carousel page builders ──

  getMijnBoekenPages(): CarouselPageDef[] {
    const enabledIds = this.config.pages['mijn-boeken'] ?? [];
    const pages: CarouselPageDef[] = [];

    if (enabledIds.includes('verder-lezen')) {
      if (this.firstLoan) {
        const dl = this.daysLeft(this.firstLoan.dueDate);
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
            id: this.firstLoan.bookId,
            title: this.firstLoan.bookTitel,
            author: '',
            cover: this.firstLoan.bookCover,
          },
          badge: {
            calendar: true,
            label: `Inleveren ${this.formatDueDate(this.firstLoan.dueDate)} · ${dl}d`,
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
    }

    if (enabledIds.includes('laatst-ingeleverd')) {
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
    }

    return pages;
  }

  getBibliotheekPages(): CarouselPageDef[] {
    const enabledIds = this.config.pages['bibliotheek'] ?? [];
    const pages: CarouselPageDef[] = [];

    if (enabledIds.includes('in-de-kijker')) {
      if (this.highlightedLoading) {
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
    }

    if (enabledIds.includes('boek-vd-maand')) {
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
          badge: { label: this.getCurrentMonthLabel() },
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
          badge: { label: this.getCurrentMonthLabel() },
        } : {
          id: 'boek-vd-maand',
          label: 'Boek van de maand',
          eyebrow: '◆ Boek van de maand',
          eyebrowColor: '#d4537e',
          infoTitle: 'Boek van de maand',
          infoBody: 'Elke maand kiest de bibbeheerder één titel die ze in de spotlight zetten. Een goed startpunt als je niet weet wat je wil lezen.',
          book: { id: 0, title: '', author: '' },
          badge: { label: this.getCurrentMonthLabel() },
          empty: true,
          emptyMessage: 'Nog niet ingesteld door de bibbeheerder.',
        });
      }
    }

    if (enabledIds.includes('themaboek')) {
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
    }

    return pages;
  }

  getCurrentMonthLabel(): string {
    return new Date().toLocaleDateString('nl-BE', { month: 'long', year: 'numeric' });
  }

  // ── Data fetching ──

  private async fetchHighlightedBooks() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    this.highlightedLoading = true;
    try {
      const ids = await this.bookService.getHighlightedBookIds(schoolId);
      const bookIds = ids || [];
      if (bookIds.length > 0) {
        const enriched = await this.bookService.enrichBooksWithDetails(
          bookIds.map((id: number) => ({ bookId: id })),
        );
        this.highlightedBooks = enriched.map((b: any) => ({
          bookId: b.bookId,
          titel: b.titel,
          auteur: b.auteur,
          cover: b.cover || "",
          genre: b.genre,
          paginas: b.paginas,
          taal: b.taal,
        }) as RecommendedBook);
      } else {
        this.highlightedBooks = [];
      }
    } catch (error) {
      console.error("Fout bij ophalen gemarkeerde boeken:", error);
    } finally {
      this.highlightedLoading = false;
    }
  }

  private async fetchSpotlights() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) {
      this.spotlightLoading = false;
      return;
    }
    try {
      const data = await this.http.get<{ maand: SpotlightBook | null; thema: SpotlightBook | null }>(
        `/api/spotlight/${schoolId}`
      ).toPromise();
      this.spotlight = data ?? { maand: null, thema: null };
    } catch {
      this.spotlight = { maand: null, thema: null };
    } finally {
      this.spotlightLoading = false;
    }
  }

  private fetchSchoolSettings() {
    const schoolId = this.schoolService.getSelectedSchoolId();
    if (!schoolId) return;

    this.settingsService.getSettings(schoolId).subscribe({
      next: (settings) => {
        this.schoolSettings = settings;
        const today = new Date().toISOString().slice(0, 10);
        this.activeMessages = (settings.messages || []).filter(m => 
          m.enabled && (!m.startsAt || m.startsAt <= today) && (!m.endsAt || m.endsAt >= today)
        );
        this.startMessageRotation();
      },
      error: (err) => console.error("Fout bij ophalen schoolinstellingen:", err)
    });
  }

  private startMessageRotation() {
    if (this.activeMessages.length < 2) return;
    this.messageRotationTimer = setInterval(() => this.nextMessage(), 20000);
  }

  get currentMessage(): SchoolMessage | null {
    return this.activeMessages[this.currentMessageIdx] || null;
  }

  get todayHours(): DayHours | null {
    if (!this.schoolSettings?.hours) return null;
    const days = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
    const todayKey = days[new Date().getDay()] as keyof typeof this.schoolSettings.hours;
    return this.schoolSettings.hours[todayKey] || null;
  }

  get todayHoursDisplay(): string {
    const h = this.todayHours;
    if (!h) return '';
    return h.open ? `Open: ${h.from} – ${h.to}` : 'Vandaag gesloten';
  }

  private async fetchWishlistCount() {
    try {
      const items = await this.bookService.getUserWishlist();
      this.wishlistCount = items?.length ?? 0;
    } catch {
      this.wishlistCount = 0;
    }
  }

  private async fetchKlasleeslijstCount(): Promise<void> {
    try {
      let klasInfo: any = null;
      try {
        klasInfo = await this.bookService.getUserKlas();
      } catch {
        klasInfo = null;
      }
      if (klasInfo?.klasId) {
        const lists = await this.bookService.getLeeslistenForKlas(klasInfo.klasId);
        this.klasleeslijstCount = lists?.length ?? 0;
      } else {
        this.klasleeslijstCount = 0;
      }
    } catch {
      this.klasleeslijstCount = 0;
    }
  }

  private async fetchAllRecommendations(excludeReadFlags: {
    trending: boolean;
    genre: boolean;
    author: boolean;
    newArrivals: boolean;
  }) {
    this.recommendationsLoading = true;
    try {
      const results = await Promise.all([
        this.recommendationService.getTrending(this.RECOMMENDATION_LIMIT, excludeReadFlags.trending),
        this.recommendationService.getByGenre(this.RECOMMENDATION_LIMIT, excludeReadFlags.genre),
        this.recommendationService.getByAuthor(this.RECOMMENDATION_LIMIT, excludeReadFlags.author),
        this.recommendationService.getNewArrivals(this.RECOMMENDATION_LIMIT, excludeReadFlags.newArrivals),
      ]);

      const bookSets = {
        trending: results[0],
        genre: results[1],
        author: results[2],
        newArrivals: results[3],
      };

      const enriched = await this.bookService.enrichMultipleBooksWithDetails(bookSets);
      this.trendingBooks = enriched["trending"];
      this.genreBooks = enriched["genre"];
      this.authorBooks = enriched["author"];
      this.newArrivalsBooks = enriched["newArrivals"];
    } catch (error) {
      console.error("Fout bij ophalen aanbevelingen:", error);
      this.trendingBooks = [];
      this.genreBooks = [];
      this.authorBooks = [];
      this.newArrivalsBooks = [];
    } finally {
      this.recommendationsLoading = false;
    }
  }

  async fetchMyLoans() {
    this.loansLoading = true;
    const userSub = this.currentUserSub;
    if (!userSub) {
      this.loansLoading = false;
      return;
    }
    try {
      this.myLoans = await this.loanService.getMyActiveLoans();
    } catch {
      this.myLoans = [];
    } finally {
      this.loansLoading = false;
    }
  }

  async fetchLoanHistory() {
    try {
      this.loanHistory = await this.loanService.getMyLoanHistory();
    } catch {
      this.loanHistory = [];
    }
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

  goToDetail(bookId: number) {
    this.router.navigate(["/detail", bookId]);
  }

  goToMijnLijsten(fragment?: string) {
    this.router.navigate(["/mijn-lijsten"], { fragment });
  }

  logout(): void {
    const accessToken = localStorage.getItem("smartschoolToken");
    if (accessToken) {
      this.http.post("/api/auth/logout", { accessToken }).subscribe({
        next: () => this.completeLogout(),
        error: () => this.completeLogout(),
      });
    } else {
      this.completeLogout();
    }
  }

  private completeLogout(): void {
    localStorage.clear();
    this.router.navigate(["/login"]);
  }

  formatDueDate(dueDate: string): string {
    return new Date(dueDate).toLocaleDateString("nl-BE", {
      day: "numeric",
      month: "long",
    });
  }

  formatReturnedDate(returnedAt: string): string {
    return new Date(returnedAt).toLocaleDateString("nl-BE", {
      day: "numeric",
      month: "long",
    });
  }
}
