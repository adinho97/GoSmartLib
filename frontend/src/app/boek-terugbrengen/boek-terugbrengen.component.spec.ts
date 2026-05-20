import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { BoekTerugbrengenComponent } from "../boek-terugbrengen/boek-terugbrengen.component";
import { LoanService } from "../services/loan.service";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { BarcodeService } from "../services/barcode.service";
import { of, Subject } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("BoekTerugbrengenComponent", () => {
  let component: BoekTerugbrengenComponent;
  let fixture: ComponentFixture<BoekTerugbrengenComponent>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let barcodeServiceSpy: jasmine.SpyObj<BarcodeService>;
  let barcodeScanSubject: Subject<string>;

  beforeEach(async () => {
    barcodeScanSubject = new Subject<string>();

    loanServiceSpy = jasmine.createSpyObj("LoanService", [
      "getLoansForBook",
      "returnLoan",
    ]);
    loanServiceSpy.getLoansForBook.and.returnValue(
      Promise.resolve([
        {
          id: 1,
          bookTitel: "Scanned Book",
          userSub: "student1",
          copyId: 101,
        } as any,
      ]),
    );
    loanServiceSpy.returnLoan.and.returnValue(Promise.resolve({} as any));

    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getBookByGoNumberFromLibrary",
    ]);
    bookServiceSpy.getBookByGoNumberFromLibrary.and.returnValue(
      Promise.resolve({ id: 1, titel: "Scanned Book" } as any),
    );

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);

    barcodeServiceSpy = jasmine.createSpyObj("BarcodeService", [
      "setupHiddenInput",
      "activateScanMode",
      "deactivateScanMode",
      "getScans",
      "cleanup",
    ]);
    barcodeServiceSpy.getScans.and.returnValue(
      barcodeScanSubject.asObservable(),
    );

    await TestBed.configureTestingModule({
      declarations: [BoekTerugbrengenComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: BarcodeService, useValue: barcodeServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BoekTerugbrengenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should activate scan mode on init", () => {
    component.ngOnInit();
    expect(barcodeServiceSpy.setupHiddenInput).toHaveBeenCalled();
    expect(barcodeServiceSpy.activateScanMode).toHaveBeenCalled();
  });

  it("should process scan and load loans for book", fakeAsync(() => {
    barcodeScanSubject.next("GO-123");
    tick(300); // Simulate buffer timeout in BarcodeService
    fixture.detectChanges();
    tick(); // Resolve promises

    expect(bookServiceSpy.getBookByGoNumberFromLibrary).toHaveBeenCalledWith(
      "GO-123",
      1,
    );
    expect(loanServiceSpy.getLoansForBook).toHaveBeenCalledWith(1);
    expect((component as any).scannedLoans.length).toBe(1);
    expect((component as any).scannedLoans[0].bookTitel).toBe("Scanned Book");
  }));

  it("should open return dialog for a loan", fakeAsync(() => {
    const loan = { id: 1, bookTitel: "Test Book" } as any;
    component.openReturnDialog(loan);
    tick();
    fixture.detectChanges();
    expect(component.returnDialogLoan).toEqual(loan);
    expect(component.returnDialogOpen).toBeTrue();
  }));

  it("should return loan and clear scanned loans on confirmReturnLoan", fakeAsync(() => {
    const loan = { id: 1, bookTitel: "Test Book" } as any;
    (component as any).scannedLoans = [loan];
    component.returnDialogLoan = loan;
    component.returnCondition = "GOOD";
    component.returnLostBook = false;

    component.confirmReturnLoan();
    tick();

    expect(loanServiceSpy.returnLoan).toHaveBeenCalledWith(1, {
      condition: "GOOD",
      lost: false,
    });
    expect((component as any).scannedLoans.length).toBe(0);
    expect(component.returnDialogOpen).toBeFalse();
    expect(component.successMessage).toContain("teruggebracht");
  }));
});
