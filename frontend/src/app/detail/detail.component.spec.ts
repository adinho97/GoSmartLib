import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute, convertToParamMap } from "@angular/router";
import { of } from "rxjs";
import axios from "axios";

import { DetailComponent } from "./detail.component";
import { BookService } from "../services/book.service";
import { Location } from "@angular/common";

describe("DetailComponent", () => {
  let component: DetailComponent;
  let fixture: ComponentFixture<DetailComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let locationSpy: jasmine.SpyObj<Location>;

  afterEach(() => {
    localStorage.removeItem("role");
  });

  beforeEach(() => {
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBookById",
      "getBookReviews",
      "addBookReview",
      "deleteBookReview",
    ]);
    bookServiceSpy.getBookById.and.returnValue(
      of({
        id: 1,
        titel: "Boek",
        auteur: "Auteur",
        cover: "",
        beschrijving: "",
        genre: "Algemeen",
        uitgaveDatum: "2020-01-01",
        paginas: 100,
        taal: "Nederlands",
        uitgeverij: "Uitgever",
      }),
    );
    bookServiceSpy.getBookReviews.and.resolveTo([]);
    bookServiceSpy.addBookReview.and.resolveTo({
      id: 77,
      rating: 5,
      comment: "Sterk boek",
      createdAt: "2026-03-18T10:00:00",
    });
    bookServiceSpy.deleteBookReview.and.resolveTo();

    locationSpy = jasmine.createSpyObj<Location>("Location", ["back"]);

    TestBed.configureTestingModule({
      declarations: [DetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: "1" }),
            },
          },
        },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: Location, useValue: locationSpy },
      ],
    });
    fixture = TestBed.createComponent(DetailComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("loads book and reviews on init", async () => {
    bookServiceSpy.getBookReviews.and.resolveTo([
      {
        id: 10,
        rating: 4,
        comment: "Goed",
        createdAt: "2026-03-18T09:00:00",
      },
    ]);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(bookServiceSpy.getBookById).toHaveBeenCalledWith(1);
    expect(bookServiceSpy.getBookReviews).toHaveBeenCalledWith(1);
    expect(component.reviews.length).toBe(1);
    expect(component.reviews[0].comment).toBe("Goed");
  });

  it("setReviewRating stores rating and clears existing review error", () => {
    component.reviewError = "oude fout";

    component.setReviewRating(3);

    expect(component.newReviewRating).toBe(3);
    expect(component.reviewError).toBe("");
  });

  it("submitReview validates rating before calling api", async () => {
    component.currentBookId = 1;
    component.newReviewRating = 0;
    component.newReviewComment = "ok";

    await component.submitReview();

    expect(component.reviewError).toBe("Kies een score van 1 tot 5 sterren.");
    expect(bookServiceSpy.addBookReview).not.toHaveBeenCalled();
  });

  it("submitReview validates non-empty trimmed comment", async () => {
    component.currentBookId = 1;
    component.newReviewRating = 4;
    component.newReviewComment = "   ";

    await component.submitReview();

    expect(component.reviewError).toBe("Voeg een korte comment toe.");
    expect(bookServiceSpy.addBookReview).not.toHaveBeenCalled();
  });

  it("submitReview appends review and resets form on success", async () => {
    component.currentBookId = 1;
    component.reviews = [
      {
        id: 1,
        rating: 3,
        comment: "Bestaande review",
        createdAt: "2026-03-18T08:00:00",
      },
    ];
    component.newReviewRating = 5;
    component.newReviewComment = "  Nieuwe review  ";

    await component.submitReview();

    expect(bookServiceSpy.addBookReview).toHaveBeenCalledWith(1, {
      rating: 5,
      comment: "Nieuwe review",
    });
    expect(component.reviews[0].id).toBe(77);
    expect(component.reviews.length).toBe(2);
    expect(component.newReviewRating).toBe(0);
    expect(component.newReviewComment).toBe("");
    expect(component.reviewError).toBe("");
  });

  it("submitReview surfaces api error message when available", async () => {
    component.currentBookId = 1;
    component.newReviewRating = 4;
    component.newReviewComment = "Inhoud";

    const apiError = { response: { data: { message: "Te veel reviews" } } };
    bookServiceSpy.addBookReview.and.rejectWith(apiError);
    spyOn(axios, "isAxiosError").and.returnValue(true);

    await component.submitReview();

    expect(component.reviewError).toBe("Te veel reviews");
  });

  it("submitReview shows fallback error for unknown failures", async () => {
    component.currentBookId = 1;
    component.newReviewRating = 4;
    component.newReviewComment = "Inhoud";
    bookServiceSpy.addBookReview.and.rejectWith(new Error("kapot"));
    spyOn(axios, "isAxiosError").and.returnValue(false);

    await component.submitReview();

    expect(component.reviewError).toBe(
      "Review opslaan mislukt. Probeer opnieuw.",
    );
  });

  it("deleteReview does nothing for non-librarian users", async () => {
    component.currentBookId = 1;
    component.reviews = [
      { id: 3, rating: 2, comment: "x", createdAt: "2026-03-18T07:00:00" },
    ];

    await component.deleteReview(3);

    expect(bookServiceSpy.deleteBookReview).not.toHaveBeenCalled();
    expect(component.reviews.length).toBe(1);
  });

  it("deleteReview removes review after confirmation for librarians", async () => {
    localStorage.setItem("role", "bibbeheerder");
    fixture = TestBed.createComponent(DetailComponent);
    component = fixture.componentInstance;

    component.currentBookId = 1;
    component.reviews = [
      { id: 5, rating: 3, comment: "a", createdAt: "2026-03-18T06:00:00" },
      { id: 6, rating: 4, comment: "b", createdAt: "2026-03-18T05:00:00" },
    ];
    spyOn(window, "confirm").and.returnValue(true);

    await component.deleteReview(5);

    expect(bookServiceSpy.deleteBookReview).toHaveBeenCalledWith(1, 5);
    expect(component.reviews.map((review) => review.id)).toEqual([6]);
  });

  it("deleteReview keeps list and sets error when api call fails", async () => {
    localStorage.setItem("role", "bibbeheerder");
    fixture = TestBed.createComponent(DetailComponent);
    component = fixture.componentInstance;

    component.currentBookId = 1;
    component.reviews = [
      { id: 5, rating: 3, comment: "a", createdAt: "2026-03-18T06:00:00" },
    ];
    spyOn(window, "confirm").and.returnValue(true);
    bookServiceSpy.deleteBookReview.and.rejectWith(new Error("mislukt"));

    await component.deleteReview(5);

    expect(component.reviews.length).toBe(1);
    expect(component.reviewError).toBe(
      "Review verwijderen mislukt. Probeer opnieuw.",
    );
  });
});
