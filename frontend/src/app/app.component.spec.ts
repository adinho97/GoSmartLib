import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { of } from "rxjs";
import { HttpClient } from "@angular/common/http";

import { AppComponent } from "./app.component";
import { ExperienceService } from "./services/experience.service";
import { UserPreferencesService } from "./services/user-preferences.service";
import { RecommendationService } from "./services/recommendation.service";
import { BookService } from "./services/book.service";

describe("AppComponent", () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let router: Router;

  const experienceServiceMock = {
    levelInfo$: of({
      level: 1,
      progressPercentage: 0,
      experienceForCurrentLevel: 0,
      experienceRequiredForLevel: 100,
    }),
  };

  const userPreferencesServiceMock = {
    init: jasmine.createSpy("init"),
    clearCache: jasmine.createSpy("clearCache"),
  };

  const recommendationServiceMock = {
    clearCache: jasmine.createSpy("clearCache"),
  };

  const bookServiceMock = {
    clearCache: jasmine.createSpy("clearCache"),
  };

  const httpClientMock = jasmine.createSpyObj<HttpClient>("HttpClient", [
    "post",
  ]);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: ExperienceService, useValue: experienceServiceMock },
        {
          provide: UserPreferencesService,
          useValue: userPreferencesServiceMock,
        },
        { provide: RecommendationService, useValue: recommendationServiceMock },
        { provide: BookService, useValue: bookServiceMock },
        { provide: HttpClient, useValue: httpClientMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    localStorage.clear();
  });

  it("should create", () => {
    spyOnProperty(router, "url", "get").and.returnValue("/dashboard");

    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(userPreferencesServiceMock.init).toHaveBeenCalled();
  });

  it("shows Informatie link for leerling", () => {
    localStorage.setItem("role", "leerling");
    spyOnProperty(router, "url", "get").and.returnValue("/dashboard");

    fixture.detectChanges();

    const navLinks = Array.from(
      fixture.nativeElement.querySelectorAll(".nav-links a"),
    ) as HTMLAnchorElement[];

    const infoLink = navLinks.find((link) =>
      link.textContent?.includes("Informatie"),
    );

    expect(infoLink).toBeTruthy();
  });

  it("does not show Informatie link for leerkracht", () => {
    localStorage.setItem("role", "leerkracht");
    spyOnProperty(router, "url", "get").and.returnValue("/dashboard");

    fixture.detectChanges();

    const navLinks = Array.from(
      fixture.nativeElement.querySelectorAll(".nav-links a"),
    ) as HTMLAnchorElement[];

    const infoLink = navLinks.find((link) =>
      link.textContent?.includes("Informatie"),
    );

    expect(infoLink).toBeUndefined();
  });
});
