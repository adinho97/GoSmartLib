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
