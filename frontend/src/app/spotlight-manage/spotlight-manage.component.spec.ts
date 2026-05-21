import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import {
  HttpClientTestingModule,
  HttpTestingController,
} from "@angular/common/http/testing";
import { SpotlightManageComponent } from "../spotlight-manage/spotlight-manage.component";
import { SchoolService } from "../services/school.service";
import { BookService } from "../services/book.service";
import { UiToastService } from "../services/ui-toast.service";
import { of } from "rxjs";

describe("SpotlightManageComponent", () => {
  let component: SpotlightManageComponent;
  let fixture: ComponentFixture<SpotlightManageComponent>;
  let httpTestingController: HttpTestingController;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let uiToastServiceSpy: jasmine.SpyObj<UiToastService>;

  beforeEach(async () => {
    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);

    bookServiceSpy = jasmine.createSpyObj("BookService", ["getBookById"]);
    bookServiceSpy.getBookById.and.returnValue(
      of({ id: 101, titel: "Spotlight Book", auteur: "Author" } as any),
    );

    uiToastServiceSpy = jasmine.createSpyObj("UiToastService", [
      "success",
      "error",
    ]);

    await TestBed.configureTestingModule({
      imports: [FormsModule, HttpClientTestingModule, SpotlightManageComponent],
      providers: [
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SpotlightManageComponent);
    component = fixture.componentInstance;
    component.schoolId = 1; // Set schoolId for tests
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should save spotlight book for month", fakeAsync(() => {
    const book = {
      id: 101,
      titel: "Spotlight Book",
      auteur: "Author",
      cover: "cover.jpg",
    };
    component.pickerType = "MAAND";
    component.selectBook(book);
    tick();

    const req = httpTestingController.expectOne("/api/spotlight/1/MAAND");
    expect(req.request.method).toBe("PUT");
    req.flush({});
    tick(); // Ensure subscription logic runs
    tick();

    expect(uiToastServiceSpy.success).toHaveBeenCalled();
  }));

  it("should remove spotlight book for month", fakeAsync(() => {
    component.spotlight.maand = {
      bookId: 101,
      titel: "Spotlight Book",
      auteur: "Author",
      cover: "cover.jpg",
    };
    component.clearSpotlight("MAAND");
    tick();

    const req = httpTestingController.expectOne("/api/spotlight/1/MAAND");
    expect(req.request.method).toBe("DELETE");
    req.flush({});
    tick(); // Ensure subscription logic runs
    tick();

    expect(uiToastServiceSpy.success).toHaveBeenCalled();
    expect(component.spotlight.maand).toBeNull();
  }));
});
