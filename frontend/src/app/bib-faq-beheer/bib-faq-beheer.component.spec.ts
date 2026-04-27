import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BibFaqBeheerComponent } from './bib-faq-beheer.component';

describe('BibFaqBeheerComponent', () => {
  let component: BibFaqBeheerComponent;
  let fixture: ComponentFixture<BibFaqBeheerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BibFaqBeheerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BibFaqBeheerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
