import { Component, HostListener } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {

  role = localStorage.getItem('role') || 'gebruiker';

  dashboardSettings = {
    showFavorites: true,
    showReadingHistory: true,
    showBorrowed: true
  };

  settingsOpen = false;

  favoriteBooks = [{ title: 'Book One', author: 'Author A', cover: '', id: 1 }, { title: 'Book Two', author: 'Author B', cover: '', id: 2 }];
  readingHistory = [{ title: 'Book Three', finishedDate: new Date(), cover: '', id: 3 }];
  borrowedBooks = [{ title: 'Book Four', deadline: new Date(), cover: '', id: 4 }];

  constructor(private location: Location, private router: Router) {}

  ngOnInit() {
    const saved = localStorage.getItem('dashboardSettings');
    if (saved) {
      this.dashboardSettings = JSON.parse(saved);
    }
  }

  goBack() {
    this.location.back();
  }

  toggleSettings() {
    this.settingsOpen = !this.settingsOpen;
  }

  saveDashboardSettings() {
    localStorage.setItem('dashboardSettings', JSON.stringify(this.dashboardSettings));
    this.settingsOpen = false;
  }

  @HostListener('document:click', ['$event'])
  clickOutside(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.settings-dropdown')) {
      this.settingsOpen = false;
    }
  }

  goToDetail(bookId: number) {
    this.router.navigate(['/detail', bookId]);
  }
}