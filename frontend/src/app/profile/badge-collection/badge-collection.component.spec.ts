import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
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
    // Mock setInterval globally to prevent tests from hanging on async ngOnInit macrotasks
    spyOn(window, "setInterval").and.returnValue(123 as any);

    bookServiceSpy = jasmine.createSpyObj("BookService", ["getMyReviewCount"]);
    loanServiceSpy = jasmine.createSpyObj("LoanService", ["getMyLoanHistory"]);
    badgeNotificationServiceSpy = jasmine.createSpyObj(
      "BadgeNotificationService",
      ["showBadgeNotification"],
    );
    experienceServiceSpy = jasmine.createSpyObj("ExperienceService", [
      "addExperienceForBadge",
      "getBadgeExperienceWorth",
      "reconcileLoanExperienceFromHistory",
      "reconcileReviewExperienceFromHistory",
    ]);

    bookServiceSpy.getMyReviewCount.and.resolveTo(0);
    loanServiceSpy.getMyLoanHistory.and.resolveTo([]);
    experienceServiceSpy.addExperienceForBadge.and.returnValue(0);
    experienceServiceSpy.getBadgeExperienceWorth.and.returnValue(20);
    experienceServiceSpy.reconcileLoanExperienceFromHistory.and.stub();
    experienceServiceSpy.reconcileReviewExperienceFromHistory.and.stub();

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
    fixture.detectChanges();
    await fixture.whenStable();

    expect(window.setInterval).toHaveBeenCalled();
    expect(window.setInterval).toHaveBeenCalledWith(
      jasmine.any(Function),
      60000,
    );
  });

  it("should avoid overlapping refresh requests", async () => {
    let resolveLoanHistory: (() => void) | undefined;

    loanServiceSpy.getMyLoanHistory.and.returnValue(
      new Promise<any[]>((resolve) => {
        resolveLoanHistory = () => resolve([]);
      }),
    );

    const firstRefresh = component.refreshBadges(true);
    const secondRefresh = component.refreshBadges(true);

    expect(loanServiceSpy.getMyLoanHistory).toHaveBeenCalledTimes(1);

    if (resolveLoanHistory) {
      resolveLoanHistory();
    }
    await firstRefresh;
    await secondRefresh;
  });

  it("should reconcile loan XP from history on init", fakeAsync(() => {
    loanServiceSpy.getMyLoanHistory.and.resolveTo([{} as any, {} as any]);

    fixture.detectChanges();
    tick(); // Process first batch of awaits in ngOnInit
    tick(); // Process second batch

    expect(
      experienceServiceSpy.reconcileLoanExperienceFromHistory,
    ).toHaveBeenCalledWith(2);
  }));
});
