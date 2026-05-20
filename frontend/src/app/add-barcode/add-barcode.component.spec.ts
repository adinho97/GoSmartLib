import { ComponentFixture, TestBed } from "@angular/core/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { AddBarcodeComponent } from "./add-barcode.component"; // Assuming this component exists
import { BarcodeService } from "../services/barcode.service";
import { BookService } from "../services/book.service"; // Corrected path
import { LoanService } from "../services/loan.service";
import { SchoolService } from "../services/school.service";
import { of } from "rxjs";

describe("AddBarcodeComponent", () => {
  let component: AddBarcodeComponent;
  let fixture: ComponentFixture<AddBarcodeComponent>;
  let barcodeServiceSpy: jasmine.SpyObj<BarcodeService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;

  beforeEach(async () => {
    barcodeServiceSpy = jasmine.createSpyObj("BarcodeService", [
      "setupHiddenInput",
      "activateScanMode",
      "deactivateScanMode",
      "getScans",
      "cleanup",
    ]);
    barcodeServiceSpy.getScans.and.returnValue(of("test-barcode"));
    bookServiceSpy = jasmine.createSpyObj("BookService", ["getBookById"]);
    bookServiceSpy.getBookById.and.returnValue(
      of({ id: 1, titel: "Test Book" } as any),
    );
    loanServiceSpy = jasmine.createSpyObj("LoanService", ["addCopy"]);
    loanServiceSpy.addCopy.and.returnValue(Promise.resolve());
    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, AddBarcodeComponent], // Assuming it's standalone
      providers: [
        { provide: BarcodeService, useValue: barcodeServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddBarcodeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
