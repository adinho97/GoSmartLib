import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { OntleningenVerlengenComponent } from "./ontleningen-verlengen.component";
import { LoanService } from "../services/loan.service";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("OntleningenVerlengenComponent", () => {
  let component: OntleningenVerlengenComponent;
  let fixture: ComponentFixture<OntleningenVerlengenComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let bibbeheerderServiceSpy: jasmine.SpyObj<BibbeheerderService>;

  beforeEach(async () => {
    loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "getActiveLoans",
      "updateLoanDueDate",
    ]);
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
    loanServiceSpy.updateLoanDueDate.and.resolveTo({} as any);

    bibbeheerderServiceSpy = jasmine.createSpyObj("BibbeheerderService", [
      "getKlassen",
      "getAllUsers",
    ]);
    bibbeheerderServiceSpy.getKlassen.and.returnValue(of([]));
    bibbeheerderServiceSpy.getAllUsers.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [OntleningenVerlengenComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: BibbeheerderService, useValue: bibbeheerderServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OntleningenVerlengenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("loads loans for the selected student", fakeAsync(() => {
    const mockStudent = {
      sub: "student1",
      displayName: "Student 1",
      klas: "1A",
    };
    component.selectStudentForExtension(mockStudent);
    tick();
    expect(loanServiceSpy.getActiveLoans).toHaveBeenCalledWith("student1");
  }));

  it("calculates overdue status correctly", () => {
    component.today = "2023-01-02";
    expect(component.isOverdue("2023-01-01")).toBeTrue();
    expect(component.isOverdue("2023-01-03")).toBeFalse();
  });
});
