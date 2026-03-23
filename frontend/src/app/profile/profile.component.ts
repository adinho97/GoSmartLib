import { Component, HostListener } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { SmartschoolService } from '../services/smartschool.service';

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.css'],
    standalone: false
})
export class ProfileComponent {


  role = localStorage.getItem('role') || 'gebruiker';

  dashboardSettings = {
    showFavorites: true,
    showReadingHistory: true,
    showBorrowed: true,
    showHighlighted: true,
    showDeadline: true
  };

  userName = localStorage.getItem('userName') || 'Gebruiker';

  settingsOpen = false;

  favoriteBooks = [{ title: 'Book One', author: 'Author A', cover: '', id: 1 }, { title: 'Book Two', author: 'Author B', cover: '', id: 2 }];
  readingHistory = [{ title: 'Book Three', loanedDate: new Date(), cover: '', id: 3 }];
  borrowedBooks = [{ title: 'Book Four', deadline: new Date(), cover: '', id: 4 }];
  readingList = [{ title: 'Book Five', author: 'Author C', cover: '', id: 5 }];

  constructor(private location: Location, private router: Router, private smartschoolService: SmartschoolService) {
  }

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

  testSmartschoolMessage(): void {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    alert('Gebruikers-ID niet gevonden. Log opnieuw in.');
    return;
  }

  this.smartschoolService.sendMessage(
    userId, 
    'Testbericht van GoSmartLib', 
    'Dit is een testbericht verstuurd vanuit je profielpagina.'
  ).subscribe({
    next: () => alert('Bericht succesvol verzonden! Check je Smartschool berichten.'),
    error: (err) => {
      console.error(err);
      alert('Er ging iets mis bij het versturen van het bericht.');
    }
  });
}
}