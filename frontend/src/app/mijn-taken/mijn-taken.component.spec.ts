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
    loanServiceSpy = jasmine.createSpyObj("LoanService", ["getActiveLoans"]);
    loanServiceSpy.getActiveLoans.and.returnValue(
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

  it("should load overdue loans on init", fakeAsync(() => {
    // Selecting a student triggers the loan loading
    const mockStudent = {
      sub: "student1",
      displayName: "Student 1",
      klas: "1A",
    };
    component.selectStudentForExtension(mockStudent);
    tick();
    expect(loanServiceSpy.getActiveLoans).toHaveBeenCalledWith("student1");
  }));

  it("should calculate overdue status correctly", () => {
    // Ensure both are valid ISO date strings or Date objects depending on implementation
    component.today = "2023-01-02";
    const loan = { dueDate: "2023-01-01" } as any;
    expect(component.isOverdue(loan.dueDate)).toBeTrue();

    const tomorrow = "2023-01-03";
    const futureLoan = { dueDate: tomorrow } as any;
    expect(component.isOverdue(futureLoan.dueDate)).toBeFalse();
  });
});
