import { ComponentFixture, TestBed } from "@angular/core/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { Router } from "@angular/router";

import { DashboardComponent } from "./dashboard.component";
import { BookService } from "../services/book.service";

describe("DashboardComponent", () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBooks",
      "addToFavorites",
      "removeFromFavorites",
    ]);
    bookServiceSpy.getBooks.and.resolveTo([]);
    routerSpy = jasmine.createSpyObj<Router>("Router", ["navigate"]);

    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  describe("toggleFavorite", () => {
    it("should add to favorites if not favorited", async () => {
      const event = new MouseEvent("click");
      spyOn(event, "stopPropagation");
      spyOn(event, "preventDefault");

      component.favoritedBookIds.clear(); // not favorited

      await component.toggleFavorite(event, 123);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(event.preventDefault).toHaveBeenCalled();
      expect(bookServiceSpy.addToFavorites).toHaveBeenCalledWith(123);
      expect(component.favoritedBookIds.has(123)).toBe(true);
    });

    it("should remove from favorites if already favorited", async () => {
      const event = new MouseEvent("click");
      spyOn(event, "stopPropagation");
      spyOn(event, "preventDefault");

      component.favoritedBookIds.add(123); // already favorited

      await component.toggleFavorite(event, 123);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(event.preventDefault).toHaveBeenCalled();
      expect(bookServiceSpy.removeFromFavorites).toHaveBeenCalledWith(123);
      expect(component.favoritedBookIds.has(123)).toBe(false);
    });

    it("should handle errors silently", async () => {
      bookServiceSpy.addToFavorites.and.rejectWith(new Error("Test error"));

      const event = new MouseEvent("click");
      spyOn(event, "stopPropagation");
      spyOn(event, "preventDefault");

      component.favoritedBookIds.clear();

      await expectAsync(component.toggleFavorite(event, 123)).toBeResolved();
      expect(component.favoritedBookIds.has(123)).toBe(false); // not added due to error
    });
  });
});
