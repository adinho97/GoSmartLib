import { Component, OnInit } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Observable } from 'rxjs';
import { ExperienceService, LevelInfo } from '../../services/experience.service';
import { LoanService } from '../../services/loan.service';
import { BookService } from '../../services/book.service';

interface BadgeChip {
  key: string;
  icon: string;
  label: string;
  hint: string;
  cssClass: string;
  unlocked: boolean;
  threshold: number;
}

const LOAN_BADGES: Omit<BadgeChip, 'unlocked'>[] = [
  { key: 'boekenwurm',  icon: '📚', label: 'Boekenwurm',  hint: '1 boek',     cssClass: 'lb1', threshold: 1   },
  { key: '5-op-rij',    icon: '🔥', label: '5 op rij',    hint: '5 boeken',   cssClass: 'lb2', threshold: 5   },
  { key: 'nachtlezer',  icon: '🌙', label: 'Nachtlezer',  hint: '10 boeken',  cssClass: 'lb3', threshold: 10  },
  { key: 'klassiek',    icon: '🏛️', label: 'Klassiek',    hint: '20 boeken',  cssClass: 'lb4', threshold: 20  },
  { key: 'bibliofiel',  icon: '🔖', label: 'Bibliofiel',  hint: '50 boeken',  cssClass: 'lb5', threshold: 50  },
  { key: 'legende',     icon: '⭐', label: 'Legende',     hint: '100 boeken', cssClass: 'lb6', threshold: 100 },
];

const REVIEW_BADGES: Omit<BadgeChip, 'unlocked'>[] = [
  { key: 'reviewer',     icon: '✍️', label: 'Reviewer',     hint: '1 review',    cssClass: 'rb1', threshold: 1   },
  { key: 'recensent',    icon: '📝', label: 'Recensent',    hint: '5 reviews',   cssClass: 'rb2', threshold: 5   },
  { key: 'criticus',     icon: '🔍', label: 'Criticus',     hint: '10 reviews',  cssClass: 'rb3', threshold: 10  },
  { key: 'schrijver',    icon: '📖', label: 'Schrijver',    hint: '20 reviews',  cssClass: 'rb4', threshold: 20  },
  { key: 'literator',    icon: '🌟', label: 'Literator',    hint: '50 reviews',  cssClass: 'rb5', threshold: 50  },
  { key: 'grootmeester', icon: '👑', label: 'Grootmeester', hint: '100 reviews', cssClass: 'rb6', threshold: 100 },
];

@Component({
  selector: 'app-fun-badges',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './fun-badges.component.html',
  styleUrl: './fun-badges.component.css',
})
export class FunBadgesComponent implements OnInit {
  levelInfo$: Observable<LevelInfo>;

  loanBadges: BadgeChip[] = LOAN_BADGES.map(b => ({ ...b, unlocked: false }));
  reviewBadges: BadgeChip[] = REVIEW_BADGES.map(b => ({ ...b, unlocked: false }));
  showAllLoan = false;
  showAllReview = false;

  get displayedLoanBadges(): BadgeChip[] {
    return this.showAllLoan ? this.loanBadges : this.loanBadges.slice(0, 4);
  }

  get displayedReviewBadges(): BadgeChip[] {
    return this.showAllReview ? this.reviewBadges : this.reviewBadges.slice(0, 4);
  }

  constructor(
    private experienceService: ExperienceService,
    private loanService: LoanService,
    private bookService: BookService,
  ) {
    this.levelInfo$ = this.experienceService.levelInfo$;
  }

  async ngOnInit(): Promise<void> {
    const [loanCount, reviewCount] = await Promise.all([
      this.fetchLoanCount(),
      this.fetchReviewCount(),
    ]);

    this.loanBadges = this.loanBadges.map(b => ({ ...b, unlocked: loanCount >= b.threshold }));
    this.reviewBadges = this.reviewBadges.map(b => ({ ...b, unlocked: reviewCount >= b.threshold }));
  }

  private async fetchLoanCount(): Promise<number> {
    try {
      const history = await this.loanService.getMyLoanHistory();
      return history.length;
    } catch {
      return this.readStoredLoanCount();
    }
  }

  private async fetchReviewCount(): Promise<number> {
    try {
      return await this.bookService.getMyReviewCount();
    } catch {
      return this.readStoredReviewCount();
    }
  }

  private readStoredLoanCount(): number {
    try {
      const stored = localStorage.getItem('profileBadgeCounts');
      if (stored) {
        const parsed = JSON.parse(stored) as { loanCount?: number };
        return parsed.loanCount ?? 0;
      }
    } catch { /* ignore */ }
    const userSub = localStorage.getItem('sub') || '';
    const key = userSub ? `loanXpSyncedCount:${userSub}` : 'loanXpSyncedCount';
    return parseInt(localStorage.getItem(key) || '0', 10);
  }

  private readStoredReviewCount(): number {
    try {
      const stored = localStorage.getItem('profileBadgeCounts');
      if (stored) {
        const parsed = JSON.parse(stored) as { reviewCount?: number };
        return parsed.reviewCount ?? 0;
      }
    } catch { /* ignore */ }
    return 0;
  }

  getTierLabel(level: number): string {
    if (level >= 25) return 'Legende';
    if (level >= 20) return 'Diamant';
    if (level >= 15) return 'Goud';
    if (level >= 10) return 'Zilver';
    if (level >= 5)  return 'Brons';
    return 'Beginner';
  }

  toggleLoan(): void { this.showAllLoan = !this.showAllLoan; }
  toggleReview(): void { this.showAllReview = !this.showAllReview; }
}
