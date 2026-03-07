import { ComponentFixture, TestBed } from "@angular/core/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { Router } from "@angular/router";

import { DashboardComponent } from "./dashboard.component";
import { BookService } from "../services/book.service";

describe("DashboardComponent", () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
      "getBoeken",
    ]);
    bookServiceSpy.getBoeken.and.resolveTo([]);
    routerSpy = jasmine.createSpyObj<Router>("Router", ["navigate"]);

    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
