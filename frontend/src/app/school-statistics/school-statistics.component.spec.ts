import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { SchoolStatisticsComponent } from "./school-statistics.component";
import { BookService, SchoolStatistics } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import axios from "axios";

describe("SchoolStatisticsComponent", () => {
  let component: SchoolStatisticsComponent;
  let fixture: ComponentFixture<SchoolStatisticsComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;

  const mockStats: SchoolStatistics = {
    totalLoans: 150,
    school: "De Horizon",
    mostReadBook: { id: 1, titel: "Test Boek", auteur: "Auteur", count: 20 },
    topReader: { sub: "user-123", displayName: "user-123", count: 15 },
    topClass: { name: "6B", count: 45 },
  };

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getSchoolStatistics",
    ]);
    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);

    await TestBed.configureTestingModule({
      imports: [SchoolStatisticsComponent, RouterTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SchoolStatisticsComponent);
    component = fixture.componentInstance;
  });

  it("should show error when no school is selected", async () => {
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(null);

    await component.ngOnInit();

    expect(component.error).toContain("Geen school geselecteerd");
    expect(component.isLoading).toBeFalse();
  });

  it("should load statistics and resolve user display name on success", async () => {
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    bookServiceSpy.getSchoolStatistics.and.resolveTo(mockStats);

    // Mock axios for profile lookup
    spyOn(axios, "get").and.resolveTo({
      data: { firstName: "Jan", lastName: "Janssens" },
    });

    await component.ngOnInit();

    expect(bookServiceSpy.getSchoolStatistics).toHaveBeenCalledWith(1);
    expect(component.stats).toBeTruthy();
    expect(component.stats?.topReader?.displayName).toBe("Jan Janssens");
    expect(component.isLoading).toBeFalse();
  });

  it("should handle API errors gracefully", async () => {
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    bookServiceSpy.getSchoolStatistics.and.rejectWith(new Error("API Error"));

    await component.ngOnInit();

    expect(component.error).toContain("Fout bij het laden");
    expect(component.isLoading).toBeFalse();
  });

  it("should fallback to sub ID if profile resolution fails", async () => {
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    bookServiceSpy.getSchoolStatistics.and.resolveTo(mockStats);
    spyOn(axios, "get").and.rejectWith(new Error("Not found"));

    await component.ngOnInit();

    expect(component.stats?.topReader?.displayName).toBe("user-123");
    expect(component.isLoading).toBeFalse();
  });
});
