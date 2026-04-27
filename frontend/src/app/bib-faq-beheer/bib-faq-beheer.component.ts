import { Component, OnInit } from '@angular/core';
import { FaqService, FaqItem } from '../services/faq.service';
import { InfoContentService, InfoContentItem, Sectie } from '../services/info-content.service';
import { SchoolService } from '../services/school.service';
import { School } from '../models/school';

type ActiveTab = 'faq' | 'stappen' | 'features' | 'tips';

@Component({
  selector: 'app-bib-faq-beheer',
  templateUrl: './bib-faq-beheer.component.html',
  styleUrls: ['./bib-faq-beheer.component.css'],
  standalone: false,
})
export class BibFaqBeheerComponent implements OnInit {
  activeTab: ActiveTab = 'faq';
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  errorMessage = '';
  isSaving = false;
  isAdding = false;

  // FAQ
  faqItems: FaqItem[] = [];
  editingFaq: FaqItem | null = null;
  newFaq: FaqItem = { question: '', answer: '' };

  // Info content (stappen / features / tips)
  infoItems: InfoContentItem[] = [];
  editingInfo: InfoContentItem | null = null;
  newInfo: InfoContentItem = { sectie: 'STAP', inhoud: '', titel: '' };

  constructor(
    private faqService: FaqService,
    private infoContentService: InfoContentService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.schools = await this.schoolService.getSchools();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId()
      ?? (this.schools[0]?.id ?? null);
    this.loadTab();
  }

  setTab(tab: ActiveTab): void {
    this.activeTab = tab;
    this.isAdding = false;
    this.editingFaq = null;
    this.editingInfo = null;
    this.loadTab();
  }

  onSchoolChange(schoolId: number): void {
    this.selectedSchoolId = Number(schoolId);
    this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    this.loadTab();
  }

  loadTab(): void {
    this.errorMessage = '';
    if (this.activeTab === 'faq') this.loadFaq();
    else this.loadInfo(this.tabToSectie());
  }

  private tabToSectie(): Sectie {
    if (this.activeTab === 'stappen') return 'STAP';
    if (this.activeTab === 'features') return 'FEATURE';
    return 'TIP';
  }

  // ── FAQ ──────────────────────────────────────────────────
  loadFaq(): void {
    this.faqService.getAll(this.selectedSchoolId ?? undefined).subscribe({
      next: (items) => (this.faqItems = items),
      error: () => (this.errorMessage = 'Kon FAQ niet laden.'),
    });
  }

  startAddFaq(): void {
    this.isAdding = true;
    this.editingFaq = null;
    this.newFaq = { question: '', answer: '', schoolId: this.selectedSchoolId ?? undefined };
  }

  saveNewFaq(): void {
    if (!this.newFaq.question.trim() || !this.newFaq.answer.trim()) return;
    this.isSaving = true;
    this.newFaq.sortOrder = this.faqItems.length;
    this.faqService.create(this.newFaq).subscribe({
      next: () => { this.isAdding = false; this.isSaving = false; this.loadFaq(); },
      error: () => { this.errorMessage = 'Kon FAQ niet opslaan.'; this.isSaving = false; },
    });
  }

  startEditFaq(item: FaqItem): void {
    this.editingFaq = { ...item };
    this.isAdding = false;
  }

  saveEditFaq(): void {
    if (!this.editingFaq?.id) return;
    this.isSaving = true;
    this.faqService.update(this.editingFaq.id, this.editingFaq).subscribe({
      next: () => { this.editingFaq = null; this.isSaving = false; this.loadFaq(); },
      error: () => { this.errorMessage = 'Kon FAQ niet bijwerken.'; this.isSaving = false; },
    });
  }

  deleteFaq(item: FaqItem): void {
    if (!item.id || !confirm(`"${item.question}" verwijderen?`)) return;
    this.faqService.delete(item.id).subscribe({
      next: () => this.loadFaq(),
      error: () => (this.errorMessage = 'Kon FAQ niet verwijderen.'),
    });
  }

  // ── Info content ─────────────────────────────────────────
  loadInfo(sectie: Sectie): void {
    this.infoContentService.getAll(sectie, this.selectedSchoolId ?? undefined).subscribe({
      next: (items) => (this.infoItems = items),
      error: () => (this.errorMessage = 'Kon inhoud niet laden.'),
    });
  }

  startAddInfo(): void {
    this.isAdding = true;
    this.editingInfo = null;
    this.newInfo = {
      sectie: this.tabToSectie(),
      inhoud: '',
      titel: '',
      schoolId: this.selectedSchoolId ?? undefined,
      sortOrder: this.infoItems.length,
    };
  }

  saveNewInfo(): void {
    if (!this.newInfo.inhoud.trim()) return;
    this.isSaving = true;
    this.infoContentService.create(this.newInfo).subscribe({
      next: () => { this.isAdding = false; this.isSaving = false; this.loadInfo(this.tabToSectie()); },
      error: () => { this.errorMessage = 'Kon item niet opslaan.'; this.isSaving = false; },
    });
  }

  startEditInfo(item: InfoContentItem): void {
    this.editingInfo = { ...item };
    this.isAdding = false;
  }

  saveEditInfo(): void {
    if (!this.editingInfo?.id) return;
    this.isSaving = true;
    this.infoContentService.update(this.editingInfo.id, this.editingInfo).subscribe({
      next: () => { this.editingInfo = null; this.isSaving = false; this.loadInfo(this.tabToSectie()); },
      error: () => { this.errorMessage = 'Kon item niet bijwerken.'; this.isSaving = false; },
    });
  }

  deleteInfo(item: InfoContentItem): void {
    if (!item.id || !confirm(`Dit item verwijderen?`)) return;
    this.infoContentService.delete(item.id).subscribe({
      next: () => this.loadInfo(this.tabToSectie()),
      error: () => (this.errorMessage = 'Kon item niet verwijderen.'),
    });
  }

  get showTitelField(): boolean {
    return this.activeTab === 'features';
  }
}