import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { Router, ActivatedRoute } from "@angular/router";
import { LeeslijstViewComponent } from "../leeslijst-view/leeslijst-view.component";
import { BookService } from "../services/book.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("LeeslijstViewComponent", () => {
  let component: LeeslijstViewComponent;
  let fixture: ComponentFixture<LeeslijstViewComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let activatedRouteSpy: any;

  const mockLeeslijst = {
    id: 1,
    titel: "My Reading List",
    description: "Books to read",
    bookIds: [101, 102],
  };

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj("BookService", ["getLeeslijst"]);
    bookServiceSpy.getLeeslijst.and.returnValue(
      Promise.resolve({
        ...mockLeeslijst,
        books: [
          { bookId: 101, titel: "Book 101" },
          { bookId: 102, titel: "Book 102" },
        ],
      }),
    );

    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);
    activatedRouteSpy = {
      snapshot: {
        paramMap: { get: (key: string) => (key === "id" ? "1" : null) },
      },
    };

    await TestBed.configureTestingModule({
      declarations: [LeeslijstViewComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeeslijstViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load reading list details on init", fakeAsync(async () => {
    // ngOnInit is called in beforeEach, which triggers loadLeeslijst
    // We need to ensure the mock for getLeeslijst resolves with enriched books
    bookServiceSpy.getLeeslijst.and.returnValue(
      Promise.resolve({
        ...mockLeeslijst,
        books: [
          { bookId: 101, titel: "Book 101" },
          { bookId: 102, titel: "Book 102" },
        ],
      }),
    );
    await component.ngOnInit(); // Re-trigger ngOnInit to use the updated mock
    tick(); // Advance time for promises to resolve
    expect(bookServiceSpy.getLeeslijst).toHaveBeenCalledWith(1);
    expect(component.leeslijst?.titel).toBe("My Reading List");
    expect(component.leeslijst?.books.length).toBe(2);
    expect(component.leeslijst?.books[0].titel).toBe("Book 101");
  }));
});
