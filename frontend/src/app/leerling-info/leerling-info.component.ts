import { Component, OnInit } from "@angular/core";
import {
  InfoContentService,
  InfoContentItem,
} from "../services/info-content.service";

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

  get isBibbeheerder(): boolean {
    return localStorage.getItem("role") === "bibbeheerder";
  }

  constructor(private infoContentService: InfoContentService) {}

  ngOnInit(): void {
    const schoolId = this.getSchoolId();
    this.infoContentService.getAll("STAP", schoolId).subscribe((items) => {
      this.loanSteps = items.map((i) => i.inhoud);
    });
    this.infoContentService.getAll("FEATURE", schoolId).subscribe((items) => {
      this.siteFeatures = items.map((i) => ({
        title: i.titel ?? "",
        description: i.inhoud,
      }));
    });
    this.infoContentService.getAll("TIP", schoolId).subscribe((items) => {
      this.tips = items.map((i) => i.inhoud);
    });
    this.infoContentService.getAll("FAQ", schoolId).subscribe((items) => {
      this.faqItems = items;
    });
  }

  private getSchoolId(): number | undefined {
    const value = localStorage.getItem("selectedSchoolId");
    if (!value) return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  toggleFaq(index: number): void {
    this.openFaqIndex = this.openFaqIndex === index ? null : index;
  }

  isFaqOpen(index: number): boolean {
    return this.openFaqIndex === index;
  }
}
