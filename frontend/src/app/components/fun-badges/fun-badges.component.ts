import { Component, OnInit } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { ExperienceService, LevelInfo } from '../../services/experience.service';

interface BadgeChip {
  key: string;
  icon: string;
  label: string;
  cssClass: string;
  unlocked: boolean;
}

@Component({
  selector: 'app-fun-badges',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './fun-badges.component.html',
  styleUrl: './fun-badges.component.css',
})
export class FunBadgesComponent implements OnInit {
  levelInfo$: Observable<LevelInfo>;

  badges: BadgeChip[] = [
    { key: 'boekenwurm', icon: '📚', label: 'Boekenwurm', cssClass: 'b1', unlocked: false },
    { key: '5-op-rij',   icon: '🔥', label: '5 op rij',   cssClass: 'b2', unlocked: false },
    { key: 'nachtlezer', icon: '🌙', label: 'Nachtlezer', cssClass: 'b3', unlocked: false },
    { key: 'klassiek',   icon: '🏛️', label: 'Klassiek',   cssClass: 'b4', unlocked: false },
  ];

  constructor(
    private experienceService: ExperienceService,
    private router: Router,
  ) {
    this.levelInfo$ = this.experienceService.levelInfo$;
  }

  ngOnInit(): void {
    const userSub = localStorage.getItem('sub') || '';
    const key = userSub ? `loanXpSyncedCount:${userSub}` : 'loanXpSyncedCount';
    const loanCount = parseInt(localStorage.getItem(key) || '0', 10);
    this.badges = this.badges.map((b, i) => ({
      ...b,
      unlocked: loanCount >= [1, 5, 10, 20][i],
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

  navigateToBadges(): void {
    this.router.navigate(['/mijn-lijsten']);
  }
}
