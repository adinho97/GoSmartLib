import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import axios from "axios"; // Import axios
import { HttpClientTestingModule } from "@angular/common/http/testing"; // Corrected path
import { LoanHistoryCatalogComponent } from "./loan-history-catalog.component";
import { LoanService } from "../services/loan.service";
import { BookService } from "../services/book.service";
import { AuthContextService } from "../services/auth-context.service";
import { of } from "rxjs";

describe("LoanHistoryCatalogComponent", () => {
  let component: LoanHistoryCatalogComponent;
  let fixture: ComponentFixture<LoanHistoryCatalogComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let authContextServiceSpy: jasmine.SpyObj<AuthContextService>;

  beforeEach(async () => {
    loanServiceSpy = jasmine.createSpyObj("LoanService", ["getLoanHistory"]);
    loanServiceSpy.getLoanHistory.and.returnValue(
      Promise.resolve([
        {
          id: 1,
          bookTitel: "Book A",
          userSub: "user1",
          returnedAt: "2023-01-01",
        } as any,
        {
          id: 2,
          bookTitel: "Book B",
          userSub: "user2",
          returnedAt: "2023-01-02",
        } as any,
      ]),
    );

    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "enrichBooksWithDetails",
    ]);
    bookServiceSpy.enrichBooksWithDetails.and.callFake((loans: any[]) =>
      Promise.resolve(loans.map((loan) => ({ ...loan, cover: "cover.jpg" }))),
    );

    authContextServiceSpy = jasmine.createSpyObj("AuthContextService", [
      "getEffectiveSub",
    ]);
    authContextServiceSpy.getEffectiveSub.and.returnValue("current-user");

    await TestBed.configureTestingModule({
      declarations: [LoanHistoryCatalogComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: AuthContextService, useValue: authContextServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanHistoryCatalogComponent);
    component = fixture.componentInstance;
    // Mock axios.get after component creation, but before detectChanges if ngOnInit uses it.
    // Or, better, mock it in the providers if it's injected.
    // Since axios is directly imported and used, we need to spy on it globally or before the component's ngOnInit.
    spyOn(axios, "get").and.callFake((url: string) => {
      if (url.includes("/api/gebruikers/leerlingen")) {
        return Promise.resolve({
          data: [
            { sub: "user1", displayName: "User One" },
            { sub: "user2", displayName: "User Two" },
          ],
        }) as any;
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`)) as any;
    });
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should filter loans by search query", fakeAsync(() => {
    component.studentSearch = "user1";
    component.onStudentSearchInput(); // Call the actual filtering method
    tick();

    expect(component.filteredStudents.length).toBe(1);
    expect(component.filteredStudents[0].sub).toBe("user1");
  }));
});
