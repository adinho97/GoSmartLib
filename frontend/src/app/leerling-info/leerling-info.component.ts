import { Component, OnInit } from "@angular/core";
import { FaqService, FaqItem } from "../services/faq.service";

type QuickAction = {
  label: string;
  link: string;
};

type SiteFeature = {
  title: string;
  description: string;
};

@Component({
  selector: "app-leerling-info",
  templateUrl: "./leerling-info.component.html",
  styleUrls: ["./leerling-info.component.css"],
  standalone: false,
})
export class LeerlingInfoComponent implements OnInit {
  openFaqIndex: number | null = 0;
  faqItems: FaqItem[] = [];

  readonly quickActions: QuickAction[] = [
    { label: "Naar Boekencatalogus", link: "/books" },
    { label: "Naar Dashboard", link: "/dashboard" },
  ];

  readonly loanSteps: string[] = [
    "Zoek een boek via Boekencatalogus en open de detailpagina.",
    "Controleer of het boek beschikbaar is in de bibliotheek.",
    "Vind het boek in de bibliotheek en ga naar de bib-verantwoordelijke om het te ontlenen.",
    "Het boek verschijnt daarna bij Geleende boeken in je dashboardprofiel.",
    "Lever op tijd in om boetes of blokkering te vermijden.",
  ];

  readonly siteFeatures: SiteFeature[] = [
    {
      title: "Dashboard",
      description:
        "persoonlijke aanbevelingen en snelle toegang tot je profielblokken.",
    },
    {
      title: "Boekencatalogus",
      description: "zoeken, filteren en boekdetails bekijken.",
    },
    {
      title: "Verlanglijst",
      description: "bewaar boeken die je later wilt lezen.",
    },
    {
      title: "Favorieten",
      description: "markeer boeken die je extra goed vond.",
    },
    {
      title: "Ontleenhistoriek",
      description: "bekijk welke boeken je eerder ontleende.",
    },
  ];

  readonly tips: string[] = [
    "Gebruik de filters in de catalogus op genre, taal en leesniveau om sneller een passend boek te vinden.",
    "Voeg interessante titels toe aan je verlanglijst, zodat je ze later makkelijk terugvindt.",
  ];

  constructor(private faqService: FaqService) {}

  ngOnInit(): void {
    this.faqService.getAll().subscribe({
      next: (items) => (this.faqItems = items),
      error: () => (this.faqItems = []),
    });
  }

  toggleFaq(index: number): void {
    this.openFaqIndex = this.openFaqIndex === index ? null : index;
  }

  isFaqOpen(index: number): boolean {
    return this.openFaqIndex === index;
  }
}