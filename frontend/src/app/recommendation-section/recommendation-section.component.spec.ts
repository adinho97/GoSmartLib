import { ComponentFixture, TestBed } from "@angular/core/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RecommendationSectionComponent } from "./recommendation-section.component"; // Assuming this component exists
import { RecommendationService } from "../services/recommendation.service";
import { BookService } from "../services/book.service";
import { of } from "rxjs";

describe("RecommendationSectionComponent", () => {
  let component: RecommendationSectionComponent;
  let fixture: ComponentFixture<RecommendationSectionComponent>;
  let recommendationServiceSpy: jasmine.SpyObj<RecommendationService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;

  beforeEach(async () => {
    recommendationServiceSpy = jasmine.createSpyObj("RecommendationService", [
      "getRecommendationsByStrategy",
    ]);
    recommendationServiceSpy.getRecommendationsByStrategy.and.returnValue(
      Promise.resolve([]),
    );
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "enrichBooksWithDetails",
    ]);
    bookServiceSpy.enrichBooksWithDetails.and.returnValue(Promise.resolve([]));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RecommendationSectionComponent], // Assuming it's standalone
      providers: [
        { provide: RecommendationService, useValue: recommendationServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RecommendationSectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
