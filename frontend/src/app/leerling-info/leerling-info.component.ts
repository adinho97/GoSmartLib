import { Component } from "@angular/core";

@Component({
  selector: "app-leerling-info",
  templateUrl: "./leerling-info.component.html",
  styleUrls: ["./leerling-info.component.css"],
  standalone: false,
})
export class LeerlingInfoComponent {
  openFaqIndex: number | null = 0;

  readonly faqItems = [
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
}
