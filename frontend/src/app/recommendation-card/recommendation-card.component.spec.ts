import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { RecommendationCardComponent } from "../recommendation-card/recommendation-card.component";
import { RecommendedBook } from "../services/recommendation.service";

describe("RecommendationCardComponent", () => {
  let component: RecommendationCardComponent;
  let fixture: ComponentFixture<RecommendationCardComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockBook: RecommendedBook = {
    bookId: 1,
    titel: "Test Book",
    auteur: "Test Author",
    genre: "Fiction",
    score: 100,
    reason: "Trending",
    cover: "cover.jpg",
  };

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      imports: [RecommendationCardComponent],
      providers: [{ provide: Router, useValue: routerSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RecommendationCardComponent);
    component = fixture.componentInstance;
    component.book = mockBook;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should display book title and author", () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain("Test Book");
  });

  it("should navigate to book detail on card click", () => {
    spyOn(component.viewDetails, "emit");
    component.onViewDetails();
    expect(component.viewDetails.emit).toHaveBeenCalled();
  });

  it("should emit requestExtension when extension button is clicked", () => {
    spyOn(component.requestExtension, "emit");
    component.profileVariant = "history";
    component.loanReturned = false;
    fixture.detectChanges();

    const event = new MouseEvent("click");
    component.onRequestExtension(event);

    expect(component.requestExtension.emit).toHaveBeenCalled();
  });

  it("should disable the extension button when hasPendingExtensionRequest is true", () => {
    component.profileVariant = "history";
    component.loanReturned = false;
    component.hasPendingExtensionRequest = true;
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector(".extension-btn");
    expect(button.disabled).toBeTrue();
  });
});
