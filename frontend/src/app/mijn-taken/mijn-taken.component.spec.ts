import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { MijnTakenComponent } from "./mijn-taken.component";
import { LoanService } from "../services/loan.service"; // Corrected path
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { UserService } from "../services/user.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("MijnTakenComponent", () => {
  let component: MijnTakenComponent;
  let fixture: ComponentFixture<MijnTakenComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  beforeEach(async () => {
    loanServiceSpy = jasmine.createSpyObj("LoanService", ["getAllActiveLoans"]);
    loanServiceSpy.getAllActiveLoans.and.returnValue(
      Promise.resolve([
        {
          id: 1,
          bookTitel: "Overdue Book",
          userSub: "student1",
          dueDate: "2023-01-01",
        } as any,
      ]),
    );

    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "enrichBooksWithDetails",
    ]);
    bookServiceSpy.enrichBooksWithDetails.and.callFake((loans: any[]) =>
      Promise.resolve(loans),
    );

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);

    userServiceSpy = jasmine.createSpyObj("UserService", ["getUserProfile"]);
    userServiceSpy.getUserProfile.and.callFake((sub: string) =>
      Promise.resolve({ firstName: sub, lastName: "User" }),
    );

    await TestBed.configureTestingModule({
      declarations: [MijnTakenComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MijnTakenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load overdue loans on init", () => {
    expect(loanServiceSpy.getAllActiveLoans).toHaveBeenCalled();
  });

  it("should calculate overdue status correctly", () => {
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);

    const loan = { dueDate: yesterday.toISOString().split("T")[0] } as any;
    expect(component.isOverdue(loan)).toBeTrue();

    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const futureLoan = { dueDate: tomorrow.toISOString().split("T")[0] } as any;
    expect(component.isOverdue(futureLoan)).toBeFalse();
  });
});
