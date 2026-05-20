import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { KlasleeslijstBeheerComponent } from "../klasleeslijst-beheer/klasleeslijst-beheer.component";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { AuthContextService } from "../services/auth-context.service";
import { of, Subject } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("KlasleeslijstBeheerComponent", () => {
  let component: KlasleeslijstBeheerComponent;
  let fixture: ComponentFixture<KlasleeslijstBeheerComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let authContextServiceSpy: jasmine.SpyObj<AuthContextService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getLeeslisten",
      "deleteLeeslijst",
      "createLeeslijst",
    ]);
    bookServiceSpy.getLeeslisten.and.returnValue(
      Promise.resolve([
        { id: 1, titel: "Lijst 1", klasIds: [1] },
        { id: 2, titel: "Lijst 2", klasIds: [2] },
      ]),
    );
    bookServiceSpy.deleteLeeslijst.and.returnValue(Promise.resolve());
    bookServiceSpy.createLeeslijst.and.returnValue(
      Promise.resolve({ id: 3, titel: "Nieuwe Lijst", klasIds: [] }),
    );

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
      "getKlassenBySchool",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);
    schoolServiceSpy.getKlassenBySchool.and.returnValue(
      Promise.resolve([
        { id: 1, naam: "Klas 1" },
        { id: 2, naam: "Klas 2" },
      ]),
    );

    authContextServiceSpy = jasmine.createSpyObj("AuthContextService", [
      "getEffectiveRole",
    ]);
    authContextServiceSpy.getEffectiveRole.and.returnValue("leerkracht");

    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [KlasleeslijstBeheerComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: AuthContextService, useValue: authContextServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KlasleeslijstBeheerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load reading lists and classes on init", () => {
    expect(schoolServiceSpy.getKlassenBySchool).toHaveBeenCalledWith(1);
    expect(component.klassen.length).toBe(2);
  });

  it("should save a new reading list for the entire school", async () => {
    component.titel = "Schoolbrede Lijst";
    component.toewijzingType = "SCHOOL";
    component.selectedKlasId = null;

    await component.saveLeeslijst();

    expect(bookServiceSpy.createLeeslijst).toHaveBeenCalledWith(
      "Schoolbrede Lijst",
      "",
      [],
      null,
      true,
      [],
      1,
    );
    expect(bookServiceSpy.getLeeslisten).toHaveBeenCalled();
    expect(component.titel).toBe("");
    expect(component.toewijzingType).toBe("KLAS");
    expect(component.selectedKlasId).toBeNull();
  });

  it("should show error if title is empty", async () => {
    component.titel = "";
    await component.saveLeeslijst();

    expect(component.error).toBe("Titel is verplicht.");
    expect(bookServiceSpy.createLeeslijst).not.toHaveBeenCalled();
  });

  it("should show error if no class is selected and not assigning to entire school", async () => {
    component.titel = "Test Lijst";
    component.toewijzingType = "KLAS";
    component.selectedKlasId = null;
    await component.saveLeeslijst();
    expect(component.error).toBe("Selecteer een klas.");
    expect(bookServiceSpy.createLeeslijst).not.toHaveBeenCalled();
  });
});
