import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { InfoButtonComponent } from '../components/info-button/info-button.component';

export interface CarouselPageDef {
  id: string;
  label: string;
  eyebrow: string;
  eyebrowColor?: string;
  pulse?: boolean;
  infoTitle?: string;
  infoBody?: string;
  linkLabel?: string;
  linkFragment?: string;
  book: {
    id: number;
    title: string;
    author: string;
    cover?: string;
  };
  badge?: {
    calendar?: boolean;
    label: string;
    tone?: 'urgent' | 'warn' | '';
  };
  primaryCta?: { label: string; bookId?: number; fragment?: string };
  secondaryCta?: { label: string; bookId?: number; fragment?: string };
  headerCta?: { label: string; bookId?: number; fragment?: string };
  empty?: boolean;
  emptyMessage?: string;
  emptyCta?: { label: string; route: string };
}

@Component({
  selector: 'app-carousel-tile',
  standalone: true,
  imports: [CommonModule, InfoButtonComponent],
  templateUrl: './carousel-tile.component.html',
  styleUrl: './carousel-tile.component.css',
})
export class CarouselTileComponent implements OnInit, OnDestroy, OnChanges {
  @Input() pages: CarouselPageDef[] = [];
  @Input() autoMs = 15000;

  currentIndex = 0;
  outgoing: { index: number; dir: 'next' | 'prev' } | null = null;

  private paused = false;
  private turning = false;
  private intervalId: ReturnType<typeof setInterval> | null = null;
  private readonly FLIP_DURATION = 680;

  constructor(
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.startInterval();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['pages']) {
      if (this.currentIndex >= this.pages.length) {
        this.currentIndex = 0;
      }
      this.stopInterval();
      this.startInterval();
    }
  }

  ngOnDestroy() {
    this.stopInterval();
  }

  get currentPage(): CarouselPageDef | null {
    return this.pages[this.currentIndex] ?? null;
  }

  get outgoingPage(): CarouselPageDef | null {
    return this.outgoing ? (this.pages[this.outgoing.index] ?? null) : null;
  }

  private startInterval() {
    if (this.pages.length <= 1) return;
    this.intervalId = setInterval(() => {
      if (this.paused || this.turning) return;
      this.flipTo((this.currentIndex + 1) % this.pages.length, 'next');
      this.cdr.detectChanges();
    }, this.autoMs);
  }

  private stopInterval() {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  private flipTo(next: number, dir: 'next' | 'prev') {
    if (this.turning || next === this.currentIndex) return;
    this.turning = true;
    this.outgoing = { index: this.currentIndex, dir };
    this.currentIndex = next;
    setTimeout(() => {
      this.outgoing = null;
      this.turning = false;
      this.cdr.detectChanges();
    }, this.FLIP_DURATION);
  }

  onMouseEnter() {
    this.paused = true;
  }
  onMouseLeave() {
    this.paused = false;
  }

  goNext() {
    this.flipTo((this.currentIndex + 1) % this.pages.length, 'next');
  }
  goPrev() {
    this.flipTo(
      (this.currentIndex - 1 + this.pages.length) % this.pages.length,
      'prev',
    );
  }
  goToIndex(i: number) {
    this.flipTo(i, i > this.currentIndex ? 'next' : 'prev');
  }

  handleCoverClick(page: CarouselPageDef) {
    if (page.book.id) {
      this.router.navigate(['/detail', page.book.id]);
    }
  }

  handleEmptyCta(page: CarouselPageDef) {
    if (page.emptyCta?.route) {
      this.router.navigate([page.emptyCta.route]);
    }
  }

  handleHeaderCta(page: CarouselPageDef) {
    if (!page.headerCta) return;
    if (page.headerCta.bookId) {
      this.router.navigate(['/detail', page.headerCta.bookId]);
    } else if (page.headerCta.fragment) {
      this.router.navigate(['/mijn-lijsten'], { fragment: page.headerCta.fragment });
    }
  }

  handleLinkClick(page: CarouselPageDef) {
    if (page.linkFragment) {
      this.router.navigate(['/mijn-lijsten'], { fragment: page.linkFragment });
    }
  }

  tintClass(title: string): string {
    const palettes = ['tint-a', 'tint-b', 'tint-c', 'tint-d', 'tint-e', 'tint-f'];
    let hash = 0;
    for (const c of title) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffff;
    return palettes[Math.abs(hash) % palettes.length];
  }
}
