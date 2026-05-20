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
    bookServiceSpy = jasmine.createSpyObj("BookService", [
      "getLeeslijst",
      "enrichBooksWithDetails",
    ]);
    bookServiceSpy.getLeeslijst.and.returnValue(Promise.resolve(mockLeeslijst));
    bookServiceSpy.enrichBooksWithDetails.and.callFake((books: any[]) =>
      Promise.resolve(books.map((b) => ({ ...b, titel: `Book ${b.bookId}` }))),
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

  it("should load reading list details on init", fakeAsync(() => {
    // Re-call ngOnInit to ensure all async operations are covered by fakeAsync
    component.ngOnInit();
    tick(); // Advance time for promises to resolve
    expect(bookServiceSpy.getLeeslijst).toHaveBeenCalledWith(1);
    expect(bookServiceSpy.enrichBooksWithDetails).toHaveBeenCalledWith([
      { bookId: 101 },
      { bookId: 102 },
    ]);
    expect(component.leeslijst?.titel).toBe("My Reading List");
    expect(component.leeslijst?.books.length).toBe(2);
    expect(component.leeslijst?.books[0].titel).toBe("Book 101");
  }));
});
