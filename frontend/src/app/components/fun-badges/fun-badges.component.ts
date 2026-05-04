import { Component, OnInit } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Observable } from 'rxjs';
import { ExperienceService, LevelInfo } from '../../services/experience.service';

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

  constructor(private experienceService: ExperienceService) {
    this.levelInfo$ = this.experienceService.levelInfo$;
  }

  ngOnInit(): void {
    const userSub = localStorage.getItem('sub') || '';
    let loanCount = 0;
    let reviewCount = 0;

    // profileBadgeCounts is kept in sync by badge-collection — most reliable source
    const storedCounts = localStorage.getItem('profileBadgeCounts');
    if (storedCounts) {
      try {
        const parsed = JSON.parse(storedCounts) as { loanCount?: number; reviewCount?: number };
        loanCount = parsed.loanCount ?? 0;
        reviewCount = parsed.reviewCount ?? 0;
      } catch { /* ignore */ }
    }

    // Fallback for loan count if profile page has not been visited yet
    if (loanCount === 0) {
      const loanKey = userSub ? `loanXpSyncedCount:${userSub}` : 'loanXpSyncedCount';
      loanCount = parseInt(localStorage.getItem(loanKey) || '0', 10);
    }

    this.loanBadges = this.loanBadges.map(b => ({ ...b, unlocked: loanCount >= b.threshold }));
    this.reviewBadges = this.reviewBadges.map(b => ({ ...b, unlocked: reviewCount >= b.threshold }));
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
