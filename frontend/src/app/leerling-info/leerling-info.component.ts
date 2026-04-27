import { Component, OnInit } from '@angular/core';
import { FaqService, FaqItem } from '../services/faq.service';
import { InfoContentService, InfoContentItem } from '../services/info-content.service';

type QuickAction = { label: string; link: string; };
type SiteFeature = { title: string; description: string; };

@Component({
  selector: 'app-leerling-info',
  templateUrl: './leerling-info.component.html',
  styleUrls: ['./leerling-info.component.css'],
  standalone: false,
})
export class LeerlingInfoComponent implements OnInit {
  openFaqIndex: number | null = 0;
  faqItems: FaqItem[] = [];
  loanSteps: string[] = [];
  siteFeatures: SiteFeature[] = [];
  tips: string[] = [];

  readonly quickActions: QuickAction[] = [
    { label: 'Naar Boekencatalogus', link: '/books' },
    { label: 'Naar Dashboard', link: '/dashboard' },
  ];

  private readonly defaultLoanSteps: string[] = [
    'Zoek een boek via Boekencatalogus en open de detailpagina.',
    'Controleer of het boek beschikbaar is in de bibliotheek.',
    'Vind het boek in de bibliotheek en ga naar de bib-verantwoordelijke om het te ontlenen.',
    'Het boek verschijnt daarna bij Geleende boeken in je dashboardprofiel.',
    'Lever op tijd in om boetes of blokkering te vermijden.',
  ];

  private readonly defaultSiteFeatures: SiteFeature[] = [
    { title: 'Dashboard', description: 'persoonlijke aanbevelingen en snelle toegang tot je profielblokken.' },
    { title: 'Boekencatalogus', description: 'zoeken, filteren en boekdetails bekijken.' },
    { title: 'Verlanglijst', description: 'bewaar boeken die je later wilt lezen.' },
    { title: 'Favorieten', description: 'markeer boeken die je extra goed vond.' },
    { title: 'Ontleenhistoriek', description: 'bekijk welke boeken je eerder ontleende.' },
  ];

  private readonly defaultTips: string[] = [
    'Gebruik de filters in de catalogus op genre, taal en leesniveau om sneller een passend boek te vinden.',
    'Voeg interessante titels toe aan je verlanglijst, zodat je ze later makkelijk terugvindt.',
  ];

  constructor(
    private faqService: FaqService,
    private infoContentService: InfoContentService,
  ) {}

  ngOnInit(): void {
    const schoolId = this.getSchoolId();
    this.loadFaq(schoolId);
    this.loadStappen(schoolId);
    this.loadFeatures(schoolId);
    this.loadTips(schoolId);
  }

  private getSchoolId(): number | undefined {
    const value = localStorage.getItem('selectedSchoolId');
    if (!value) return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private loadFaq(schoolId?: number): void {
    this.faqService.getAll(schoolId).subscribe({
      next: (items) => (this.faqItems = items),
      error: () => (this.faqItems = []),
    });
  }

  private loadStappen(schoolId?: number): void {
    this.infoContentService.hasContent('STAP', schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.loanSteps = this.defaultLoanSteps;
          return;
        }
        this.infoContentService.getAll('STAP', schoolId).subscribe({
          next: (items) => (this.loanSteps = items.map(i => i.inhoud)),
          error: () => (this.loanSteps = this.defaultLoanSteps),
        });
      },
      error: () => (this.loanSteps = this.defaultLoanSteps),
    });
  }

  private loadFeatures(schoolId?: number): void {
    this.infoContentService.hasContent('FEATURE', schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.siteFeatures = this.defaultSiteFeatures;
          return;
        }
        this.infoContentService.getAll('FEATURE', schoolId).subscribe({
          next: (items) => (this.siteFeatures = items.map(i => ({
            title: i.titel ?? '',
            description: i.inhoud,
          }))),
          error: () => (this.siteFeatures = this.defaultSiteFeatures),
        });
      },
      error: () => (this.siteFeatures = this.defaultSiteFeatures),
    });
  }

  private loadTips(schoolId?: number): void {
    this.infoContentService.hasContent('TIP', schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.tips = this.defaultTips;
          return;
        }
        this.infoContentService.getAll('TIP', schoolId).subscribe({
          next: (items) => (this.tips = items.map(i => i.inhoud)),
          error: () => (this.tips = this.defaultTips),
        });
      },
      error: () => (this.tips = this.defaultTips),
    });
  }

  toggleFaq(index: number): void {
    this.openFaqIndex = this.openFaqIndex === index ? null : index;
  }

  isFaqOpen(index: number): boolean {
    return this.openFaqIndex === index;
  }
}