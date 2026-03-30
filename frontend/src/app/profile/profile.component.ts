import { Component, HostListener, Input } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SmartschoolService } from '../services/smartschool.service';
import { BookService } from '../services/book.service';

type ProfileBookCard = {
  title: string;
  author?: string;
  cover: string;
  id: number;
  deadline?: Date;
  loanedDate?: Date;
};

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.css'],
    standalone: false
})
export class ProfileComponent {

  @Input() embedded = false;
  @Input() showHero = true;
  @Input() showSections = true;


  role = localStorage.getItem('role') || 'gebruiker';

  dashboardSettings = {
    showWishlist: true,
    showFavorites: true,
    showReadingHistory: true,
    showBorrowed: true,
    showHighlighted: true,
    showDeadline: true
  };

  userName = localStorage.getItem('userName') || 'Gebruiker';

  settingsOpen = false;

  wishlistBooks: ProfileBookCard[] = [];
  favoriteBooks: ProfileBookCard[] = [{ title: 'Book One', author: 'Author A', cover: '', id: 1 }, { title: 'Book Two', author: 'Author B', cover: '', id: 2 }];
  readingHistory: ProfileBookCard[] = [{ title: 'Book Three', loanedDate: new Date(), cover: '', id: 3 }];
  borrowedBooks: ProfileBookCard[] = [{ title: 'Book Four', deadline: new Date(), cover: '', id: 4 }];
  readingList: ProfileBookCard[] = [{ title: 'Book Five', author: 'Author C', cover: '', id: 5 }];
  wishlistLoading = false;

  constructor(
    private location: Location,
    private router: Router,
    private smartschoolService: SmartschoolService,
    private http: HttpClient,
    private bookService: BookService,
  ) {
  }

  async ngOnInit() {
    const saved = localStorage.getItem('dashboardSettings');
    if (saved) {
      this.dashboardSettings = JSON.parse(saved);
    }
    await this.loadWishlistBooks();
  }

  private async loadWishlistBooks() {
    this.wishlistLoading = true;
    try {
      const wishlist = await this.bookService.getUserWishlist();
      this.wishlistBooks = wishlist.map((item) => ({
        id: item.bookId,
        title: item.titel,
        author: item.auteur,
        cover: item.cover || '',
      }));
    } catch {
      this.wishlistBooks = [];
    } finally {
      this.wishlistLoading = false;
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
    
  this.smartschoolService.sendMessage(
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