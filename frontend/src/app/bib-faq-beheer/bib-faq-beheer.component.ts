import { Component, OnInit } from '@angular/core';
import { FaqService, FaqItem } from '../services/faq.service';
import { SchoolService } from '../services/school.service';
import { School } from '../models/school';

@Component({
  selector: 'app-bib-faq-beheer',
  templateUrl: './bib-faq-beheer.component.html',
  styleUrls: ['./bib-faq-beheer.component.css'],
  standalone: false,
})
export class BibFaqBeheerComponent implements OnInit {
  faqItems: FaqItem[] = [];
  schools: School[] = [];
  selectedSchoolId: number | null = null;
  editingItem: FaqItem | null = null;
  isAdding = false;
  isSaving = false;
  errorMessage = '';
  newItem: FaqItem = { question: '', answer: '' };

  constructor(
    private faqService: FaqService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.schools = await this.schoolService.getSchools();
    this.selectedSchoolId = this.schoolService.getSelectedSchoolId()
      ?? (this.schools[0]?.id ?? null);
    this.loadFaqs();
  }

  loadFaqs(): void {
    if (!this.selectedSchoolId) return;
    this.faqService.getAll(this.selectedSchoolId).subscribe({
      next: (items) => (this.faqItems = items),
      error: () => (this.errorMessage = 'Kon FAQ niet laden.'),
    });
  }

  onSchoolChange(schoolId: number): void {
    this.selectedSchoolId = Number(schoolId);
    this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    this.loadFaqs();
  }

  startAdd(): void {
    this.isAdding = true;
    this.editingItem = null;
    this.newItem = { question: '', answer: '', schoolId: this.selectedSchoolId ?? undefined };
  }

  cancelAdd(): void {
    this.isAdding = false;
  }

  saveNew(): void {
    if (!this.newItem.question.trim() || !this.newItem.answer.trim()) return;
    this.isSaving = true;
    this.newItem.sortOrder = this.faqItems.length;
    this.newItem.schoolId = this.selectedSchoolId ?? undefined;
    this.faqService.create(this.newItem).subscribe({
      next: () => { this.isAdding = false; this.isSaving = false; this.loadFaqs(); },
      error: () => { this.errorMessage = 'Kon FAQ niet opslaan.'; this.isSaving = false; },
    });
  }

  startEdit(item: FaqItem): void {
    this.editingItem = { ...item };
    this.isAdding = false;
  }

  cancelEdit(): void {
    this.editingItem = null;
  }

  saveEdit(): void {
    if (!this.editingItem?.id) return;
    if (!this.editingItem.question.trim() || !this.editingItem.answer.trim()) return;
    this.isSaving = true;
    this.faqService.update(this.editingItem.id, this.editingItem).subscribe({
      next: () => { this.editingItem = null; this.isSaving = false; this.loadFaqs(); },
      error: () => { this.errorMessage = 'Kon FAQ niet bijwerken.'; this.isSaving = false; },
    });
  }

  delete(item: FaqItem): void {
    if (!item.id || !confirm(`"${item.question}" verwijderen?`)) return;
    this.faqService.delete(item.id).subscribe({
      next: () => this.loadFaqs(),
      error: () => (this.errorMessage = 'Kon FAQ niet verwijderen.'),
    });
  }
}