import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { StatistiekenComponent } from "./statistieken.component";
import { BookService } from "../services/book.service"; // Corrected path
import { SchoolService } from "../services/school.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("StatistiekenComponent", () => {
  let component: StatistiekenComponent;
  let fixture: ComponentFixture<StatistiekenComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;

  const mockSchoolStats = {
    mostReadBook: { id: 1, titel: "Most Read", auteur: "Author", count: 10 },
    topReader: { sub: "reader1", displayName: "Top Reader", count: 5 },
    topClass: { name: "Class A", count: 20 },
    totalLoans: 100,
    school: "Test School",
  };

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getSchoolStatistics",
    ]);
    bookServiceSpy.getSchoolStatistics.and.returnValue(
      Promise.resolve(mockSchoolStats),
    );

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
      "getSchools",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    schoolServiceSpy.getSchools.and.returnValue(
      Promise.resolve([{ id: 1, naam: "Test School" }]),
    );

    await TestBed.configureTestingModule({
      declarations: [StatistiekenComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StatistiekenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
