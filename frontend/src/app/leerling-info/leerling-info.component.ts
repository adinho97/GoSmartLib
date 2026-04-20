import { Component } from "@angular/core";

type QuickAction = {
  label: string;
  link: string;
};

type SiteFeature = {
  title: string;
  description: string;
};

type FaqItem = {
  question: string;
  answer: string;
};

@Component({
  selector: "app-leerling-info",
  templateUrl: "./leerling-info.component.html",
  styleUrls: ["./leerling-info.component.css"],
  standalone: false,
})
export class LeerlingInfoComponent {
  openFaqIndex: number | null = 0;

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

  readonly faqItems: FaqItem[] = [
    {
      question: "Ik vind een boek online, maar niet in de bib. Wat nu?",
      answer:
        "Vraag aan de bib-verantwoordelijke of het boek momenteel uitgeleend, verplaatst of niet aanwezig is. Je kunt het boek intussen op je verlanglijst zetten.",
    },
    {
      question: "Hoe zie ik wanneer ik een boek moet terugbrengen?",
      answer:
        "Open je dashboard en kijk bij Geleende boeken. Daar zie je je actieve uitleningen en de relevante datums.",
    },
    {
      question: "Wat is het verschil tussen Verlanglijst en Favorieten?",
      answer:
        "Verlanglijst is voor boeken die je nog wilt lezen. Favorieten zijn boeken die je extra goed vond en snel wilt terugvinden.",
    },
    {
      question: "Hoe krijg ik betere aanbevelingen?",
      answer:
        "Geef voorkeur aan genres die je graag leest, gebruik favorieten en werk je profielgebruik regelmatig bij. Dan worden aanbevelingen persoonlijker.",
    },
  ];

  toggleFaq(index: number): void {
    this.openFaqIndex = this.openFaqIndex === index ? null : index;
  }

  isFaqOpen(index: number): boolean {
    return this.openFaqIndex === index;
  }
}
