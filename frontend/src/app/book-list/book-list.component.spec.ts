import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";

import { BookListComponent } from "./book-list.component";
import { ItemService } from "../item.service";

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

const createBoek = (id: number, titel: string): TestBoek => ({
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
});

describe("BookListComponent", () => {
  let component: BookListComponent;
  let fixture: ComponentFixture<BookListComponent>;
  let itemServiceSpy: jasmine.SpyObj<ItemService>;

  beforeEach(() => {
    itemServiceSpy = jasmine.createSpyObj<ItemService>("ItemService", [
      "getBoeken",
      "deleteBoek",
    ]);
    itemServiceSpy.getBoeken.and.resolveTo([]);
    itemServiceSpy.deleteBoek.and.resolveTo();

    TestBed.configureTestingModule({
      declarations: [BookListComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: ItemService, useValue: itemServiceSpy }],
    });

    fixture = TestBed.createComponent(BookListComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("loads and sorts books alphabetically on init", async () => {
    itemServiceSpy.getBoeken.and.resolveTo([
      createBoek(2, "Zebra"),
      createBoek(1, "Aap"),
    ]);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(itemServiceSpy.getBoeken).toHaveBeenCalled();
    expect(component.boeken.map((b) => b.titel)).toEqual(["Aap", "Zebra"]);
    expect(component.isLoading).toBeFalse();
    expect(component.currentPage).toBe(1);
  });

  it("sets error when loading books fails", async () => {
    itemServiceSpy.getBoeken.and.rejectWith(new Error("failed"));

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.error).toBe(
      "Boeken laden mislukt. Probeer later opnieuw.",
    );
    expect(component.isLoading).toBeFalse();
  });

  it("computes pagination values", () => {
    component.boeken = Array.from({ length: 30 }, (_, index) =>
      createBoek(index + 1, `Boek ${index + 1}`),
    );
    component.currentPage = 2;

    expect(component.totalPages).toBe(2);
    expect(component.pageNumbers).toEqual([1, 2]);
    expect(component.pagedBoeken.length).toBe(5);
    expect(component.pagedBoeken[0].id).toBe(26);
  });

  it("navigates to a valid page and scrolls to top", () => {
    spyOn(window, "scrollTo");
    component.boeken = Array.from({ length: 30 }, (_, index) =>
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
    expect(itemServiceSpy.deleteBoek).not.toHaveBeenCalled();
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

    expect(itemServiceSpy.deleteBoek).toHaveBeenCalledWith(1);
    expect(component.boeken.map((b) => b.id)).toEqual([2]);
  });

  it("sets error when delete fails", async () => {
    const event = {
      stopPropagation: jasmine.createSpy("stopPropagation"),
      preventDefault: jasmine.createSpy("preventDefault"),
    } as unknown as MouseEvent;
    component.boeken = [createBoek(1, "Aap")];
    spyOn(window, "confirm").and.returnValue(true);
    itemServiceSpy.deleteBoek.and.rejectWith(new Error("failed"));

    await component.verwijderBoek(event, component.boeken[0]);

    expect(component.error).toBe("Verwijderen mislukt. Probeer later opnieuw.");
  });
});
