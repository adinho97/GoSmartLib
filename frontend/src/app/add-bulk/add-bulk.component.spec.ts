import { ComponentFixture, TestBed } from "@angular/core/testing";
import { AddBulkComponent } from "./add-bulk.component"; // Assuming this component exists
import { FormsModule } from "@angular/forms";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { UiToastService } from "../services/ui-toast.service";

describe("AddBulkComponent", () => {
  let component: AddBulkComponent;
  let fixture: ComponentFixture<AddBulkComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let uiToastServiceSpy: jasmine.SpyObj<UiToastService>;

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "importBooksByUpload",
    ]);
    bookServiceSpy.importBooksByUpload.and.returnValue(
      Promise.resolve({
        totalRows: 0,
        uniqueIsbnsProcessed: 0,
        duplicateRowsSkipped: 0,
        totalCopiesAdded: 0,
        results: [],
      }),
    );
    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "selectUserDefaultSchool",
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.selectUserDefaultSchool.and.returnValue(Promise.resolve());
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    uiToastServiceSpy = jasmine.createSpyObj("UiToastService", [
      "success",
      "error",
    ]);
    await TestBed.configureTestingModule({
      declarations: [AddBulkComponent], // Assuming it's NOT standalone
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddBulkComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
