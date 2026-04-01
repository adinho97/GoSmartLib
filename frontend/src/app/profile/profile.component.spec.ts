import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ProfileComponent } from "./profile.component";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { Location } from "@angular/common";
import { By } from "@angular/platform-browser";
import { BookService } from "../services/book.service";

describe("ProfileComponent", () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;

  let routerSpy = jasmine.createSpyObj("Router", ["navigate"]);
  let locationSpy = jasmine.createSpyObj("Location", ["back"]);

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getUserFavorites",
      "removeFromFavorites",
    ]);

    await TestBed.configureTestingModule({
      declarations: [ProfileComponent],
      imports: [FormsModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy },
        { provide: BookService, useValue: bookServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  describe("Favorites functionality", () => {
    it("should load favorite books on init", async () => {
      const mockFavorites = [
        {
          id: 1,
          bookId: 123,
          titel: "Test Book",
          auteur: "Test Author",
          cover: "test.jpg",
          addedAt: "2023-01-01",
        },
      ];
      bookServiceSpy.getUserFavorites.and.resolveTo(mockFavorites);

      component.ngOnInit();

      await fixture.whenStable();

      expect(bookServiceSpy.getUserFavorites).toHaveBeenCalled();
      expect(component.favoriteBooks).toEqual([
        {
          id: 123,
          title: "Test Book",
          author: "Test Author",
          cover: "test.jpg",
        },
      ]);
    });

    it("should handle error when loading favorites", async () => {
      bookServiceSpy.getUserFavorites.and.rejectWith(new Error("Test error"));

      component.ngOnInit();

      await fixture.whenStable();

      expect(component.favoriteBooks).toEqual([]);
    });

    it("should remove from favorites", async () => {
      component.favoriteBooks = [
        { id: 123, title: "Test Book", author: "Test Author", cover: "" },
      ];
      bookServiceSpy.removeFromFavorites.and.resolveTo();

      const event = new MouseEvent("click");
      spyOn(event, "stopPropagation");
      spyOn(event, "preventDefault");

      await component.removeFromFavorites(event, 123);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(event.preventDefault).toHaveBeenCalled();
      expect(bookServiceSpy.removeFromFavorites).toHaveBeenCalledWith(123);
      expect(component.favoriteBooks).toEqual([]);
    });

    it("should handle error when removing from favorites", async () => {
      component.favoriteBooks = [
        { id: 123, title: "Test Book", author: "Test Author", cover: "" },
      ];
      bookServiceSpy.removeFromFavorites.and.rejectWith(
        new Error("Test error"),
      );

      const event = new MouseEvent("click");

      await expectAsync(
        component.removeFromFavorites(event, 123),
      ).toBeResolved();
      expect(component.favoriteBooks).toEqual([
        { id: 123, title: "Test Book", author: "Test Author", cover: "" },
      ]); // not removed due to error
    });
  });

  it("should toggle settings menu", () => {
    expect(component.settingsOpen).toBeFalse();

    component.toggleSettings();
    expect(component.settingsOpen).toBeTrue();

    component.toggleSettings();
    expect(component.settingsOpen).toBeFalse();
  });

  it("should call location.back when goBack is clicked", () => {
    component.goBack();
    expect(locationSpy.back).toHaveBeenCalled();
  });

  it("should navigate to book detail", () => {
    component.goToDetail(5);
    expect(routerSpy.navigate).toHaveBeenCalledWith(["/detail", 5]);
  });

  it("should save dashboard settings to localStorage", () => {
    spyOn(localStorage, "setItem");

    component.saveDashboardSettings();

    expect(localStorage.setItem).toHaveBeenCalledWith(
      "dashboardSettings",
      JSON.stringify(component.dashboardSettings),
    );
    expect(component.settingsOpen).toBeFalse();
  });

  it("should load dashboard settings from localStorage on init", () => {
    const mockSettings = {
      showFavorites: false,
      showReadingHistory: false,
      showBorrowed: false,
      showHighlighted: false,
      showDeadline: false,
    };

    spyOn(localStorage, "getItem").and.returnValue(
      JSON.stringify(mockSettings),
    );

    component.ngOnInit();

    expect(component.dashboardSettings.showFavorites).toBeFalse();
  });

  it("should close settings when clicking outside", () => {
    component.settingsOpen = true;

    const event = new MouseEvent("click");
    spyOn(event, "target").and.returnValue(document.createElement("div"));

    component.clickOutside(event);

    expect(component.settingsOpen).toBeFalse();
  });

  it("should render borrowed books", () => {
    fixture.detectChanges();

    const books = fixture.debugElement.queryAll(By.css(".profile-book-card"));
    expect(books.length).toBeGreaterThan(0);
  });
});
