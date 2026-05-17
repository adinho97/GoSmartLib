import { Component, OnInit } from "@angular/core";
import {
  InfoContentService,
  InfoContentItem,
} from "../services/info-content.service";
import { Router } from "@angular/router";

type SiteFeature = { title: string; description: string };

@Component({
  selector: "app-leerling-info",
  templateUrl: "./leerling-info.component.html",
  styleUrls: ["./leerling-info.component.css"],
  standalone: false,
})
export class LeerlingInfoComponent implements OnInit {
  openFaqIndex: number | null = 0;
  faqItems: InfoContentItem[] = [];
  loanSteps: string[] = [];
  siteFeatures: SiteFeature[] = [];
  tips: string[] = [];

  private readonly defaultLoanSteps: string[] = [
    "Zoek een boek via Boekencatalogus en open de detailpagina.",
    "Controleer of het boek beschikbaar is in de bibliotheek.",
    "Vind het boek in de bibliotheek en ga naar de bib-verantwoordelijke om het te ontlenen.",
    "Het boek verschijnt daarna bij Geleende boeken in je lijsten.",
    "Lever op tijd in om sancties te voorkomen.",
  ];

  private readonly defaultSiteFeatures: SiteFeature[] = [
    { title: 'Dashboard', description: 'persoonlijke aanbevelingen en snelle toegang tot je profielblokken.' },
    { title: 'Boekencatalogus', description: 'zoeken, filteren en boekdetails bekijken.' },
    { title: 'Mijn lijsten', description: 'verlanglijsten, ontleenhistoriek, klasleeslijsten en geleende boeken.' },
    { title: 'Klassement', description: 'bekijk de top lezers in jouw klas en school.' },
  ];

  private readonly defaultTips: string[] = [
    "Gebruik de filters in de catalogus op genre, taal en leesniveau om sneller een passend boek te vinden.",
    "Voeg interessante boeken toe aan je verlanglijst, zodat je ze later makkelijk terugvindt.",
  ];

  private readonly defaultFaqItems: InfoContentItem[] = [
    {
      sectie: "FAQ",
      titel: "Ik vind een boek online, maar niet in de bib. Wat nu?",
      inhoud:
        "Vraag aan de bib-verantwoordelijke of het boek momenteel uitgeleend, verplaatst of niet aanwezig is. Je kunt het boek intussen op je verlanglijst zetten.",
    },
    {
      sectie: "FAQ",
      titel: "Hoe zie ik wanneer ik een boek moet terugbrengen?",
      inhoud:
        "Open je dashboard en kijk bij Geleende boeken. Daar zie je je actieve uitleningen en de relevante datums.",
    },
  ];

  get isBibbeheerder(): boolean {
    return localStorage.getItem("role") === "bibbeheerder";
  }

  constructor(
    private infoContentService: InfoContentService,
    private router: Router,
  ) {}

  goToEdit(): void {
    this.router.navigate(["/faq-beheer"]);
  }
  ngOnInit(): void {
    const schoolId = this.getSchoolId();
    this.loadFaq(schoolId);
    this.loadStappen(schoolId);
    this.loadFeatures(schoolId);
    this.loadTips(schoolId);
  }

  private getSchoolId(): number | undefined {
    const value = localStorage.getItem("selectedSchoolId");
    if (!value) return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private loadFaq(schoolId?: number): void {
    this.infoContentService.hasContent("FAQ", schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.faqItems = this.defaultFaqItems;
          return;
        }
        this.infoContentService.getAll("FAQ", schoolId).subscribe({
          next: (items) => (this.faqItems = items),
          error: () => (this.faqItems = this.defaultFaqItems),
        });
      },
      error: () => (this.faqItems = this.defaultFaqItems),
    });
  }

  private loadStappen(schoolId?: number): void {
    this.infoContentService.hasContent("STAP", schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.loanSteps = this.defaultLoanSteps;
          return;
        }
        this.infoContentService.getAll("STAP", schoolId).subscribe({
          next: (items) => (this.loanSteps = items.map((i) => i.inhoud)),
          error: () => (this.loanSteps = this.defaultLoanSteps),
        });
      },
      error: () => (this.loanSteps = this.defaultLoanSteps),
    });
  }

  private loadFeatures(schoolId?: number): void {
    this.infoContentService.hasContent("FEATURE", schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.siteFeatures = this.defaultSiteFeatures;
          return;
        }
        this.infoContentService.getAll("FEATURE", schoolId).subscribe({
          next: (items) =>
            (this.siteFeatures = items.map((i) => ({
              title: i.titel ?? "",
              description: i.inhoud,
            }))),
          error: () => (this.siteFeatures = this.defaultSiteFeatures),
        });
      },
      error: () => (this.siteFeatures = this.defaultSiteFeatures),
    });
  }

  private loadTips(schoolId?: number): void {
    this.infoContentService.hasContent("TIP", schoolId).subscribe({
      next: (hasContent) => {
        if (!hasContent) {
          this.tips = this.defaultTips;
          return;
        }
        this.infoContentService.getAll("TIP", schoolId).subscribe({
          next: (items) => (this.tips = items.map((i) => i.inhoud)),
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
