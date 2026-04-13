import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecommendationSectionComponent } from './recommendation-section.component';

describe('RecommendationSectionComponent', () => {
  let component: RecommendationSectionComponent;
  let fixture: ComponentFixture<RecommendationSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecommendationSectionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RecommendationSectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
