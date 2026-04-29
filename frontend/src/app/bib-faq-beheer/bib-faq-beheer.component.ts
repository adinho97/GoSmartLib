import { Component, OnInit } from '@angular/core';
import { InfoContentService, InfoContentItem, Sectie } from '../services/info-content.service';
import { SchoolService } from '../services/school.service';
import { School } from '../models/school';

type ActiveTab = 'FAQ' | 'STAP' | 'FEATURE' | 'TIP';

@Component({
  selector: 'app-bib-faq-beheer',
  templateUrl: './bib-faq-beheer.component.html',
  styleUrls: ['./bib-faq-beheer.component.css'],
  standalone: false,
})
export class BibFaqBeheerComponent implements OnInit {
  activeTab: ActiveTab = 'FAQ';
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  errorMessage = '';
  isSaving = false;
  isAdding = false;

  items: InfoContentItem[] = [];
  editingItem: InfoContentItem | null = null;
  newItem: InfoContentItem = { sectie: 'FAQ', titel: '', inhoud: '' };

  confirmVisible = false;
  confirmMessage = '';
  private pendingDeleteFn: (() => void) | null = null;

  constructor(
    private infoContentService: InfoContentService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.schools = await this.schoolService.getSchools();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId()
      ?? (this.schools[0]?.id ?? null);
    this.loadItems();
  }

  setTab(tab: ActiveTab): void {
    this.activeTab = tab;
    this.isAdding = false;
    this.editingItem = null;
    this.errorMessage = '';
    this.loadItems();
  }

  onSchoolChange(schoolId: number): void {
    this.selectedSchoolId = Number(schoolId);
    this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    this.loadItems();
  }

  loadItems(): void {
    this.infoContentService.getAll(this.activeTab, this.selectedSchoolId ?? undefined).subscribe({
      next: (items) => (this.items = items),
      error: () => (this.errorMessage = 'Kon inhoud niet laden.'),
    });
  }

  startAdd(): void {
    this.isAdding = true;
    this.editingItem = null;
    this.newItem = {
      sectie: this.activeTab,
      titel: '',
      inhoud: '',
      schoolId: this.selectedSchoolId ?? undefined,
      sortOrder: this.items.length,
    };
  }

  saveNew(): void {
    if (!this.newItem.inhoud.trim()) return;
    if (this.showTitelField && !this.newItem.titel?.trim()) return;
    this.isSaving = true;
    this.infoContentService.create(this.newItem).subscribe({
      next: () => { this.isAdding = false; this.isSaving = false; this.loadItems(); },
      error: () => { this.errorMessage = 'Kon item niet opslaan.'; this.isSaving = false; },
    });
  }

  startEdit(item: InfoContentItem): void {
    this.editingItem = { ...item };
    this.isAdding = false;
  }

  saveEdit(): void {
    if (!this.editingItem?.id) return;
    if (!this.editingItem.inhoud.trim()) return;
    this.isSaving = true;
    this.infoContentService.update(this.editingItem.id, this.editingItem).subscribe({
      next: () => { this.editingItem = null; this.isSaving = false; this.loadItems(); },
      error: () => { this.errorMessage = 'Kon item niet bijwerken.'; this.isSaving = false; },
    });
  }

  deleteItem(item: InfoContentItem): void {
    if (!item.id) return;
    const label = item.titel || item.inhoud.substring(0, 40);
    this.showConfirm(`"${label}" verwijderen?`, () => {
      this.infoContentService.delete(item.id!).subscribe({
        next: () => this.loadItems(),
        error: () => (this.errorMessage = 'Kon item niet verwijderen.'),
      });
    });
  }

  private showConfirm(message: string, onConfirm: () => void): void {
    this.confirmMessage = message;
    this.pendingDeleteFn = onConfirm;
    this.confirmVisible = true;
  }

  confirmDelete(): void {
    this.pendingDeleteFn?.();
    this.confirmVisible = false;
    this.pendingDeleteFn = null;
  }

  cancelDelete(): void {
    this.confirmVisible = false;
    this.pendingDeleteFn = null;
  }

  get showTitelField(): boolean {
    return this.activeTab === 'FEATURE' || this.activeTab === 'FAQ';
  }

  get titelLabel(): string {
    return this.activeTab === 'FAQ' ? 'Vraag' : 'Titel';
  }

  get inhoudLabel(): string {
    return this.activeTab === 'FAQ' ? 'Antwoord' : 'Inhoud';
  }
}