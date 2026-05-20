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
import { BadgeNotificationService } from "../services/badge-notification.service";
import { ExperienceService } from "../services/experience.service";
import { SchoolService } from "../services/school.service";
import { UiToastService } from "../services/ui-toast.service";

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
      "isHighlighted",
      "isClassReadingListItem",
      "getMyReviewCount",
      "getBookLestipDetails",
      "isWishlisted",
      "toggleHighlight",
      "toggleClassReadingListItem",
      "addToWishlist",
      "removeFromWishlist",
    ]);
    loanServiceSpy = jasmine.createSpyObj<LoanService>("LoanService", [
      "getMyActiveLoans", // Added missing mock
      "getMyLoanHistory", // Added missing mock
      "getCopySummary",
    ]);

    const badgeNotificationServiceSpy = jasmine.createSpyObj(
      "BadgeNotificationService",
      ["showBadgeNotification"],
    );
    const experienceServiceSpy = jasmine.createSpyObj("ExperienceService", [
      "addExperienceForReview",
      "removeExperienceForReview",
    ]);
    const schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    const uiToastServiceSpy = jasmine.createSpyObj("UiToastService", [
      "success",
      "error",
    ]);

    bookServiceSpy.isWishlisted.and.resolveTo(false);
    bookServiceSpy.getBookLestipDetails.and.resolveTo({
      lestip: "",
      auteurNaam: "",
      magVerwijderen: false,
    });
    bookServiceSpy.getMyReviewCount.and.resolveTo(0);
    bookServiceSpy.getBookById.and.returnValue(
      of({
        id: 1,
        titel: "Boek",
        auteur: "Auteur",
        isbn: "9780140328721",
        goNumber: "GO-12345678",
        cover: "test-cover.jpg",
        beschrijving: "",
        genre: "Algemeen",
        uitgaveDatum: "2020-01-01",
        paginas: 100,
        taal: "Nederlands",
        uitgeverij: "Uitgever",
      }),
    );
    bookServiceSpy.getBookReviews.and.resolveTo([]);
    bookServiceSpy.addBookReview.and.callFake((bookId, payload) =>
      Promise.resolve({
        id: 77,
        rating: payload.rating,
        comment: payload.comment,
        reviewerUserId: 1,
        anonymous: payload.anonymous,
        createdAt: "2026-03-18T10:00:00",
      }),
    );
    // The updateBookReview mock is fine as it is, as it's for a specific test case.
    bookServiceSpy.updateBookReview.and.resolveTo({
      id: 5,
      rating: 4,
      comment: "Bijgewerkt",
      reviewerUserId: 1,
      anonymous: false,
      createdAt: "2026-03-18T10:00:00",
    });
    bookServiceSpy.deleteBookReview.and.resolveTo();
    bookServiceSpy.isHighlighted.and.returnValue(Promise.resolve(false));
    bookServiceSpy.isClassReadingListItem.and.returnValue(
      Promise.resolve(false),
    );
    loanServiceSpy.getCopySummary.and.resolveTo({ total: 2, available: 1 });
    experienceServiceSpy.addExperienceForReview.and.stub(); // Stub to prevent errors during review submission
    experienceServiceSpy.removeExperienceForReview.and.stub(); // Stub to prevent errors during review deletion

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
        {
          provide: DomSanitizer,
          useValue: {
            bypassSecurityTrustResourceUrl: (url: string) => url, // Return the URL directly for testing
          },
        },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        {
          provide: BadgeNotificationService,
          useValue: badgeNotificationServiceSpy,
        },
        { provide: ExperienceService, useValue: experienceServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
      ],
    }).compileComponents();

    createComponent();
    fixture.detectChanges(); // Ensure ngOnInit is called
  });

  afterEach(() => {
    localStorage.clear();
    routerEvents$.complete();
  });
  it("should create", () => {
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

    // Re-trigger load to use new mock data
    await (component as any).loadReviews(1);
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
    await fixture.whenStable();

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

  it("shows GO-number only for librarians", async () => {
    localStorage.setItem("role", "bibbeheerder");
    createComponent();

    fixture.detectChanges();
    await fixture.whenStable();

    const infoText = fixture.nativeElement.textContent as string;
    expect(infoText).toContain("GO-nummer");
    expect(infoText).toContain("GO-12345678");
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
    expect(component.reviewError).toBe("Voeg een korte opmerking toe.");
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
      "Review verwijderen mislukt. Probeer het opnieuw.",
    );
    expect(component.deleteReviewDialogOpen).toBeFalse();
    expect(component.pendingDeleteReviewId).toBeNull();
  });

  it("openPreview uses archive embed url from books api when available", async () => {
    const sanitizer = TestBed.inject(DomSanitizer);
    spyOn(sanitizer, "bypassSecurityTrustResourceUrl").and.callThrough();

    const getSpy = spyOn(axios, "get").and.callFake((async (
      url: string,
      config?: any,
    ) => {
      if (url.includes("jscmd=viewapi")) {
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
    }) as any);

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
    await fixture.whenStable();

    expect(getSpy).toHaveBeenCalled();
    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe("https://archive.org/embed/some-book-id");
    expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(
      "https://archive.org/embed/some-book-id",
    );
  });

  it("openPreview resolves openlibrary edition to archive embed", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (url.includes("jscmd=viewapi")) {
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
    }) as any);

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
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe(
      "https://archive.org/embed/archive-edition-id",
    );
  });

  it("openPreview falls back to search ia embed when isbn lookup has no preview", async () => {
    const getSpy = spyOn(axios, "get").and.callFake((async (
      url: string,
      config?: any,
    ) => {
      if (url.includes("jscmd=viewapi") && !url.includes("OLID")) {
        return {
          data: {},
        };
      }

      if (url.includes("search.json")) {
        return {
          data: {
            docs: [{ ia: ["search-hit-ia"], isbn: ["9780140328721"] }],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as any);

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
    await fixture.whenStable();

    expect(getSpy).toHaveBeenCalled();
    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe(
      "https://archive.org/embed/search-hit-ia",
    );
  });

  it("openPreview rejects isbn-mismatched search hits and shows alert", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (url.includes("jscmd=viewapi")) {
        return {
          data: {},
        };
      }

      if (url.includes("search.json")) {
        return {
          data: {
            docs: [{ ia: ["wrong-book-id"], isbn: ["9780000000000"] }],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as any);

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
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeFalse();
    expect(component.previewAlertOpen).toBeTrue();
    expect(component.previewAlertTitle).toBe("Geen voorbeeld beschikbaar");
  });

  it("openPreview ignores wrong title/author matches and returns no preview", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (url.includes("search.json")) {
        return {
          data: {
            docs: [
              {
                ia: ["wrong-book-id"],
                title: "Completely Different Book",
                author_name: ["Another Author"],
              },
            ],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as any);

    component.book = {
      id: 1,
      titel: "Boek",
      auteur: "Auteur",
      isbn: "",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeFalse();
    expect(component.previewAlertOpen).toBeTrue();
    expect(component.previewAlertTitle).toBe("Geen voorbeeld beschikbaar");
  });

  it("openPreview skips wrong search doc and uses later matching title/author doc", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (
        url.includes("search.json") ||
        url.includes("proxy/openlibrary/search")
      ) {
        return {
          data: {
            docs: [
              {
                ia: ["wrong-book-id"],
                title: "Completely Different Book",
                author_name: ["Another Author"],
              },
              {
                ia: ["correct-book-id"],
                title: "Boek",
                author_name: ["Auteur"],
              },
            ],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as any);

    component.book = {
      id: 1,
      titel: "Boek",
      auteur: "Auteur",
      isbn: "",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeTrue();
    expect(component.previewUrl).toBe(
      "https://archive.org/embed/correct-book-id",
    );
  });

  it("openPreview rejects search doc when only title matches", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (url.includes("search.json")) {
        return {
          data: {
            docs: [
              {
                ia: ["title-only-id"],
                title: "Test Boek",
                author_name: ["Totaal Andere Schrijver"],
              },
            ],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as any);

    component.book = {
      id: 1,
      titel: "Test Boek",
      auteur: "Originele Auteur",
      isbn: "",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeFalse();
    expect(component.previewAlertOpen).toBeTrue();
    expect(component.previewAlertTitle).toBe("Geen voorbeeld beschikbaar");
  });

  it("openPreview rejects search doc when only author matches", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (url.includes("search.json")) {
        return {
          data: {
            docs: [
              {
                ia: ["author-only-id"],
                title: "Totaal Andere Titel",
                author_name: ["Schrijver"],
              },
            ],
          },
        };
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as any);

    component.book = {
      id: 1,
      titel: "Originele Titel",
      auteur: "Schrijver",
      isbn: "",
      cover: "",
      beschrijving: "",
      genre: "Algemeen",
      uitgaveDatum: "2020-01-01",
      paginas: 100,
      taal: "Nederlands",
      uitgeverij: "Uitgever",
    };

    await component.openPreview();
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeFalse();
    expect(component.previewAlertOpen).toBeTrue();
    expect(component.previewAlertTitle).toBe("Geen voorbeeld beschikbaar");
  });

  it("openPreview shows preview alert when no readable preview exists", async () => {
    spyOn(axios, "get").and.callFake((async (url: string, config?: any) => {
      if (url.includes("jscmd=viewapi")) {
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
    }) as any);

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
    await fixture.whenStable();

    expect(component.previewModalOpen).toBeFalse();
    expect(component.previewAlertOpen).toBeTrue();
    expect(component.previewAlertTitle).toBe("Geen voorbeeld beschikbaar");
  });
});
