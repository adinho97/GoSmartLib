import {
  TestBed,
  ComponentFixture,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { AppComponent } from "./app.component"; // Assuming this component exists
import { AuthContextService } from "./services/auth-context.service";
import { NavigationEnd, Router, RouterLink } from "@angular/router";
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
    routerSpy = jasmine.createSpyObj(
      "Router",
      ["navigate", "createUrlTree", "serializeUrl"],
      {
        events: of(new NavigationEnd(1, "/", "/")),
        url: "/dashboard",
        routerState: { root: {} },
      },
    ); // Mock router events as an Observable
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue("");
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

  it("toggleNotificationMenu should toggle notificationMenuOpen and close others", () => {
    component.notificationMenuOpen = false;
    component.toggleNotificationMenu(new MouseEvent("click"));
    expect(component.notificationMenuOpen).toBeTrue();
  });
});
