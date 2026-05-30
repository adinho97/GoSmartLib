import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router, ActivatedRoute } from "@angular/router"; // Corrected path
import { MijnLijstenComponent } from "./mijn-lijsten.component";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";
import { AuthContextService } from "../services/auth-context.service";
import { SchoolService } from "../services/school.service";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { UserService } from "../services/user.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("MijnLijstenComponent", () => {
  let component: MijnLijstenComponent;
  let fixture: ComponentFixture<MijnLijstenComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let authContextServiceSpy: jasmine.SpyObj<AuthContextService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let bibbeheerderServiceSpy: jasmine.SpyObj<BibbeheerderService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let activatedRouteSpy: any;

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getUserWishlist",
      "enrichBooksWithDetails",
      "getLeeslijst",
      "getLeeslistenForKlas",
      "getMyLeeslisten",
      "getUserKlas",
      "removeFromWishlist",
      "updateWishlistNotification",
      "getHighlightedBookIds",
      "getDidacticBooks",
    ]);
    bookServiceSpy.getUserWishlist.and.returnValue(Promise.resolve([]));
    bookServiceSpy.enrichBooksWithDetails.and.callFake((books: any[]) =>
      Promise.resolve(books),
    );
    bookServiceSpy.getLeeslijst.and.returnValue(
      Promise.resolve({ id: 1, titel: "Test Leeslijst", bookIds: [] }),
    );
    bookServiceSpy.getLeeslistenForKlas.and.returnValue(Promise.resolve([]));
    bookServiceSpy.getMyLeeslisten.and.returnValue(Promise.resolve([]));
    bookServiceSpy.getHighlightedBookIds.and.returnValue(Promise.resolve([]));
    bookServiceSpy.getDidacticBooks.and.returnValue(Promise.resolve([]));
    bookServiceSpy.getUserKlas.and.returnValue(
      Promise.resolve({ klasId: 1, klasName: "1A", schoolId: 1 }),
    );
    bookServiceSpy.removeFromWishlist.and.returnValue(Promise.resolve());
    bookServiceSpy.updateWishlistNotification.and.returnValue(
      Promise.resolve({} as any),
    );

    loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "getMyActiveLoans",
      "getMyLoanHistory",
      "createLoanExtensionRequestTicket",
    ]);
    loanServiceSpy.getMyActiveLoans.and.returnValue(Promise.resolve([]));
    loanServiceSpy.getMyLoanHistory.and.returnValue(Promise.resolve([]));

    authContextServiceSpy = jasmine.createSpyObj("AuthContextService", [
      "getEffectiveRole",
      "getEffectiveSub",
    ]);
    authContextServiceSpy.getEffectiveRole.and.returnValue("leerling");
    authContextServiceSpy.getEffectiveSub.and.returnValue("test-sub");

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    bibbeheerderServiceSpy = jasmine.createSpyObj("BibbeheerderService", [
      "getLibrariansForSchool",
    ]);

    userServiceSpy = jasmine.createSpyObj("UserService", ["getUserProfile"]);
    userServiceSpy.getUserProfile.and.returnValue(Promise.resolve({}));

    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);
    activatedRouteSpy = {
      snapshot: { fragment: "verlanglijst" },
      fragment: of("verlanglijst"), // Mock fragment for initial tab selection
    };

    localStorage.clear();
    localStorage.setItem("sub", "test-sub");
    localStorage.setItem("role", "leerling");

    await TestBed.configureTestingModule({
      declarations: [MijnLijstenComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: AuthContextService, useValue: authContextServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: BibbeheerderService, useValue: bibbeheerderServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MijnLijstenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should select tab based on fragment on init", async () => {
    // ngOnInit is already called in beforeEach, which handles the fragment.
    await fixture.whenStable(); // Wait for async operations in ngOnInit
    fixture.detectChanges();
    expect(component.activeTab).toBe("verlanglijst"); // Expect 'verlanglijst' as per mock
  });

  it("should load wishlist books on init", () => {
    expect(bookServiceSpy.getUserWishlist).toHaveBeenCalled();
  });

  it("should load my reading lists on init for non-student roles", () => {
    localStorage.setItem("role", "leerkracht");
    component.ngOnInit(); // Re-initialize component
    expect(bookServiceSpy.getMyLeeslisten).toHaveBeenCalled();
  });

  it("should open the librarian message modal without fetching librarians", async () => {
    const mockLoan = { id: 123, bookTitel: "Test Boek" } as any;

    component.openLibrarianMessageModal(mockLoan);

    expect(component.showLibrarianMessageModal).toBeTrue();
    expect(component.selectedLoanForMessage).toBe(mockLoan);
  });

  it("should disable sending message if a pending request exists on the loan", () => {
    const mockLoan = { id: 123, hasPendingExtensionRequest: true } as any;
    const canSend = component.canSendMessageToLibrarian(mockLoan);
    expect(canSend).toBeFalse();
  });

  it("should return correct list type info for badges", () => {
    const globalList: any = { isGlobal: true };
    const schoolList: any = { isSchool: true };
    const ownList: any = { createdByName: "test-sub" };
    const sharedList: any = { createdByName: "other-sub" };

    expect(component.getListTypeInfo(globalList)).toEqual({
      label: "Globaal",
      class: "type-global",
    });
    expect(component.getListTypeInfo(schoolList)).toEqual({
      label: "School",
      class: "type-school",
    });
    expect(component.getListTypeInfo(ownList)).toEqual({
      label: "Persoonlijk",
      class: "type-personal",
    });
    expect(component.getListTypeInfo(sharedList)).toEqual({
      label: "Gedeeld",
      class: "type-shared",
    });
  });

  it("should deduplicate and sort leeslisten by newest first", async () => {
    const mockLists = [
      { id: 1, titel: "Oldest", createdAt: "2023-01-01T10:00:00Z" },
      { id: 2, titel: "Newest", createdAt: "2024-01-01T10:00:00Z" },
      { id: 1, titel: "Oldest Duplicate", createdAt: "2023-01-01T10:00:00Z" },
    ];
    bookServiceSpy.getMyLeeslisten.and.returnValue(Promise.resolve(mockLists));

    // Manually trigger load logic
    await (component as any).loadClassReading();

    expect(component.leeslisten.length).toBe(2);
    expect(component.leeslisten[0].id).toBe(2); // Newest first
    expect(component.leeslisten[1].id).toBe(1);
  });

  it("should format dates correctly for nl-BE", () => {
    const date = "2024-05-20";
    // Expect localized string containing 20 and mei (may)
    const formatted = component.formatDate(date);
    expect(formatted).toContain("20");
    expect(formatted.toLowerCase()).toContain("mei");
    expect(formatted).toContain("2024");
  });

  it("should return paged leeslisten correctly", () => {
    component.leeslisten = Array.from({ length: 20 }, (_, i) => ({ id: i }) as any);
    component.leeslijstPage = 1;
    
    // Page size is 12
    expect(component.pagedLeeslisten.length).toBe(12);
    expect(component.totalLeeslijstPages).toBe(2);

    component.leeslijstPage = 2;
    expect(component.pagedLeeslisten.length).toBe(8);
  });
});
