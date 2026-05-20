import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router"; // Corrected path
import { SuperAdminLoginComponent } from "./super-admin-login.component";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";
import { of, throwError } from "rxjs";

describe("SuperAdminLoginComponent", () => {
  let component: SuperAdminLoginComponent;
  let fixture: ComponentFixture<SuperAdminLoginComponent>;
  let superAdminAuthServiceSpy: jasmine.SpyObj<SuperAdminAuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    superAdminAuthServiceSpy = jasmine.createSpyObj("SuperAdminAuthService", [
      "login",
      "isAuthenticated",
    ]);
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [SuperAdminLoginComponent],
      imports: [FormsModule],
      providers: [
        { provide: SuperAdminAuthService, useValue: superAdminAuthServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdminLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should call login on submit and navigate to admin dashboard on success", fakeAsync(() => {
    superAdminAuthServiceSpy.login.and.returnValue(of({ token: "test-token" }));
    component.username = "admin";
    component.password = "password";

    component.login();
    tick();

    expect(superAdminAuthServiceSpy.login).toHaveBeenCalledWith(
      "admin",
      "password",
    );
    expect(routerSpy.navigate).toHaveBeenCalledWith(["/admin/dashboard"]);
    expect(component.errorMessage).toBe("");
  }));

  it("should show error message on failed login", fakeAsync(() => {
    superAdminAuthServiceSpy.login.and.returnValue(
      throwError(() => new Error("Login failed")),
    );
    component.username = "admin";
    component.password = "wrong";

    component.login();
    tick();

    expect(superAdminAuthServiceSpy.login).toHaveBeenCalledWith(
      "admin",
      "wrong",
    );
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(component.errorMessage).toContain("Inloggen mislukt");
  }));
});
