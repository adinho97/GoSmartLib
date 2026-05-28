import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReportedReviewsComponent } from "./reported-reviews.component";
import { BookService, ReportedReviewStatus } from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";
import { Router } from "@angular/router";
import { of } from "rxjs";
import { FormsModule } from "@angular/forms";

describe("ReportedReviewsComponent", () => {
  let component: ReportedReviewsComponent;
  let fixture: ComponentFixture<ReportedReviewsComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let uiToastServiceSpy: jasmine.SpyObj<UiToastService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getReportedReviews",
      "deleteReportedReview",
      "resolveReportedReview",
    ]);
    uiToastServiceSpy = jasmine.createSpyObj("UiToastService", [
      "success",
      "error",
    ]);
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    bookServiceSpy.getReportedReviews.and.resolveTo([
      {
        id: 1,
        book: { id: 10, titel: "Test Boek" },
        review: {
          id: 5,
          rating: 2,
          comment: "Niet zo leuk",
          reviewerUserName: "Jan Janssen",
          anonymous: false,
        },
        reporterUserSub: "sub-123",
        reporterUserName: "Melder Piet",
        reason: "Ongepast taalgebruik",
        reportedAt: "2024-05-20T10:00:00Z",
        status: ReportedReviewStatus.PENDING,
      },
    ]);

    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [ReportedReviewsComponent],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportedReviewsComponent);
    component = fixture.componentInstance;
  });

  it("should create and load reports on init", async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.reportedReviews.length).toBe(1);
    expect(component.loading).toBeFalse();
    expect(bookServiceSpy.getReportedReviews).toHaveBeenCalled();
  });

  it("should navigate to book detail when book title is clicked", () => {
    component.goToBookDetail(10);
    expect(routerSpy.navigate).toHaveBeenCalledWith(["/detail", 10]);
  });

  it("confirmDeleteReview calls service and reloads list", async () => {
    bookServiceSpy.deleteReportedReview.and.resolveTo();
    component.pendingReportId = 1;
    component.pendingBookId = 10;
    component.pendingReviewId = 5;

    await component.confirmDeleteReview();

    expect(bookServiceSpy.deleteReportedReview).toHaveBeenCalledWith(1, 10, 5);
    expect(uiToastServiceSpy.success).toHaveBeenCalled();
    expect(bookServiceSpy.getReportedReviews).toHaveBeenCalledTimes(2); // Initial + Reload
    expect(component.deleteReviewDialogOpen).toBeFalse();
  });

  it("confirmResolveReport calls service and reloads list", async () => {
    bookServiceSpy.resolveReportedReview.and.resolveTo();
    component.pendingReportId = 1;

    await component.confirmResolveReport();

    expect(bookServiceSpy.resolveReportedReview).toHaveBeenCalledWith(
      1,
      ReportedReviewStatus.RESOLVED_KEPT,
    );
    expect(uiToastServiceSpy.success).toHaveBeenCalled();
    expect(bookServiceSpy.getReportedReviews).toHaveBeenCalledTimes(2);
    expect(component.resolveReportDialogOpen).toBeFalse();
  });

  it("getAuthorDisplayName handles anonymous reviews", () => {
    const review = {
      anonymous: true,
      reviewerUserName: "Jan Janssen",
    } as any;
    expect(component.getAuthorDisplayName(review)).toBe("Anoniem");

    review.anonymous = false;
    expect(component.getAuthorDisplayName(review)).toBe("Jan Janssen");
  });
});
