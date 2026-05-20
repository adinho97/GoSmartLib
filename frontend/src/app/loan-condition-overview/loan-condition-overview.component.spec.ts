import { ComponentFixture, TestBed } from "@angular/core/testing";
import { LoanConditionOverviewComponent } from "./loan-condition-overview.component"; // Assuming this component exists
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { LoanService } from "../services/loan.service";
import { of } from "rxjs"; // Corrected path
import { FormsModule } from "@angular/forms";

describe("LoanConditionOverviewComponent", () => {
  let component: LoanConditionOverviewComponent;
  let fixture: ComponentFixture<LoanConditionOverviewComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;

  beforeEach(async () => {
    loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "getConditionOverview",
      "updateCopyState",
    ]);
    loanServiceSpy.getConditionOverview.and.returnValue(
      Promise.resolve({
        worsenedReturns: [],
        bookStates: [],
        lostCopies: [],
      }),
    );
    loanServiceSpy.updateCopyState.and.returnValue(Promise.resolve());

    await TestBed.configureTestingModule({
      declarations: [LoanConditionOverviewComponent], // Assuming it's NOT standalone
      imports: [HttpClientTestingModule, FormsModule],
      providers: [{ provide: LoanService, useValue: loanServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanConditionOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
