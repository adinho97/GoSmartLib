import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { AddIsbnComponent } from "./add-isbn.component";
import { ItemService } from "../item.service";

class MockItemService {
  fetchBoekByIsbn = jasmine
    .createSpy("fetchBoekByIsbn")
    .and.returnValue(
      Promise.resolve({ titel: "Dune", auteur: "Frank Herbert", isbn: "9780553808049" })
    );

  bestaatBoekInBibliotheek = jasmine
    .createSpy("bestaatBoekInBibliotheek")
    .and.returnValue(Promise.resolve(false));

  importBoekByIsbn = jasmine
    .createSpy("importBoekByIsbn")
    .and.returnValue(
      Promise.resolve({ titel: "Dune", auteur: "Frank Herbert", isbn: "9780553808049" })
    );
}

describe("AddIsbnComponent", () => {
  let component: AddIsbnComponent;
  let fixture: ComponentFixture<AddIsbnComponent>;
  let itemService: MockItemService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AddIsbnComponent],
      imports: [FormsModule, CommonModule],
      providers: [{ provide: ItemService, useClass: MockItemService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AddIsbnComponent);
    component = fixture.componentInstance;
    itemService = TestBed.inject(ItemService) as any;
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
    expect(itemService.fetchBoekByIsbn).not.toHaveBeenCalled();
    expect(component.book).toBeNull();
  });

  it("zoekBoek should fetch book and check library status", async () => {
    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(itemService.fetchBoekByIsbn).toHaveBeenCalledWith("9780553808049");
    expect(itemService.bestaatBoekInBibliotheek).toHaveBeenCalledWith("9780553808049");
    expect(component.book).toBeTruthy();
    expect(component.book.titel).toBe("Dune");
    expect(component.hasCheckedLibraryStatus).toBeTrue();
    expect(component.isLoading).toBeFalse();
  });

  it("zoekBoek should set isAlreadyInLibrary and successMessage when book is in library", async () => {
    itemService.bestaatBoekInBibliotheek.and.returnValue(Promise.resolve(true));
    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(component.isAlreadyInLibrary).toBeTrue();
    expect(component.successMessage).toBe("Reeds in de bibliotheek.");
    expect(component.hasCheckedLibraryStatus).toBeTrue();
  });

  it("zoekBoek should show 404 error message when book not found", async () => {
    itemService.fetchBoekByIsbn.and.returnValue(
      Promise.reject({ response: { status: 404 } })
    );
    component.isbn = "0000000000000";
    await component.zoekBoek();

    expect(component.errorMessage).toBe("Geen boek gevonden voor dit ISBN-nummer.");
    expect(component.book).toBeNull();
    expect(component.isLoading).toBeFalse();
  });

  it("zoekBoek should show generic error message on unexpected failure", async () => {
    itemService.fetchBoekByIsbn.and.returnValue(Promise.reject(new Error("Network error")));
    component.isbn = "9780553808049";
    await component.zoekBoek();

    expect(component.errorMessage).toBe("Er ging iets mis bij het ophalen van het boek.");
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

  it("voegToeAanBibliotheek should call importBoekByIsbn and set success state", async () => {
    component.book = { titel: "Dune", auteur: "Frank Herbert", isbn: "9780553808049" };
    component.hasCheckedLibraryStatus = true;
    component.isAlreadyInLibrary = false;

    await component.voegToeAanBibliotheek();

    expect(itemService.importBoekByIsbn).toHaveBeenCalledWith("9780553808049");
    expect(component.successMessage).toBe("Boek toegevoegd aan bibliotheek.");
    expect(component.isAlreadyInLibrary).toBeTrue();
    expect(component.hasCheckedLibraryStatus).toBeTrue();
    expect(component.isImporting).toBeFalse();
  });

  it("voegToeAanBibliotheek should not call service when book already in library", async () => {
    component.isAlreadyInLibrary = true;

    await component.voegToeAanBibliotheek();

    expect(itemService.importBoekByIsbn).not.toHaveBeenCalled();
    expect(component.successMessage).toBe("Reeds in de bibliotheek.");
  });

  it("voegToeAanBibliotheek should show generic error when service fails", async () => {
    component.book = { titel: "Dune", auteur: "Frank Herbert", isbn: "9780553808049" };
    component.hasCheckedLibraryStatus = true;
    component.isAlreadyInLibrary = false;
    itemService.importBoekByIsbn.and.returnValue(
      Promise.reject({ response: { status: 500 } })
    );

    await component.voegToeAanBibliotheek();

    expect(component.errorMessage).toBe(
      "Er ging iets mis bij het toevoegen aan de bibliotheek."
    );
    expect(component.isImporting).toBeFalse();
  });

  it("voegToeAanBibliotheek should show 404 error when not found during import", async () => {
    component.book = { titel: "Dune", auteur: "Frank Herbert", isbn: "9780553808049" };
    component.hasCheckedLibraryStatus = true;
    component.isAlreadyInLibrary = false;
    itemService.importBoekByIsbn.and.returnValue(
      Promise.reject({ response: { status: 404 } })
    );

    await component.voegToeAanBibliotheek();

    expect(component.errorMessage).toBe("Boek niet gevonden om te importeren.");
    expect(component.isImporting).toBeFalse();
  });
});
