import { Component, Input, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-info-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './info-button.component.html',
  styleUrl: './info-button.component.css',
})
export class InfoButtonComponent {
  @Input() title = '';
  @Input() description = '';

  isOpen = false;

  open(): void { this.isOpen = true; }
  close(): void { this.isOpen = false; }

  @HostListener('document:keydown.escape')
  onEsc(): void { this.close(); }
}
