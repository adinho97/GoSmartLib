import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ColorblindService } from '../services/colorblind.service';

@Component({
  selector: 'app-colorblind-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './colorblind-toggle.component.html',
  styleUrls: ['./colorblind-toggle.component.css'],
})
export class ColorblindToggleComponent {
  constructor(public service: ColorblindService) {}

  get isActive(): boolean {
    return this.service.getMode() !== 'none';
  }
}