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
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { of } from "rxjs";

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
    ]);
    userPreferencesServiceSpy.getSnapshotForLegacyUse.and.returnValue({});

    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getUserWishlist",
      "enrichBooksWithDetails",
    ]);
    bookServiceSpy.getUserWishlist.and.resolveTo([]);
    bookServiceSpy.enrichBooksWithDetails.and.resolveTo([]);

    const smartschoolServiceSpy = jasmine.createSpyObj("SmartschoolService", ["sendMessage"]);
    const loanServiceSpy = jasmine.createSpyObj("LoanService", ["getMyLoanHistory", "getActiveLoans"]);
    loanServiceSpy.getMyLoanHistory.and.resolveTo([]);
    loanServiceSpy.getActiveLoans.and.resolveTo([]);
    const experienceServiceSpy = jasmine.createSpyObj("ExperienceService", ["levelInfo$"]);
    experienceServiceSpy.levelInfo$ = of({ level: 1 });
    const uiToastServiceSpy = jasmine.createSpyObj("UiToastService", ["success", "error"]);

    await TestBed.configureTestingModule({
      declarations: [ProfileComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: UserPreferencesService, useValue: userPreferencesServiceSpy },
        { provide: SmartschoolService, useValue: smartschoolServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: ExperienceService, useValue: experienceServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
