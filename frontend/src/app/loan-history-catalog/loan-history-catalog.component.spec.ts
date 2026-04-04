import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoanHistoryCatalogComponent } from './loan-history-catalog.component';

describe('LoanHistoryCatalogComponent', () => {
  let component: LoanHistoryCatalogComponent;
  let fixture: ComponentFixture<LoanHistoryCatalogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LoanHistoryCatalogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoanHistoryCatalogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
