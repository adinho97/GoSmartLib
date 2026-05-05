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
  });

  describe("Klasleeslijst functionality", () => {
    it("should toggle a class reading list item for the selected school", async () => {
      localStorage.setItem("selectedSchoolId", "7");
      localStorage.setItem("sub", "student-123");

      const axiosPostSpy = spyOn(axios, "post").and.resolveTo({
        data: true,
      } as any);

      const result = await service.toggleClassReadingListItem(42);

      expect(axiosPostSpy).toHaveBeenCalledWith(
        "/api/class-reading-list/42/toggle?schoolId=7",
        {},
        {
          headers: {
            "X-User-Sub": "student-123",
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
      localStorage.setItem("sub", "student-456");

      const axiosGetSpy = spyOn(axios, "get").and.resolveTo({
        data: false,
      } as any);

      const result = await service.isClassReadingListItem(99);

      expect(axiosGetSpy).toHaveBeenCalledWith(
        "/api/class-reading-list/99/status?schoolId=11",
        {
          headers: {
            "X-User-Sub": "student-456",
          },
        },
      );
      expect(result).toBeFalse();
    });

    it("should save a class reading list with role-based headers", async () => {
      localStorage.setItem("role", "leerkracht");
      localStorage.setItem("sub", "teacher-77");
      localStorage.setItem("firstName", "Anna");
      localStorage.setItem("lastName", "Berg");

      const axiosPostSpy = spyOn(axios, "post").and.resolveTo({
        data: undefined,
      } as any);

      await service.saveClassReadingList({
        klassenIds: [1, 4],
        bookIds: [10, 20],
      });

      expect(axiosPostSpy).toHaveBeenCalledWith(
        "/api/leeslijsten",
        {
          klassenIds: [1, 4],
          bookIds: [10, 20],
        },
        {
          headers: {
            "X-User-Role": "leerkracht",
            "X-User-Sub": "teacher-77",
            "X-User-Name": "Anna Berg",
          },
        },
      );
    });

    it("should throw when toggling a class reading list item without a selected school", async () => {
      const axiosPostSpy = spyOn(axios, "post");

      await expectAsync(
        service.toggleClassReadingListItem(42),
      ).toBeRejectedWithError("No school selected.");
      expect(axiosPostSpy).not.toHaveBeenCalled();
    });
  });
});
