import { CommonModule } from "@angular/common";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import axios from "axios";
import { DatePipe } from "@angular/common";

import { LoanOverviewComponent } from "./loan-overview.component";
import { Loan, LoanService } from "../services/loan.service";

describe("LoanOverviewComponent", () => {
  let component: LoanOverviewComponent;
  let fixture: ComponentFixture<LoanOverviewComponent>;
  let mockLoanService: jasmine.SpyObj<LoanService>;

  const getRelativeDateString = (days: number): string => {
    const d = new Date();
    d.setDate(d.getDate() + days);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  };

  beforeEach(async () => {
    mockLoanService = jasmine.createSpyObj("LoanService", [
      "getAllActiveLoans",
    ]);
    mockLoanService.getAllActiveLoans.and.callFake(() =>
      Promise.resolve([
        {
          id: 1,
          copyId: 11,
          bookId: 101,
          bookTitel: "Dune",
          bookCover: "",
          userSub: "student-1",
          loanedAt: getRelativeDateString(-30),
          dueDate: getRelativeDateString(-5), // Overdue
          returnedAt: null,
        },
        {
          id: 2,
          copyId: 12,
          bookId: 102,
          bookTitel: "Clean Code",
          bookCover: "",
          userSub: "student-2",
          loanedAt: getRelativeDateString(-10),
          dueDate: getRelativeDateString(5), // On time
          returnedAt: null,
        },
      ] as Loan[]),
    );

    spyOn(axios, "get").and.callFake((url: string) => {
      if (url.includes("student-1")) {
        return Promise.resolve({
          data: { firstName: "Mila", lastName: "Vermeulen" },
        }) as any;
      }

      if (url.includes("student-2")) {
        return Promise.resolve({
          data: { firstName: "Noah", lastName: "Peeters" },
        }) as any;
      }

      return Promise.reject(new Error(`Unexpected URL: ${url}`)) as any;
    });

    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [LoanOverviewComponent],
      providers: [
        { provide: LoanService, useValue: mockLoanService },
        DatePipe,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("loadLoans enriches active loans and computes overdue state", async () => {
    await component.loadLoans();

    expect(mockLoanService.getAllActiveLoans).toHaveBeenCalled();
    expect(component.allLoans.length).toBe(2);
    expect(component.filteredLoans.length).toBe(2);

    const overdueLoan = component.allLoans[0];
    expect(overdueLoan.userName).toBe("Mila Vermeulen");
    expect(overdueLoan.isOverdue).toBeTrue();
    expect(overdueLoan.daysUntilDue).toBeLessThan(0);

    const onTimeLoan = component.allLoans[1];
    expect(onTimeLoan.userName).toBe("Noah Peeters");
    expect(onTimeLoan.isOverdue).toBeFalse();
    expect(component.getOverdueCount()).toBe(1);
    expect(component.getOntimeCount()).toBe(1);
  });

  it("filters loans by status and search query", async () => {
    await component.loadLoans();

    component.filterStatus = "overdue";
    component.onFilterChange();
    expect(component.filteredLoans.length).toBe(1);
    expect(component.filteredLoans[0].bookTitel).toBe("Dune");

    component.filterStatus = "all";
    component.searchQuery = "clean";
    component.onSearch();
    expect(component.filteredLoans.length).toBe(1);
    expect(component.filteredLoans[0].bookTitel).toBe("Clean Code");
  });

  it("formats remaining days labels", () => {
    expect(component.getDaysLabel(-2)).toBe("2 dagen te laat");
    expect(component.getDaysLabel(0)).toBe("Vandaag");
    expect(component.getDaysLabel(1)).toBe("1 dag resterend");
  });
});
