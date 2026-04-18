import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, NgForm } from "@angular/forms";
import { AddBookComponent } from "./add-book-component";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { LEESNIVEAUS } from "./add-book-component";

class MockBookService {
  addBook = jasmine.createSpy("addBook").and.returnValue(Promise.resolve());
}

class MockSchoolService {
  getSchools = jasmine
    .createSpy("getSchools")
    .and.returnValue(Promise.resolve([{ id: 1, naam: "Testschool" }]));
  getSelectedSchoolId = jasmine
    .createSpy("getSelectedSchoolId")
    .and.returnValue(1);
  setSelectedSchoolId = jasmine.createSpy("setSelectedSchoolId");
}

describe("AddBookComponent", () => {
  let component: AddBookComponent;
  let fixture: ComponentFixture<AddBookComponent>;
  let bookService: MockBookService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AddBookComponent],
      imports: [FormsModule],
      providers: [
        { provide: BookService, useClass: MockBookService },
        { provide: SchoolService, useClass: MockSchoolService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddBookComponent);
    component = fixture.componentInstance;
    bookService = TestBed.inject(BookService) as any;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should mark form invalid and show error message", async () => {
    const mockForm = {
      invalid: true,
      control: { markAllAsTouched: jasmine.createSpy() },
    } as unknown as NgForm;

    await component.onSubmit(mockForm);

    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toContain("Controleer het formulier");
  });

  it("should call service on valid submit", async () => {
    const mockForm = {
      invalid: false,
      resetForm: jasmine.createSpy(),
    } as unknown as NgForm;

    component.book.titel = "Test Titel";
    component.book.auteur = "Test Auteur";

    spyOn<any>(component, "toBase64").and.returnValue(
      Promise.resolve("base64data"),
    );

    component.selectedCoverFile = new File([""], "test.png");

    await component.onSubmit(mockForm);

    expect(bookService.addBook).toHaveBeenCalled();
    expect(bookService.addBook).toHaveBeenCalledWith(jasmine.any(Object), 1);
    expect(component.submitState).toBe("success");
    expect(component.isSaving).toBeFalse();
  });

  it("should handle service error", async () => {
    bookService.addBook.and.returnValue(Promise.reject());

    const mockForm = {
      invalid: false,
      resetForm: jasmine.createSpy(),
    } as unknown as NgForm;

    await component.onSubmit(mockForm);

    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toContain("Opslaan mislukt");
  });

  it("should handle cover selection", () => {
    const file = new File(["data"], "cover.png", { type: "image/png" });

    const event = {
      target: {
        files: [file],
      },
    } as unknown as Event;

    spyOn(URL, "createObjectURL").and.returnValue("blob:url");

    component.onCoverSelected(event);

    expect(component.selectedCoverFile).toBe(file);
    expect(component.coverPreviewUrl).toBe("blob:url");
    expect(component.book.cover).toBe("cover.png");
  });

  it("exposes all expected leesniveau options", () => {
    expect(component.leesniveaus).toEqual(LEESNIVEAUS);
    expect(component.leesniveaus).toContain("1ste-2de leerljaar");
    expect(component.leesniveaus).toContain("3de-4de leerjaar");
    expect(component.leesniveaus).toContain("5de-6de leerjaar");
    expect(component.leesniveaus).toContain("1ste graad");
    expect(component.leesniveaus).toContain("2de graad");
    expect(component.leesniveaus).toContain("3de graad");
  });

  it("sends selected leesniveau in submit payload", async () => {
    const mockForm = {
      invalid: false,
      resetForm: jasmine.createSpy(),
    } as unknown as NgForm;

    component.book.titel = "Leesboek";
    component.book.auteur = "Auteur";
    component.book.leesniveau = "2de graad";

    await component.onSubmit(mockForm);

    expect(bookService.addBook).toHaveBeenCalledWith(
      jasmine.objectContaining({ leesniveau: "2de graad" }),
      1,
    );
  });
});
