import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  convertToParamMap,
} from "@angular/router";
import { DomSanitizer } from "@angular/platform-browser";
import { of, Subject } from "rxjs";
import axios from "axios";

import { DetailComponent } from "./detail.component";
import { BookService } from "../services/book.service";
import { LoanService } from "../services/loan.service";

describe("DetailComponent", () => {
  let component: DetailComponent;
  let fixture: ComponentFixture<DetailComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;
  let routerEvents$: Subject<NavigationEnd>;

  function createComponent(): void {
    fixture = TestBed.createComponent(DetailComponent);
    component = fixture.componentInstance;
  }

  beforeEach(async () => {
    localStorage.clear();

    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBookById",
      "getBookReviews",
      "addBookReview",
      "updateBookReview",
      "deleteBookReview",
    ]);
    loanServiceSpy = jasmine.createSpyObj<LoanService>("LoanService", [
      "getCopySummary",
    ]);

    bookServiceSpy.getBookById.and.returnValue(
      of({
        id: 1,
        titel: "Boek",
        auteur: "Auteur",
        isbn: "9780140328721",
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
      reviewerUserId: 1,
      anonymous: true,
      createdAt: "2026-03-18T10:00:00",
    });
    bookServiceSpy.updateBookReview.and.resolveTo({
      id: 5,
      rating: 4,
      comment: "Bijgewerkt",
      reviewerUserId: 1,
      anonymous: false,
      createdAt: "2026-03-18T10:00:00",
    });
    bookServiceSpy.deleteBookReview.and.resolveTo();
    loanServiceSpy.getCopySummary.and.resolveTo({ total: 2, available: 1 });

    routerEvents$ = new Subject<NavigationEnd>();

    await TestBed.configureTestingModule({
      imports: [FormsModule],
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
        {
          provide: Router,
          useValue: {
            events: routerEvents$.asObservable(),
            navigate: jasmine.createSpy("navigate"),
          },
        },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
      ],
    }).compileComponents();

    createComponent();
  });

  afterEach(() => {
    localStorage.clear();
    routerEvents$.complete();
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
        reviewerUserId: 2,
        anonymous: false,
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

  it("submitReview appends review and resets form on success", async () => {
    component.currentBookId = 1;
    component.newReviewAnonymous = true;
    component.reviews = [
      {
        id: 1,
        rating: 3,
        comment: "Bestaande review",
        reviewerUserId: 1,
        anonymous: false,
        createdAt: "2026-03-18T08:00:00",
      },
    ];
    component.newReviewRating = 5;
    component.newReviewComment = "  Nieuwe review  ";

    await component.submitReview();

    expect(bookServiceSpy.addBookReview).toHaveBeenCalledWith(1, {
      rating: 5,
      comment: "Nieuwe review",
      anonymous: true,
    });
    expect(component.reviews[0].id).toBe(77);
    expect(component.reviews.length).toBe(2);
    expect(component.newReviewRating).toBe(0);
    expect(component.newReviewComment).toBe("");
    expect(component.reviewError).toBe("");
  });

  it("opens delete dialog and deletes review after confirmation", async () => {
    localStorage.setItem("role", "bibbeheerder");
    createComponent();
    component.currentBookId = 1;
    component.reviews = [
      {
        id: 5,
        rating: 3,
        comment: "a",
        reviewerUserId: 2,
        anonymous: false,
        createdAt: "2026-03-18T06:00:00",
      },
    ];

    component.deleteReview(5);
    expect(component.deleteReviewDialogOpen).toBeTrue();
    expect(component.pendingDeleteReviewId).toBe(5);

    await component.confirmDeleteReview();

    expect(bookServiceSpy.deleteBookReview).toHaveBeenCalledWith(1, 5);
    expect(component.reviews.length).toBe(0);
    expect(component.deleteReviewDialogOpen).toBeFalse();
  });

  it("startReviewEdit fills edit form when review is manageable", () => {
    const review = {
      id: 8,
      rating: 3,
      comment: "Origineel",
      reviewerUserId: 1,
      anonymous: false,
      canManage: true,
      createdAt: "2026-03-18T06:00:00",
    };

    component.startReviewEdit(review);

    expect(component.editReviewId).toBe(8);
    expect(component.editReviewRating).toBe(3);
    expect(component.editReviewComment).toBe("Origineel");
  });

  it("saveReviewEdit validates rating", async () => {
    component.currentBookId = 1;
    component.editReviewId = 5;
    component.editReviewRating = 0;
    component.editReviewComment = "ok";

    await component.saveReviewEdit(5);

    expect(bookServiceSpy.updateBookReview).not.toHaveBeenCalled();
    expect(component.reviewError).toBe("Kies een score van 1 tot 5 sterren.");
  });

  it("saveReviewEdit validates non-empty trimmed comment", async () => {
    component.currentBookId = 1;
    component.editReviewId = 5;
    component.editReviewRating = 4;
    component.editReviewComment = "   ";

    await component.saveReviewEdit(5);

    expect(bookServiceSpy.updateBookReview).not.toHaveBeenCalled();
    expect(component.reviewError).toBe("Voeg een korte comment toe.");
  });

  it("saveReviewEdit updates review and exits edit mode", async () => {
    component.currentBookId = 1;
    component.reviews = [
      {
        id: 5,
        rating: 2,
        comment: "Oud",
        reviewerUserId: 1,
        anonymous: false,
        canManage: true,
        createdAt: "2026-03-18T06:00:00",
      },
    ];
    component.editReviewId = 5;
    component.editReviewRating = 4;
    component.editReviewComment = "  Bijgewerkt  ";

    await component.saveReviewEdit(5);

    expect(bookServiceSpy.updateBookReview).toHaveBeenCalledWith(1, 5, {
      rating: 4,
      comment: "Bijgewerkt",
    });
    expect(component.reviews[0].comment).toBe("Bijgewerkt");
    expect(component.editReviewId).toBeNull();
    expect(component.reviewSuccess).toBe("Review bijgewerkt.");
  });

  it("saveReviewEdit surfaces api validation message", async () => {
    component.currentBookId = 1;
    component.editReviewId = 5;
    component.editReviewRating = 4;
    component.editReviewComment = "Inhoud";
    const apiError = { response: { data: { message: "Niet toegestaan" } } };
    bookServiceSpy.updateBookReview.and.rejectWith(apiError);
    spyOn(axios, "isAxiosError").and.returnValue(true);

    await component.saveReviewEdit(5);

    expect(component.reviewError).toBe("Niet toegestaan");
    expect(component.reviewSuccess).toBe("");
  });

  it("cancelDeleteReviewDialog closes dialog and clears pending id", () => {
    component.deleteReviewDialogOpen = true;
    component.pendingDeleteReviewId = 11;

    component.cancelDeleteReviewDialog();

    expect(component.deleteReviewDialogOpen).toBeFalse();
    expect(component.pendingDeleteReviewId).toBeNull();
  });

  it("confirmDeleteReview keeps review and sets error when delete fails", async () => {
    localStorage.setItem("role", "bibbeheerder");
    createComponent();
    component.currentBookId = 1;
    component.reviews = [
      {
        id: 5,
        rating: 3,
        comment: "a",
        reviewerUserId: 2,
        anonymous: false,
        createdAt: "2026-03-18T06:00:00",
      },
    ];
    component.pendingDeleteReviewId = 5;
    component.deleteReviewDialogOpen = true;
    bookServiceSpy.deleteBookReview.and.rejectWith(new Error("mislukt"));

    await component.confirmDeleteReview();

    expect(component.reviews.length).toBe(1);
    expect(component.reviewError).toBe(
      "Review verwijderen mislukt. Probeer opnieuw.",
    );
    expect(component.deleteReviewDialogOpen).toBeFalse();
    expect(component.pendingDeleteReviewId).toBeNull();
  });

  it("openPreview uses archive embed url from books api when available", async () => {
    const sanitizer = TestBed.inject(DomSanitizer);
    spyOn(sanitizer, "bypassSecurityTrustResourceUrl").and.callThrough();

    const getSpy = spyOn(axios, "get").and.callFake(async (url: string) => {
      if (url.includes("api/books")) {
        return {
          data: {
            "ISBN:9780140328721": {
              preview: "full",
              preview_url: "https://archive.org/details/some-book-id",
            },
          },
        };
      }
      throw new Error(`Unexpected URL: ${url}`);
    });

    component.book = {
      id: 1,
      titel: "Boek",
      auteur: "Auteur",
      isbn: "9780140328721",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();

    expect(getSpy).toHaveBeenCalled();
    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe("https://archive.org/embed/some-book-id");
    expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(
      "https://archive.org/embed/some-book-id",
    );
  });

  it("openPreview resolves openlibrary edition to archive embed", async () => {
    spyOn(axios, "get").and.callFake(async (url: string) => {
      if (url.includes("api/books")) {
        return {
          data: {
            "ISBN:9780140328721": {
              preview: "full",
              preview_url: "https://openlibrary.org/books/OL123M/Test",
            },
          },
        };
      }

      if (url.includes("/books/OL123M.json")) {
        return {
          data: {
            ocaid: "archive-edition-id",
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    });

    component.book = {
      id: 1,
      titel: "Boek",
      auteur: "Auteur",
      isbn: "9780140328721",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();

    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe(
      "https://archive.org/embed/archive-edition-id",
    );
  });

  it("openPreview falls back to search ia embed when isbn lookup has no preview", async () => {
    const getSpy = spyOn(axios, "get").and.callFake(async (url: string) => {
      if (url.includes("api/books")) {
        return {
          data: {},
        };
      }

      if (url.includes("search.json")) {
        return {
          data: {
            docs: [{ ia: ["search-hit-ia"] }],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    });

    component.book = {
      id: 1,
      titel: "Boek",
      auteur: "Auteur",
      isbn: "9780140328721",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();

    expect(getSpy).toHaveBeenCalled();
    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe(
      "https://archive.org/embed/search-hit-ia",
    );
  });

  it("openPreview shows preview alert when no readable preview exists", async () => {
    spyOn(axios, "get").and.callFake(async (url: string) => {
      if (url.includes("api/books")) {
        return {
          data: {
            "ISBN:9780140328721": {
              preview: "noview",
              preview_url: "https://openlibrary.org/books/OL111M/Test",
            },
          },
        };
      }

      if (url.includes("search.json")) {
        return {
          data: {
            docs: [],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    });

    component.book = {
      id: 1,
      titel: "Boek",
      auteur: "Auteur",
      isbn: "9780140328721",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();

    expect(component.previewModalOpen).toBeFalse();
    expect(component.previewAlertOpen).toBeTrue();
    expect(component.previewAlertTitle).toBe("Geen voorbeeld beschikbaar");
  });
});
