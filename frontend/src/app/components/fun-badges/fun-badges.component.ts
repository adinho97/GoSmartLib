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

const ALL_BADGES: Omit<BadgeChip, 'unlocked'>[] = [
  { key: 'boekenwurm',   icon: '📚', label: 'Boekenwurm',   hint: '1 boek',    cssClass: 'b1', threshold: 1   },
  { key: '5-op-rij',     icon: '🔥', label: '5 op rij',     hint: '5 boeken',  cssClass: 'b2', threshold: 5   },
  { key: 'nachtlezer',   icon: '🌙', label: 'Nachtlezer',   hint: '10 boeken', cssClass: 'b3', threshold: 10  },
  { key: 'klassiek',     icon: '🏛️', label: 'Klassiek',     hint: '20 boeken', cssClass: 'b4', threshold: 20  },
  { key: 'leesheld',     icon: '🦸', label: 'Leesheld',     hint: '30 boeken', cssClass: 'b5', threshold: 30  },
  { key: 'bibliofiel',   icon: '🔖', label: 'Bibliofiel',   hint: '50 boeken', cssClass: 'b6', threshold: 50  },
  { key: 'meestelezer',  icon: '🏆', label: 'Meestelezer',  hint: '75 boeken', cssClass: 'b7', threshold: 75  },
  { key: 'legende',      icon: '⭐', label: 'Legende',      hint: '100 boeken',cssClass: 'b8', threshold: 100 },
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

  allBadges: BadgeChip[] = ALL_BADGES.map(b => ({ ...b, unlocked: false }));
  showAll = false;

  get displayedBadges(): BadgeChip[] {
    return this.showAll ? this.allBadges : this.allBadges.slice(0, 4);
  }

  constructor(private experienceService: ExperienceService) {
    this.levelInfo$ = this.experienceService.levelInfo$;
  }

  ngOnInit(): void {
    const userSub = localStorage.getItem('sub') || '';
    const key = userSub ? `loanXpSyncedCount:${userSub}` : 'loanXpSyncedCount';
    const loanCount = parseInt(localStorage.getItem(key) || '0', 10);
    this.allBadges = this.allBadges.map(b => ({
      ...b,
      unlocked: loanCount >= b.threshold,
    }));
  }

  getTierLabel(level: number): string {
    if (level >= 25) return 'Legende';
    if (level >= 20) return 'Diamant';
    if (level >= 15) return 'Goud';
    if (level >= 10) return 'Zilver';
    if (level >= 5)  return 'Brons';
    return 'Beginner';
  }

  toggleShowAll(): void {
    this.showAll = !this.showAll;
  }
}
