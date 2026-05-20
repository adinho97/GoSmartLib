import { TestBed } from "@angular/core/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import axios from "axios";

import { BookService } from "./book.service";
import { AuthContextService } from "./auth-context.service";

describe("BookService", () => {
  let service: BookService;
  let authContextSpy: jasmine.SpyObj<AuthContextService>;

  beforeEach(() => {
    authContextSpy = jasmine.createSpyObj<AuthContextService>(
      "AuthContextService",
      [
        "getEffectiveRole",
        "getEffectiveSub",
        "getEffectiveBearerToken",
        "isAdminMode",
      ],
    );
    authContextSpy.getEffectiveRole.and.returnValue("leerkracht");
    authContextSpy.getEffectiveSub.and.returnValue("test-sub-123");
    authContextSpy.getEffectiveBearerToken.and.returnValue("test-token");
    authContextSpy.isAdminMode.and.returnValue(false);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: AuthContextService, useValue: authContextSpy }],
    });
    service = TestBed.inject(BookService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("GO-ID functionality", () => {
    it("should fetch a book by GO-id", async () => {
      const mockBook = { id: 7, titel: "GO Boek", goNumber: "GO-12345678" };
      spyOn(axios, "get").and.resolveTo({ data: mockBook });

      const result = await service.fetchBookByGoNumber("GO-12345678");

      expect(axios.get).toHaveBeenCalledWith("/api/boeken/go/GO-12345678");
      expect(result).toEqual(mockBook);
    });

    it("should get book by GO-id from library with school scope", async () => {
      const mockBook = { id: 9, titel: "Schoolboek", goNumber: "GO-87654321" };
      spyOn(axios, "get").and.resolveTo({ data: mockBook });

      const result = await service.getBookByGoNumberFromLibrary(
        "GO-87654321",
        3,
      );

      expect(axios.get).toHaveBeenCalledWith(
        "/api/boeken/go/GO-87654321?schoolId=3",
      );
      expect(result).toEqual(mockBook as any);
    });

    it("should return null for GO-id library lookup on 404", async () => {
      spyOn(axios, "get").and.rejectWith({ response: { status: 404 } });

      const result = await service.getBookByGoNumberFromLibrary(
        "GO-00000000",
        1,
      );

      expect(result).toBeNull();
    });

    it("should rethrow GO-id library lookup errors except 404", async () => {
      spyOn(axios, "get").and.rejectWith({ response: { status: 500 } });

      await expectAsync(
        service.getBookByGoNumberFromLibrary("GO-00000000", 1),
      ).toBeRejected();
    });

    it("should create a book with full auth headers", async () => {
      const axiosPostSpy = spyOn(axios, "post").and.resolveTo({
        data: { id: 123, titel: "Nieuw boek" },
      } as any);

      const newBook = {
        titel: "Nieuw boek",
        auteur: "Auteur",
        cover: "cover.jpg",
        beschrijving: "Beschrijving",
        genre: "Fictie",
        uitgaveDatum: "2026-05-20",
        paginas: 120,
        taal: "NL",
        uitgeverij: "Uitgever",
      } as any;

      const result = await service.addBook(newBook);

      expect(axiosPostSpy).toHaveBeenCalledWith(
        "/api/boeken",
        jasmine.objectContaining({ titel: "Nieuw boek" }),
        {
          headers: {
            "X-User-Role": "leerkracht",
            "X-User-Sub": "test-sub-123",
            "X-User-Name": "Gebruiker",
            Authorization: "Bearer test-token",
          },
        },
      );
      expect(result).toEqual({ id: 123, titel: "Nieuw boek" });
    });

    it("should fetch book reviews with full auth headers", async () => {
      const axiosGetSpy = spyOn(axios, "get").and.resolveTo({
        data: [{ id: 1, rating: 5, comment: "Goed" }],
      } as any);

      const result = await service.getBookReviews(42);

      expect(axiosGetSpy).toHaveBeenCalledWith("/api/boeken/42/reviews", {
        headers: {
          "X-User-Role": "leerkracht",
          "X-User-Sub": "test-sub-123",
          "X-User-Name": "Gebruiker",
          Authorization: "Bearer test-token",
        },
      });
      expect(result).toEqual([{ id: 1, rating: 5, comment: "Goed" }]);
    });
  });

  describe("Leeslijst functionality", () => {
    it("should toggle a class reading list item for the selected school", async () => {
      localStorage.setItem("selectedSchoolId", "7");
      const axiosPostSpy = spyOn(axios, "post").and.resolveTo({
        data: true,
      } as any);

      const result = await service.toggleClassReadingListItem(42);

      expect(axiosPostSpy).toHaveBeenCalledWith(
        "/api/class-reading-list/42/toggle?schoolId=7",
        {},
        {
          headers: {
            "X-User-Sub": "test-sub-123",
            Authorization: "Bearer test-token",
          },
        },
      );
      expect(result).toBeTrue();
    });

    it("should return false when no school is selected for class reading list status", async () => {
      const axiosGetSpy = spyOn(axios, "get");

      const result = await service.isClassReadingListItem(42);

      expect(result).toBeFalse();
      expect(axiosGetSpy).not.toHaveBeenCalled();
    });

    it("should check class reading list status for the selected school", async () => {
      localStorage.setItem("selectedSchoolId", "11");
      const axiosGetSpy = spyOn(axios, "get").and.resolveTo({
        data: false,
      } as any);

      const result = await service.isClassReadingListItem(99);

      expect(axiosGetSpy).toHaveBeenCalledWith(
        "/api/class-reading-list/99/status?schoolId=11",
        {
          headers: {
            "X-User-Sub": "test-sub-123",
            Authorization: "Bearer test-token",
          },
        },
      );
      expect(result).toBeFalse();
    });

    it("should get class reading list item ids for a school", async () => {
      const axiosGetSpy = spyOn(axios, "get").and.resolveTo({
        data: [1, 2, 3],
      } as any);

      const result = await service.getClassReadingListItemIds(5);

      expect(axiosGetSpy).toHaveBeenCalledWith(
        "/api/class-reading-list/school/5",
        {
          headers: {
            "X-User-Sub": "test-sub-123",
            Authorization: "Bearer test-token",
          },
        },
      );
      expect(result).toEqual([1, 2, 3]);
    });

    it("should create a leeslijst with title, description, books and klas ids", async () => {
      const axiosPostSpy = spyOn(axios, "post").and.resolveTo({
        data: { id: 77 },
      } as any);

      const result = await service.createLeeslijst(
        "Nieuwe lijst",
        "Beschrijving",
        [10, 11],
        [3, 4],
      );

      expect(axiosPostSpy).toHaveBeenCalledWith(
        "/api/leeslisten",
        jasmine.objectContaining({
          titel: "Nieuwe lijst",
          description: "Beschrijving",
          bookIds: [10, 11],
          klasIds: [3, 4],
        }),
        {
          headers: {
            "X-User-Role": "leerkracht",
            "X-User-Sub": "test-sub-123",
            "X-User-Name": "Gebruiker",
            Authorization: "Bearer test-token",
          },
        },
      );
      expect(result).toEqual({ id: 77 });
    });

    it("should get leeslijst by id with full auth headers", async () => {
      const axiosGetSpy = spyOn(axios, "get").and.resolveTo({
        data: { id: 9, titel: "Lijst" },
      } as any);

      const result = await service.getLeeslijst(9);

      expect(axiosGetSpy).toHaveBeenCalledWith("/api/leeslisten/9", {
        headers: {
          "X-User-Role": "leerkracht",
          "X-User-Sub": "test-sub-123",
          "X-User-Name": "Gebruiker",
          Authorization: "Bearer test-token",
        },
      });
      expect(result).toEqual({ id: 9, titel: "Lijst" });
    });

    it("should update leeslijst with title, description, books and klas ids", async () => {
      const axiosPutSpy = spyOn(axios, "put").and.resolveTo({
        data: { id: 9, titel: "Bijgewerkt" },
      } as any);

      const result = await service.updateLeeslijst(
        9,
        "Bijgewerkt",
        "Nieuwe beschrijving",
        [1],
        [2],
      );

      expect(axiosPutSpy).toHaveBeenCalledWith(
        "/api/leeslisten/9",
        jasmine.objectContaining({
          titel: "Bijgewerkt",
          description: "Nieuwe beschrijving",
          bookIds: [1],
          klasIds: [2],
        }),
        {
          headers: {
            "X-User-Role": "leerkracht",
            "X-User-Sub": "test-sub-123",
            "X-User-Name": "Gebruiker",
            Authorization: "Bearer test-token",
          },
        },
      );
      expect(result).toEqual({ id: 9, titel: "Bijgewerkt" });
    });
  });
});
