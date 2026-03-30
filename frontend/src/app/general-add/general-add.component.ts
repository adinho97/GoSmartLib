import { Component } from '@angular/core';

type AddTab = 'isbn' | 'manual' | 'bulk' | 'barcode';

@Component({
  selector: 'app-general-add',
  standalone: false,
  templateUrl: './general-add.component.html',
  styleUrls: ['./general-add.component.css'],
})
export class GeneralAddComponent {
  readonly tabs: Array<{ key: AddTab; label: string }> = [
    { key: 'isbn', label: 'Via ISBN' },
    { key: 'manual', label: 'Manueel invoeren' },
    { key: 'bulk', label: 'Bulk import' },
    { key: 'barcode', label: 'Barcode scannen' },
  ];

  activeTab: AddTab = 'isbn';

  setTab(tab: AddTab) {
    this.activeTab = tab;
  }
}
