import { Component, OnInit } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Observable } from 'rxjs';
import { ExperienceService, LevelInfo } from '../../services/experience.service';
import { LoanService } from '../../services/loan.service';
import { BookService } from '../../services/book.service';

export type BadgeIconKey =
  | 'boek'
  | 'rij'
  | 'nacht'
  | 'klas'
  | 'bib'
  | 'leg'
  | 'rev'
  | 'recens'
  | 'crit'
  | 'schr'
  | 'lit'
  | 'master';

interface BadgeChip {
  key: string;
  iconKey: BadgeIconKey;
  label: string;
  hint: string;
  cssClass: string;
  unlocked: boolean;
  threshold: number;
  unit: 'boek' | 'review';
}

const LOAN_BADGES: Omit<BadgeChip, 'unlocked'>[] = [
  { key: 'boekenwurm',  iconKey: 'boek',  label: 'Boekenwurm',  hint: '1 boek',     cssClass: 'lb1', threshold: 1,   unit: 'boek' },
  { key: '5-op-rij',    iconKey: 'rij',   label: '5 op rij',    hint: '5 boeken',   cssClass: 'lb2', threshold: 5,   unit: 'boek' },
  { key: 'nachtlezer',  iconKey: 'nacht', label: 'Nachtlezer',  hint: '10 boeken',  cssClass: 'lb3', threshold: 10,  unit: 'boek' },
  { key: 'klassiek',    iconKey: 'klas',  label: 'Klassiek',    hint: '20 boeken',  cssClass: 'lb4', threshold: 20,  unit: 'boek' },
  { key: 'bibliofiel',  iconKey: 'bib',   label: 'Liefhebber',  hint: '50 boeken',  cssClass: 'lb5', threshold: 50,  unit: 'boek' },
  { key: 'legende',     iconKey: 'leg',   label: 'Legende',     hint: '100 boeken', cssClass: 'lb6', threshold: 100, unit: 'boek' },
];

const REVIEW_BADGES: Omit<BadgeChip, 'unlocked'>[] = [
  { key: 'reviewer',     iconKey: 'rev',    label: 'Reviewer',     hint: '1 review',    cssClass: 'rb1', threshold: 1,   unit: 'review' },
  { key: 'recensent',    iconKey: 'recens', label: 'Recensent',    hint: '5 reviews',   cssClass: 'rb2', threshold: 5,   unit: 'review' },
  { key: 'criticus',     iconKey: 'crit',   label: 'Criticus',     hint: '10 reviews',  cssClass: 'rb3', threshold: 10,  unit: 'review' },
  { key: 'schrijver',    iconKey: 'schr',   label: 'Schrijver',    hint: '20 reviews',  cssClass: 'rb4', threshold: 20,  unit: 'review' },
  { key: 'literator',    iconKey: 'lit',    label: 'Literator',    hint: '50 reviews',  cssClass: 'rb5', threshold: 50,  unit: 'review' },
  { key: 'grootmeester', iconKey: 'master', label: 'Grootmeester', hint: '100 reviews', cssClass: 'rb6', threshold: 100, unit: 'review' },
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
  loanCount = 0;
  reviewCount = 0;

  get loanUnlockedCount(): number {
    return this.loanBadges.filter(b => b.unlocked).length;
  }

  get reviewUnlockedCount(): number {
    return this.reviewBadges.filter(b => b.unlocked).length;
  }

  progressLabel(badge: BadgeChip): string {
    if (badge.unlocked) return badge.hint;
    const current = badge.unit === 'boek' ? this.loanCount : this.reviewCount;
    const unit = badge.unit === 'boek' ? (badge.threshold === 1 ? 'boek' : 'boeken') : (badge.threshold === 1 ? 'review' : 'reviews');
    return `${current} / ${badge.threshold} ${unit}`;
  }

  get displayedLoanBadges(): BadgeChip[] {
    return this.loanBadges;
  }

  get displayedReviewBadges(): BadgeChip[] {
    return this.reviewBadges;
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

    this.loanCount = loanCount;
    this.reviewCount = reviewCount;
    this.loanBadges = this.loanBadges.map(b => ({ ...b, unlocked: loanCount >= b.threshold }));
    this.reviewBadges = this.reviewBadges.map(b => ({ ...b, unlocked: reviewCount >= b.threshold }));
  }

  private async fetchLoanCount(): Promise<number> {
    try {
      const history = await this.loanService.getMyLoanHistory();
      return history.length;
    } catch {
      return 0;
    }
  }

  private async fetchReviewCount(): Promise<number> {
    try {
      return await this.bookService.getMyReviewCount();
    } catch {
      return 0;
    }
  }

  getTierLabel(level: number): string {
    if (level >= 25) return 'Legende';
    if (level >= 20) return 'Diamant';
    if (level >= 15) return 'Goud';
    if (level >= 10) return 'Zilver';
    if (level >= 5)  return 'Brons';
    return 'Beginner';
  }

}
