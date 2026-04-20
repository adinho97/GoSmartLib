import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, NgForm } from "@angular/forms";
import { AddBookComponent } from "./add-book-component";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";
import { LEESNIVEAUS } from "./add-book-component";

class MockBookService {
  addBook = jasmine.createSpy("addBook").and.returnValue(Promise.resolve());
  fetchBookByGoNumber = jasmine
    .createSpy("fetchBookByGoNumber")
    .and.returnValue(
      Promise.resolve({
        titel: "GO Boek",
        auteur: "Auteur",
        isbn: "",
        goNumber: "GO-12345678",
        cover: "data:image/png;base64,abc",
        beschrijving: "Beschrijving",
        genre: "Fantasy",
        uitgaveDatum: "2020-01-01",
        paginas: 123,
        taal: "Nederlands",
        uitgeverij: "Uitgeverij",
        leesniveau: "2de graad",
      }),
    );
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

    expect(component.submitState).toBe("");
    expect(component.submitMessage).toBe("");
    expect(component.isSubmitted).toBeTrue();
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
    expect(component.submitMessage).toContain("Fout bij opslaan");
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

  it("should load book data from GO number", async () => {
    component.goNumberLookup = "GO-12345678";

    await component.loadBookFromGoNumber();

    expect(bookService.fetchBookByGoNumber).toHaveBeenCalledWith("GO-12345678");
    expect(component.book.titel).toBe("GO Boek");
    expect(component.book.auteur).toBe("Auteur");
    expect(component.book.goNumber).toBe("GO-12345678");
    expect(component.coverPreviewUrl).toBe("data:image/png;base64,abc");
    expect(component.submitState).toBe("success");
  });

  it("should normalize lowercase go-id before lookup", async () => {
    component.goNumberLookup = "  go-12345678  ";

    await component.loadBookFromGoNumber();

    expect(bookService.fetchBookByGoNumber).toHaveBeenCalledWith("GO-12345678");
    expect(component.submitMessage).toContain("GO-12345678");
  });

  it("should use looked-up GO-id when backend response has no goNumber", async () => {
    bookService.fetchBookByGoNumber.and.returnValue(
      Promise.resolve({
        titel: "Boek zonder goNumber in payload",
        auteur: "Auteur",
      }),
    );
    component.goNumberLookup = "go-87654321";

    await component.loadBookFromGoNumber();

    expect(component.book.goNumber).toBe("GO-87654321");
  });

  it("should show validation error when GO-id is empty", async () => {
    component.goNumberLookup = "   ";

    await component.loadBookFromGoNumber();

    expect(bookService.fetchBookByGoNumber).not.toHaveBeenCalled();
    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toBe("Voer een GO-nummer in.");
  });

  it("should show not found message for unknown GO-id", async () => {
    bookService.fetchBookByGoNumber.and.returnValue(
      Promise.reject({ response: { status: 404 } }),
    );
    component.goNumberLookup = "GO-00000000";

    await component.loadBookFromGoNumber();

    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toBe(
      "Geen boek gevonden met dit GO-nummer.",
    );
    expect(component.isLookupLoading).toBeFalse();
  });

  it("should show generic error when GO-id lookup fails", async () => {
    bookService.fetchBookByGoNumber.and.returnValue(
      Promise.reject({ response: { status: 500 } }),
    );
    component.goNumberLookup = "GO-11112222";

    await component.loadBookFromGoNumber();

    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toBe("Fout bij het ophalen van het boek.");
    expect(component.isLookupLoading).toBeFalse();
  });

  it("should send goNumber in addBook payload", async () => {
    const mockForm = {
      invalid: false,
      resetForm: jasmine.createSpy(),
    } as unknown as NgForm;

    component.book.titel = "GO Boek";
    component.book.auteur = "Auteur";
    component.book.goNumber = "GO-12345678";

    await component.onSubmit(mockForm);

    expect(bookService.addBook).toHaveBeenCalledWith(
      jasmine.objectContaining({ goNumber: "GO-12345678" }),
      1,
    );
  });
});
