import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";

import { LeeslijstCreateComponent } from "./leeslijst-create.component";
import { SchoolService } from "./services/school.service";
import { BookService } from "./services/book.service";
import { UiToastService } from "./services/ui-toast.service";

describe("LeeslijstCreateComponent", () => {
  let component: LeeslijstCreateComponent;
  let fixture: ComponentFixture<LeeslijstCreateComponent>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let uiToastSpy: jasmine.SpyObj<UiToastService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    schoolServiceSpy = jasmine.createSpyObj<SchoolService>("SchoolService", [
      "getSelectedSchoolId",
      "getKlassenBySchool",
    ]);
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBooksPage",
      "saveClassReadingList",
    ]);
    uiToastSpy = jasmine.createSpyObj<UiToastService>("UiToastService", [
      "error",
      "success",
    ]);
    routerSpy = jasmine.createSpyObj<Router>("Router", ["navigate"]);

    schoolServiceSpy.getKlassenBySchool.and.resolveTo([]);
    bookServiceSpy.getBooksPage.and.resolveTo({ items: [], total: 0 });
    bookServiceSpy.saveClassReadingList.and.resolveTo();

    await TestBed.configureTestingModule({
      declarations: [LeeslijstCreateComponent],
      imports: [FormsModule],
      providers: [
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: UiToastService, useValue: uiToastSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeeslijstCreateComponent);
    component = fixture.componentInstance;
  });

  it("should redirect to the dashboard when no school is selected", async () => {
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(null);

    await component.ngOnInit();

    expect(uiToastSpy.error).toHaveBeenCalledWith("Geen school geselecteerd.");
    expect(routerSpy.navigate).toHaveBeenCalledWith(["/dashboard"]);
  });

  it("should save the selected klassen and boeken as a leeslijst", async () => {
    component.selectedKlassenIds.add(3);
    component.selectedKlassenIds.add(8);
    component.selectedBookIds.add(21);
    component.selectedBookIds.add(34);

    await component.saveLeeslijst();

    expect(bookServiceSpy.saveClassReadingList).toHaveBeenCalledWith({
      klassenIds: [3, 8],
      bookIds: [21, 34],
    });
    expect(uiToastSpy.success).toHaveBeenCalledWith(
      "Leeslijst succesvol aangemaakt.",
    );
    expect(routerSpy.navigate).toHaveBeenCalledWith(["/dashboard"]);
    expect(component.isSaving).toBeFalse();
  });

  it("should block the class selection flow when no klas is selected", () => {
    component.goToBooks();

    expect(uiToastSpy.error).toHaveBeenCalledWith(
      "Selecteer minstens één klas.",
    );
    expect(component.step).toBe("klassen");
  });

  it("should block the confirmation step when no books are selected", () => {
    component.selectedKlassenIds.add(1);

    component.goToConfirmation();

    expect(uiToastSpy.error).toHaveBeenCalledWith(
      "Selecteer minstens één boek.",
    );
    expect(component.step).toBe("klassen");
  });
});
