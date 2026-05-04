import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DarkModeService } from '../services/dark-mode.service';

@Component({
  selector: 'app-dark-mode-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dark-mode-toggle.component.html',
  styleUrls: ['./dark-mode-toggle.component.css'],
})
export class DarkModeToggleComponent {
  constructor(public service: DarkModeService) {}
  get isDark(): boolean { return this.service.isDark(); }
}