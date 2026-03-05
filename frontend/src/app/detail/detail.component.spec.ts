import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute, convertToParamMap } from "@angular/router";
import { of } from "rxjs";

import { DetailComponent } from "./detail.component";
import { BookService } from "../services/book.service";
import { Location } from "@angular/common";

describe("DetailComponent", () => {
  let component: DetailComponent;
  let fixture: ComponentFixture<DetailComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let locationSpy: jasmine.SpyObj<Location>;

  beforeEach(() => {
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBookById",
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
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
