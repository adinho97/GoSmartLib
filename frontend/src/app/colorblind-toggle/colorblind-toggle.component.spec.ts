import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ColorblindToggleComponent } from './colorblind-toggle.component';

describe('ColorblindToggleComponent', () => {
  let component: ColorblindToggleComponent;
  let fixture: ComponentFixture<ColorblindToggleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ColorblindToggleComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ColorblindToggleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
