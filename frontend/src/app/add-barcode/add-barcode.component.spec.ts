import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddBarcodeComponent } from './add-barcode.component';

describe('AddBarcodeComponent', () => {
  let component: AddBarcodeComponent;
  let fixture: ComponentFixture<AddBarcodeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddBarcodeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddBarcodeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
