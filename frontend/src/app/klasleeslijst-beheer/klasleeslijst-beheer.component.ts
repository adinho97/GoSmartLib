import { Component } from '@angular/core';
import { Router } from '@angular/router';

interface KlasLeeslijstItem {
  titel: string;
  auteur: string;
  toegevoegd: string;
}

interface Klas {
  naam: string;
  leeslijst: KlasLeeslijstItem[];
}

@Component({
  selector: 'app-klasleeslijst-beheer',
  templateUrl: './klasleeslijst-beheer.component.html',
  styleUrls: ['./klasleeslijst-beheer.component.css'],
  standalone: false,
})
export class KlasleeslijstBeheerComponent {
  selectedKlas: string | null = null;

  klassen: Klas[] = [
    {
      naam: '5A',
      leeslijst: [
        { titel: 'De Hobbit', auteur: 'J.R.R. Tolkien', toegevoegd: '12 sept 2024' },
        { titel: 'De Kleine Prins', auteur: 'Antoine de Saint-Exupéry', toegevoegd: '3 okt 2024' },
      ],
    },
    {
      naam: '5B',
      leeslijst: [
        { titel: 'Harry Potter en de Steen der Wijzen', auteur: 'J.K. Rowling', toegevoegd: '8 okt 2024' },
      ],
    },
    {
      naam: '6A',
      leeslijst: [],
    },
    {
      naam: '6B',
      leeslijst: [
        { titel: 'Het Achterhuis', auteur: 'Anne Frank', toegevoegd: '15 nov 2024' },
        { titel: 'Oorlogswinter', auteur: 'Jan Terlouw', toegevoegd: '20 nov 2024' },
        { titel: 'Kruistocht in spijkerbroek', auteur: 'Thea Beckman', toegevoegd: '2 dec 2024' },
      ],
    },
  ];

  constructor(private router: Router) {}

  toggleKlas(naam: string): void {
    this.selectedKlas = this.selectedKlas === naam ? null : naam;
  }

  getSelectedKlas(): Klas | null {
    return this.klassen.find(k => k.naam === this.selectedKlas) ?? null;
  }

  goToBooks(): void {
    this.router.navigate(['/books']);
  }
}
