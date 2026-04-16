import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectBibbeheerderComponent } from './select-bibbeheerder.component';

describe('SelectBibbeheerderComponent', () => {
  let component: SelectBibbeheerderComponent;
  let fixture: ComponentFixture<SelectBibbeheerderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectBibbeheerderComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelectBibbeheerderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
