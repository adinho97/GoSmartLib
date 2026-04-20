import { TestBed } from "@angular/core/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import axios from "axios";

import { BookService } from "./book.service";

describe("BookService", () => {
  let service: BookService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(BookService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("Favoriting functionality", () => {
    beforeEach(() => {
      spyOn(localStorage, "getItem").and.returnValue("test-user");
    });

    it("should add a book to favorites", async () => {
      const mockResponse = { data: {} };
      spyOn(axios, "post").and.resolveTo(mockResponse);

      await service.addToFavorites(123);

      expect(axios.post).toHaveBeenCalledWith(
        "/api/favorieten",
        { bookId: 123 },
        jasmine.any(Object),
      );
    });

    it("should remove a book from favorites", async () => {
      const mockResponse = { data: {} };
      spyOn(axios, "delete").and.resolveTo(mockResponse);

      await service.removeFromFavorites(123);

      expect(axios.delete).toHaveBeenCalledWith(
        "/api/favorieten/123",
        jasmine.any(Object),
      );
    });

    it("should get user favorites", async () => {
      const mockFavorites = [
        {
          id: 1,
          bookId: 123,
          titel: "Test Book",
          auteur: "Test Author",
          cover: null,
          addedAt: "2023-01-01",
        },
      ];
      const mockResponse = { data: mockFavorites };
      spyOn(axios, "get").and.resolveTo(mockResponse);

      const result = await service.getUserFavorites();

      expect(axios.get).toHaveBeenCalledWith(
        "/api/favorieten",
        jasmine.any(Object),
      );
      expect(result).toEqual(mockFavorites);
    });

    it("should check if a book is favorited (true)", async () => {
      const mockResponse = { data: true };
      spyOn(axios, "get").and.resolveTo(mockResponse);

      const result = await service.isFavorited(123);

      expect(axios.get).toHaveBeenCalledWith(
        "/api/favorieten/123/check",
        jasmine.any(Object),
      );
      expect(result).toBe(true);
    });

    it("should check if a book is favorited (false on 404)", async () => {
      const error = { response: { status: 404 } };
      spyOn(axios, "get").and.rejectWith(error);

      const result = await service.isFavorited(123);

      expect(result).toBe(false);
    });

    it("should throw error for isFavorited on non-404 error", async () => {
      const error = { response: { status: 500 } };
      spyOn(axios, "get").and.rejectWith(error);

      await expectAsync(service.isFavorited(123)).toBeRejected();
    });
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
  });
});
