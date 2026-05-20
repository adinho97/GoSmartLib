import { ComponentFixture, TestBed } from "@angular/core/testing";
import { LoanPageComponent } from "./loan-page.component";
import { HttpClientTestingModule } from "@angular/common/http/testing"; // Corrected path
import { LoanService } from "../services/loan.service";
import { SchoolService } from "../services/school.service";
import { BookService } from "../services/book.service";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { of } from "rxjs";

describe("LoanPageComponent", () => {
  let component: LoanPageComponent;
  let fixture: ComponentFixture<LoanPageComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;

  beforeEach(async () => {
    loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "createLoan",
      "returnLoan",
      "getLoansForBook",
      "getActiveLoans",
    ]);
    loanServiceSpy.createLoan.and.returnValue(Promise.resolve({} as any));
    loanServiceSpy.returnLoan.and.returnValue(Promise.resolve({} as any));
    loanServiceSpy.getLoansForBook.and.returnValue(Promise.resolve([]));
    loanServiceSpy.getActiveLoans.and.returnValue(Promise.resolve([]));
    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
      "getDefaultLoanDays",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    schoolServiceSpy.getDefaultLoanDays.and.returnValue(Promise.resolve(14));
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getBookByGoNumberFromLibrary",
    ]);
    bookServiceSpy.getBookByGoNumberFromLibrary.and.returnValue(
      Promise.resolve({} as any),
    );

    await TestBed.configureTestingModule({
      declarations: [LoanPageComponent], // It's a non-standalone component
      imports: [HttpClientTestingModule, FormsModule, CommonModule],
      providers: [
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
