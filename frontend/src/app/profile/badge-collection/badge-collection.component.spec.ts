import { ComponentFixture, TestBed } from "@angular/core/testing";
import { BadgeCollectionComponent } from "./badge-collection.component";
import { BookService } from "../../services/book.service";
import { LoanService } from "../../services/loan.service";
import { BadgeNotificationService } from "../../services/badge-notification.service";
import { ExperienceService } from "../../services/experience.service";

describe("BadgeCollectionComponent", () => {
  let component: BadgeCollectionComponent;
  let fixture: ComponentFixture<BadgeCollectionComponent>;

  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let badgeNotificationServiceSpy: jasmine.SpyObj<BadgeNotificationService>;
  let experienceServiceSpy: jasmine.SpyObj<ExperienceService>;

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", ["getMyReviewCount"]);
    loanServiceSpy = jasmine.createSpyObj("LoanService", ["getMyLoanHistory"]);
    badgeNotificationServiceSpy = jasmine.createSpyObj(
      "BadgeNotificationService",
      ["showBadgeNotification"],
    );
    experienceServiceSpy = jasmine.createSpyObj("ExperienceService", [
      "addExperienceForBadge",
      "getBadgeExperienceWorth",
    ]);

    bookServiceSpy.getMyReviewCount.and.resolveTo(0);
    loanServiceSpy.getMyLoanHistory.and.resolveTo([]);
    experienceServiceSpy.addExperienceForBadge.and.returnValue(0);
    experienceServiceSpy.getBadgeExperienceWorth.and.returnValue(20);

    await TestBed.configureTestingModule({
      imports: [BadgeCollectionComponent],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        {
          provide: BadgeNotificationService,
          useValue: badgeNotificationServiceSpy,
        },
        { provide: ExperienceService, useValue: experienceServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BadgeCollectionComponent);
    component = fixture.componentInstance;
  });

  it("should use a 60 second background refresh interval", async () => {
    const intervalSpy = spyOn(window, "setInterval").and.callFake(
      () => 1 as any,
    );

    fixture.detectChanges();
    await fixture.whenStable();

    expect(intervalSpy).toHaveBeenCalled();
    expect(intervalSpy.calls.mostRecent().args[1]).toBe(60000);
  });

  it("should avoid overlapping refresh requests", async () => {
    let resolveLoanHistory: (() => void) | null = null;

    loanServiceSpy.getMyLoanHistory.and.returnValue(
      new Promise<any[]>((resolve) => {
        resolveLoanHistory = () => resolve([]);
      }),
    );

    const firstRefresh = component.refreshBadges(true);
    const secondRefresh = component.refreshBadges(true);

    expect(loanServiceSpy.getMyLoanHistory).toHaveBeenCalledTimes(1);

    resolveLoanHistory?.();
    await firstRefresh;
    await secondRefresh;
  });
});
