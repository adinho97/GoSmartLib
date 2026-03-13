import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { AddIsbnComponent } from "./add-isbn.component";
import { BookService } from "../services/book.service";
import { SchoolService } from "../services/school.service";

class MockBookService {
  fetchBookByIsbn = jasmine
    .createSpy("fetchBookByIsbn")
    .and.returnValue(
      Promise.resolve({
        titel: "Dune",
        auteur: "Frank Herbert",
        isbn: "9780553808049",
      }),
    );

  isBookInLibrary = jasmine
    .createSpy("isBookInLibrary")
    .and.returnValue(Promise.resolve(false));

  importBookByIsbn = jasmine
    .createSpy("importBookByIsbn")
    .and.returnValue(
      Promise.resolve({
        titel: "Dune",
        auteur: "Frank Herbert",
        isbn: "9780553808049",
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

describe("AddIsbnComponent", () => {
  let component: AddIsbnComponent;
  let fixture: ComponentFixture<AddIsbnComponent>;
  let bookService: MockBookService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AddIsbnComponent],
      imports: [FormsModule, CommonModule],
      providers: [
        { provide: BookService, useClass: MockBookService },
        { provide: SchoolService, useClass: MockSchoolService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddIsbnComponent);
    component = fixture.componentInstance;
    bookService = TestBed.inject(BookService) as any;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should start with no book, no messages, and button hidden", () => {
    expect(component.book).toBeNull();
    expect(component.errorMessage).toBe("");
    expect(component.successMessage).toBe("");
    expect(component.hasCheckedLibraryStatus).toBeFalse();
    expect(component.isAlreadyInLibrary).toBeFalse();
  });

  // ---- zoekBoek -----------------------------------------------------------

  it("zoekBoek should show error and not call service when isbn is blank", async () => {
    component.isbn = "   ";
    await component.zoekBoek();

    expect(component.errorMessage).toBe("Voer een ISBN-nummer in.");
    expect(bookService.fetchBookByIsbn).not.toHaveBeenCalled();
    expect(component.book).toBeNull();
  });

  it("zoekBoek should fetch book and check library status", async () => {
    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(bookService.fetchBookByIsbn).toHaveBeenCalledWith("9780553808049");
    expect(bookService.isBookInLibrary).toHaveBeenCalledWith(
      "9780553808049",
      1,
    );
    expect(component.book).toBeTruthy();
    expect(component.book.titel).toBe("Dune");
    expect(component.hasCheckedLibraryStatus).toBeTrue();
    expect(component.isLoading).toBeFalse();
  });

  it("zoekBoek should set isAlreadyInLibrary and successMessage when book is in library", async () => {
    bookService.isBookInLibrary.and.returnValue(Promise.resolve(true));
    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(component.isAlreadyInLibrary).toBeTrue();
    expect(component.successMessage).toBe("Reeds in de bibliotheek.");
    expect(component.hasCheckedLibraryStatus).toBeTrue();
  });

  it("zoekBoek should show 404 error message when book not found", async () => {
    bookService.fetchBookByIsbn.and.returnValue(
      Promise.reject({ response: { status: 404 } }),
    );
    component.isbn = "0000000000000";
    await component.zoekBoek();

    expect(component.errorMessage).toBe(
      "Geen boek gevonden voor dit ISBN-nummer.",
    );
    expect(component.book).toBeNull();
    expect(component.isLoading).toBeFalse();
  });

  it("zoekBoek should show generic error message on unexpected failure", async () => {
    bookService.fetchBookByIsbn.and.returnValue(
      Promise.reject(new Error("Network error")),
    );
    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(component.errorMessage).toBe(
      "Er ging iets mis bij het ophalen van het boek.",
    );
    expect(component.isLoading).toBeFalse();
  });

  it("zoekBoek should reset previous state on new search", async () => {
    component.book = { titel: "Old Book" };
    component.successMessage = "Oude melding";
    component.errorMessage = "Oude fout";
    component.isAlreadyInLibrary = true;
    component.hasCheckedLibraryStatus = true;

    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(component.successMessage).toBe("");
    expect(component.errorMessage).toBe("");
  });

  // ---- voegToeAanBibliotheek ----------------------------------------------

  it("voegToeAanBibliotheek should call importBookByIsbn and set success state", async () => {
    component.book = {
      titel: "Dune",
      auteur: "Frank Herbert",
      isbn: "9780553808049",
    };
    component.hasCheckedLibraryStatus = true;
    component.isAlreadyInLibrary = false;

    await component.voegToeAanBibliotheek();

    expect(bookService.importBookByIsbn).toHaveBeenCalledWith(
      "9780553808049",
      1,
    );
    expect(component.successMessage).toBe("Boek toegevoegd aan bibliotheek.");
    expect(component.isAlreadyInLibrary).toBeTrue();
    expect(component.hasCheckedLibraryStatus).toBeTrue();
    expect(component.isImporting).toBeFalse();
  });

  it("voegToeAanBibliotheek should not call service when book already in library", async () => {
    component.isAlreadyInLibrary = true;

    await component.voegToeAanBibliotheek();

    expect(bookService.importBookByIsbn).not.toHaveBeenCalled();
    expect(component.successMessage).toBe("Reeds in de bibliotheek.");
  });

  it("voegToeAanBibliotheek should show generic error when service fails", async () => {
    component.book = {
      titel: "Dune",
      auteur: "Frank Herbert",
      isbn: "9780553808049",
    };
    component.hasCheckedLibraryStatus = true;
    component.isAlreadyInLibrary = false;
    bookService.importBookByIsbn.and.returnValue(
      Promise.reject({ response: { status: 500 } }),
    );

    await component.voegToeAanBibliotheek();

    expect(component.errorMessage).toBe(
      "Er ging iets mis bij het toevoegen aan de bibliotheek.",
    );
    expect(component.isImporting).toBeFalse();
  });

  it("voegToeAanBibliotheek should show 404 error when not found during import", async () => {
    component.book = {
      titel: "Dune",
      auteur: "Frank Herbert",
      isbn: "9780553808049",
    };
    component.hasCheckedLibraryStatus = true;
    component.isAlreadyInLibrary = false;
    bookService.importBookByIsbn.and.returnValue(
      Promise.reject({ response: { status: 404 } }),
    );

    await component.voegToeAanBibliotheek();

    expect(component.errorMessage).toBe("Boek niet gevonden om te importeren.");
    expect(component.isImporting).toBeFalse();
  });
});
