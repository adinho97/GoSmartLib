import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { BoekTerugbrengenComponent } from "../boek-terugbrengen/boek-terugbrengen.component";
import { LoanService } from "../services/loan.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import axios from "axios";

describe("BoekTerugbrengenComponent", () => {
  let component: BoekTerugbrengenComponent;
  let fixture: ComponentFixture<BoekTerugbrengenComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;

  beforeEach(async () => {
    loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "getActiveLoans",
      "returnLoan",
      "getCopiesForBook",
    ]);
    loanServiceSpy.getActiveLoans.and.resolveTo([]);
    loanServiceSpy.returnLoan.and.resolveTo({} as any);
    loanServiceSpy.getCopiesForBook.and.resolveTo([]);

    spyOn(axios, "get").and.resolveTo({ data: [] });

    await TestBed.configureTestingModule({
      declarations: [BoekTerugbrengenComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [{ provide: LoanService, useValue: loanServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(BoekTerugbrengenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should open return dialog for a loan", fakeAsync(async () => {
    const loan = { id: 1, bookTitel: "Test Book" } as any;
    await component.openReturnDialog(loan);
    tick();
    fixture.detectChanges();
    expect(component.returnDialogLoan).toEqual(loan);
    expect(component.returnDialogOpen).toBeTrue();
  }));

  it("should return loan and clear scanned loans on confirmReturnLoan", fakeAsync(() => {
    const loan = {
      id: 1,
      bookTitel: "Test Book",
      userSub: "student1",
      bookId: 101,
      copyId: 1,
    } as any;
    component.activeLoans = [loan];
    component.returnDialogLoan = loan;
    component.returnCondition = "GOOD";
    component.returnLostBook = false;

    component.confirmReturnLoan();
    tick();

    expect(loanServiceSpy.returnLoan).toHaveBeenCalledWith(1, {
      condition: "GOOD",
      lost: false,
    });
    expect(component.activeLoans.length).toBe(0);
    expect(component.returnDialogOpen).toBeFalse();
    expect(component.successMessage).toContain("teruggebracht");
  }));
});
