import {
  TestBed,
  ComponentFixture,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { AppComponent } from "./app.component"; // Assuming this component exists
import { AuthContextService } from "./services/auth-context.service";
import { NavigationEnd, Router, RouterLinkWithHref } from "@angular/router";
import { of } from "rxjs";
import { By } from "@angular/platform-browser";
import { DebugElement } from "@angular/core";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { UserPreferencesService } from "./services/user-preferences.service";
import { DashboardConfigService } from "./services/dashboard-config.service";
import { CommonModule } from "@angular/common";

describe("AppComponent", () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let authContextServiceSpy: jasmine.SpyObj<AuthContextService>;
  let routerSpy: jasmine.SpyObj<Router>;

  let userPreferencesServiceSpy: jasmine.SpyObj<UserPreferencesService>;
  let dashboardConfigServiceSpy: jasmine.SpyObj<DashboardConfigService>;

  beforeEach(async () => {
    authContextServiceSpy = jasmine.createSpyObj("AuthContextService", [
      "getEffectiveRole",
      "isAdminMode",
    ]);
    routerSpy = jasmine.createSpyObj("Router", ["navigate"], {
      events: of(new NavigationEnd(1, "/", "/")),
      url: "/dashboard",
    }); // Mock router events as an Observable
    userPreferencesServiceSpy = jasmine.createSpyObj("UserPreferencesService", [
      "init",
      "preferences$",
    ]);
    userPreferencesServiceSpy.preferences$ = of({}); // Mock preferences$ as an Observable
    dashboardConfigServiceSpy = jasmine.createSpyObj("DashboardConfigService", [
      "init",
      "config$",
    ]);
    dashboardConfigServiceSpy.config$ = of({ tiles: [], pages: {} }); // Mock config$ as an Observable

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, CommonModule],
      declarations: [AppComponent],
      providers: [
        { provide: AuthContextService, useValue: authContextServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: UserPreferencesService,
          useValue: userPreferencesServiceSpy,
        },
        {
          provide: DashboardConfigService,
          useValue: dashboardConfigServiceSpy,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  it("should create the app", () => {
    expect(component).toBeTruthy();
  });

  it("shows Informatie link for leerling", fakeAsync(() => {
    authContextServiceSpy.getEffectiveRole.and.returnValue("leerling");
    authContextServiceSpy.isAdminMode.and.returnValue(false);
    localStorage.setItem("role", "leerling"); // Ensure localStorage also reflects the role

    fixture.detectChanges(); // Trigger initial change detection
    tick(); // Process microtasks (e.g., router events, promises)

    const infoLink = fixture.debugElement.query(
      By.directive(RouterLinkWithHref),
    );
    expect(infoLink).not.toBeNull(); // Check if the RouterLinkWithHref directive is found
    expect(infoLink.injector.get(RouterLinkWithHref).routerLink).toBe("/info"); // Check the routerLink value
  }));
});
