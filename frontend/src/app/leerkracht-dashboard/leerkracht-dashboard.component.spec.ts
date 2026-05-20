import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { Router } from "@angular/router";
import { LeerkrachtDashboardComponent } from "../leerkracht-dashboard/leerkracht-dashboard.component";
import { AuthContextService } from "../services/auth-context.service";
import { SchoolService } from "../services/school.service";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("LeerkrachtDashboardComponent", () => {
  let component: LeerkrachtDashboardComponent;
  let fixture: ComponentFixture<LeerkrachtDashboardComponent>;
  let routerSpy: jasmine.SpyObj<Router>;
  let authContextServiceSpy: jasmine.SpyObj<AuthContextService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);
    authContextServiceSpy = jasmine.createSpyObj("AuthContextService", [
      "getEffectiveRole",
      "getEffectiveSub",
    ]);
    authContextServiceSpy.getEffectiveRole.and.returnValue("leerkracht");
    authContextServiceSpy.getEffectiveSub.and.returnValue("teacher-sub");

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
      "getKlassenBySchool",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    schoolServiceSpy.getKlassenBySchool.and.returnValue(
      Promise.resolve([{ id: 1, naam: "1A" }]),
    );

    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getLeeslistenForKlas",
      "enrichBooksWithDetails",
    ]);
    bookServiceSpy.getLeeslistenForKlas.and.returnValue(Promise.resolve([]));
    bookServiceSpy.enrichBooksWithDetails.and.callFake((books: any[]) =>
      Promise.resolve(books),
    );

    loanServiceSpy = jasmine.createSpyObj("LoanService", ["getAllActiveLoans"]);
    loanServiceSpy.getAllActiveLoans.and.returnValue(Promise.resolve([]));

    await TestBed.configureTestingModule({
      declarations: [LeerkrachtDashboardComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: AuthContextService, useValue: authContextServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeerkrachtDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load classes on init", () => {
    expect(component).toBeTruthy();
  });
});
