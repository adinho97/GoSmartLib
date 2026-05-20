import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ProfileComponent } from "./profile.component";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { Location } from "@angular/common";
import { BookService } from "../services/book.service";
import { UserPreferencesService } from "../services/user-preferences.service";
import { SmartschoolService } from "../services/smartschool.service";
import { LoanService } from "../services/loan.service";
import { ExperienceService } from "../services/experience.service";
import { UiToastService } from "../services/ui-toast.service";
import { SchoolService } from "../services/school.service";
import { DashboardConfigService } from "../services/dashboard-config.service";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { of, Subject } from "rxjs";
import { LevelInfo } from "../services/experience.service";

describe("ProfileComponent", () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let userPreferencesServiceSpy: jasmine.SpyObj<UserPreferencesService>;

  let routerSpy = jasmine.createSpyObj("Router", ["navigate"]);
  let locationSpy = jasmine.createSpyObj("Location", ["back"]);

  beforeEach(async () => {
    userPreferencesServiceSpy = jasmine.createSpyObj("UserPreferencesService", [
      "savePreference",
      "getSnapshotForLegacyUse",
      "clearCache",
      "loadPreferencesFromBackend", // Added missing mock
      "preferences$", // Added missing mock
    ]);
    userPreferencesServiceSpy.getSnapshotForLegacyUse.and.returnValue({});
    userPreferencesServiceSpy.preferences$ = of({});

    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getUserWishlist",
      "enrichBooksWithDetails",
      "getClassReadingListItemIds",
      "getHighlightedBookIds",
      "getMyReviewCount", // Added missing mock for badge logic
      "removeFromWishlist", // Added missing mock
      "updateWishlistNotification", // Added missing mock
    ]);
    bookServiceSpy.getUserWishlist.and.resolveTo([]);
    bookServiceSpy.enrichBooksWithDetails.and.resolveTo([]);
    bookServiceSpy.getClassReadingListItemIds.and.resolveTo([]);
    bookServiceSpy.getHighlightedBookIds.and.resolveTo([]);
    bookServiceSpy.getMyReviewCount.and.resolveTo(0);
    bookServiceSpy.removeFromWishlist.and.resolveTo();
    bookServiceSpy.updateWishlistNotification.and.resolveTo({} as any);
    (bookServiceSpy as any).wishlistChanged$ =
      new Subject<void>().asObservable();

    const smartschoolServiceSpy = jasmine.createSpyObj("SmartschoolService", [
      "sendMessage",
    ]);
    smartschoolServiceSpy.sendMessage.and.returnValue(of(undefined));

    const loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "getMyLoanHistory",
      "getActiveLoans",
    ]);
    loanServiceSpy.getMyLoanHistory.and.resolveTo([]);
    loanServiceSpy.getActiveLoans.and.resolveTo([]);
    const experienceServiceSpy = jasmine.createSpyObj("ExperienceService", [
      "addExperienceForBadge", // Added missing mock
      "getBadgeExperienceWorth", // Added missing mock
      "reconcileLoanExperienceFromHistory", // Added missing mock
      "reconcileReviewExperienceFromHistory", // Added missing mock
      "levelInfo$",
    ]);
    experienceServiceSpy.levelInfo$ = of({
      level: 1,
      totalExperience: 0,
      experienceForCurrentLevel: 0,
      experienceRequiredForLevel: 100,
      progressPercentage: 0,
    }); // Provide a complete LevelInfo object
    const uiToastServiceSpy = jasmine.createSpyObj("UiToastService", [
      "success",
      "error",
    ]);

    const schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    const dashboardConfigServiceSpy = jasmine.createSpyObj(
      "DashboardConfigService",
      ["init", "config$"], // Added config$ to mock
    );
    dashboardConfigServiceSpy.config$ = of({ tiles: [], pages: {} });
    dashboardConfigServiceSpy.init.and.resolveTo();

    await TestBed.configureTestingModule({
      declarations: [ProfileComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy },
        { provide: BookService, useValue: bookServiceSpy },
        {
          provide: UserPreferencesService,
          useValue: userPreferencesServiceSpy,
        },
        { provide: SmartschoolService, useValue: smartschoolServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: ExperienceService, useValue: experienceServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // Trigger ngOnInit and initial data binding
  });

  it("should create", () => {
    expect(component).toBeTruthy(); // This now implicitly checks if ngOnInit completes without errors
  });
});
