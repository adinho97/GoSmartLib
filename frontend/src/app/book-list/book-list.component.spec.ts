import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";

import { BookListComponent } from "./book-list.component";
import { BookService } from "../services/book.service";

type TestBoek = {
  id?: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  genre: string;
  uitgaveDatum: string;
  paginas: number | null;
  taal: string;
  uitgeverij: string;
};

const createBoek = (
  id: number,
  titel: string,
  overrides: Partial<TestBoek> = {},
): TestBoek => ({
  id,
  titel,
  auteur: "Auteur",
  cover: "",
  beschrijving: "",
  genre: "",
  uitgaveDatum: "",
  paginas: null,
  taal: "",
  uitgeverij: "",
  ...overrides,
});

describe("BookListComponent", () => {
  let component: BookListComponent;
  let fixture: ComponentFixture<BookListComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;

  beforeEach(() => {
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBoeken",
      "deleteBoek",
    ]);
    bookServiceSpy.getBoeken.and.resolveTo([]);
    bookServiceSpy.deleteBoek.and.resolveTo();

    TestBed.configureTestingModule({
      declarations: [BookListComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: BookService, useValue: bookServiceSpy }],
    });

    fixture = TestBed.createComponent(BookListComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("loads and sorts books alphabetically on init", async () => {
    bookServiceSpy.getBoeken.and.resolveTo([
      createBoek(2, "Zebra"),
      createBoek(1, "Aap"),
    ]);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(bookServiceSpy.getBoeken).toHaveBeenCalled();
    expect(component.boeken.map((b) => b.titel)).toEqual(["Aap", "Zebra"]);
    expect(component.isLoading).toBeFalse();
    expect(component.currentPage).toBe(1);
  });

  it("sets error when loading books fails", async () => {
    bookServiceSpy.getBoeken.and.rejectWith(new Error("failed"));

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.error).toBe(
      "Boeken laden mislukt. Probeer later opnieuw.",
    );
    expect(component.isLoading).toBeFalse();
  });

  it("computes pagination values", () => {
    component.boeken = Array.from({ length: 40 }, (_, index) =>
      createBoek(index + 1, `Boek ${index + 1}`),
    );
    component.currentPage = 2;

    expect(component.totalPages).toBe(2);
    expect(component.pageNumbers).toEqual([1, 2]);
    expect(component.pagedBoeken.length).toBe(8);
    expect(component.pagedBoeken[0].id).toBe(33);
  });

  it("applies genre and taal filters only after applyFilters", () => {
    component.boeken = [
      createBoek(1, "Boek 1", { genre: "Fantasy", taal: "Nederlands" }),
      createBoek(2, "Boek 2", { genre: "Fantasy", taal: "Engels" }),
      createBoek(3, "Boek 3", { genre: "Sci-fi", taal: "Nederlands" }),
    ];

    component.selectedGenre = "Fantasy";
    component.selectedTaal = "Nederlands";

    expect(component.filteredBoeken.map((boek) => boek.id)).toEqual([1, 2, 3]);

    component.applyFilters();

    expect(component.filteredBoeken.map((boek) => boek.id)).toEqual([1]);
  });

  it("applies release date range filter inclusively", () => {
    component.boeken = [
      createBoek(1, "Boek 1", { uitgaveDatum: "2022-01-01" }),
      createBoek(2, "Boek 2", { uitgaveDatum: "2023-06-15" }),
      createBoek(3, "Boek 3", { uitgaveDatum: "2024-01-01" }),
    ];

    component.releaseDateFrom = "2023-01-01";
    component.releaseDateTo = "2023-12-31";
    component.applyFilters();

    expect(component.filteredBoeken.map((boek) => boek.id)).toEqual([2]);
  });

  it("applies min and max page filters", () => {
    component.boeken = [
      createBoek(1, "Boek 1", { paginas: 90 }),
      createBoek(2, "Boek 2", { paginas: 250 }),
      createBoek(3, "Boek 3", { paginas: 450 }),
    ];

    component.onMinPagesChange(100);
    component.onMaxPagesChange(300);
    component.applyFilters();

    expect(component.filteredBoeken.map((boek) => boek.id)).toEqual([2]);
  });

  it("keeps min and max pages consistent while changing sliders", () => {
    component.minPages = 100;
    component.maxPages = 300;

    component.onMinPagesChange(350);
    expect(component.minPages).toBe(350);
    expect(component.maxPages).toBe(350);

    component.onMaxPagesChange(200);
    expect(component.maxPages).toBe(200);
    expect(component.minPages).toBe(200);
  });

  it("clearFilters resets applied filter state and search", () => {
    component.boeken = [
      createBoek(1, "Fantasy Boek", { genre: "Fantasy", taal: "Nederlands" }),
      createBoek(2, "Sci-fi Boek", { genre: "Sci-fi", taal: "Engels" }),
    ];

    component.searchInput = "Fantasy";
    component.applySearch();
    component.selectedGenre = "Fantasy";
    component.selectedTaal = "Nederlands";
    component.releaseDateFrom = "2020-01-01";
    component.releaseDateTo = "2025-01-01";
    component.onMinPagesChange(100);
    component.onMaxPagesChange(200);
    component.applyFilters();

    component.clearFilters();

    expect(component.searchInput).toBe("");
    expect(component.searchQuery).toBe("");
    expect(component.appliedGenre).toBe("");
    expect(component.appliedTaal).toBe("");
    expect(component.appliedReleaseDateFrom).toBe("");
    expect(component.appliedReleaseDateTo).toBe("");
    expect(component.appliedMinPages).toBe(component.minPageFilterLimit);
    expect(component.appliedMaxPages).toBe(component.maxPageFilterLimit);
  });

  it("navigates to a valid page and scrolls to top", () => {
    spyOn(window, "scrollTo");
    component.boeken = Array.from({ length: 40 }, (_, index) =>
      createBoek(index + 1, `Boek ${index + 1}`),
    );

    component.gaNaarPagina(2);

    expect(component.currentPage).toBe(2);
    expect(window.scrollTo).toHaveBeenCalled();
  });

  it("ignores invalid page navigation", () => {
    spyOn(window, "scrollTo");
    component.boeken = Array.from({ length: 30 }, (_, index) =>
      createBoek(index + 1, `Boek ${index + 1}`),
    );
    component.currentPage = 1;

    component.gaNaarPagina(0);
    component.gaNaarPagina(99);

    expect(component.currentPage).toBe(1);
    expect(window.scrollTo).not.toHaveBeenCalled();
  });

  it("does not delete when user cancels confirmation", async () => {
    const stopPropagation = jasmine.createSpy("stopPropagation");
    const preventDefault = jasmine.createSpy("preventDefault");
    const event = {
      stopPropagation,
      preventDefault,
    } as unknown as MouseEvent;
    component.boeken = [createBoek(1, "Aap")];
    spyOn(window, "confirm").and.returnValue(false);

    await component.verwijderBoek(event, component.boeken[0]);

    expect(stopPropagation).toHaveBeenCalled();
    expect(preventDefault).toHaveBeenCalled();
    expect(bookServiceSpy.deleteBoek).not.toHaveBeenCalled();
    expect(component.boeken.length).toBe(1);
  });

  it("deletes a book after confirmation", async () => {
    const event = {
      stopPropagation: jasmine.createSpy("stopPropagation"),
      preventDefault: jasmine.createSpy("preventDefault"),
    } as unknown as MouseEvent;
    component.boeken = [createBoek(1, "Aap"), createBoek(2, "Beer")];
    spyOn(window, "confirm").and.returnValue(true);

    await component.verwijderBoek(event, component.boeken[0]);

    expect(bookServiceSpy.deleteBoek).toHaveBeenCalledWith(1);
    expect(component.boeken.map((b) => b.id)).toEqual([2]);
  });

  it("sets error when delete fails", async () => {
    const event = {
      stopPropagation: jasmine.createSpy("stopPropagation"),
      preventDefault: jasmine.createSpy("preventDefault"),
    } as unknown as MouseEvent;
    component.boeken = [createBoek(1, "Aap")];
    spyOn(window, "confirm").and.returnValue(true);
    bookServiceSpy.deleteBoek.and.rejectWith(new Error("failed"));

    await component.verwijderBoek(event, component.boeken[0]);

    expect(component.error).toBe("Verwijderen mislukt. Probeer later opnieuw.");
  });
});
