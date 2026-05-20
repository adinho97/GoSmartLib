import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router"; // Corrected path
import { AdminChangePasswordComponent } from "./admin-change-password.component";
import { SuperAdminAuthService } from "../services/super-admin-auth.service";
import { of, throwError } from "rxjs";

describe("AdminChangePasswordComponent", () => {
  let component: AdminChangePasswordComponent;
  let fixture: ComponentFixture<AdminChangePasswordComponent>;
  let superAdminAuthServiceSpy: jasmine.SpyObj<SuperAdminAuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    superAdminAuthServiceSpy = jasmine.createSpyObj("SuperAdminAuthService", [
      "changePassword",
    ]);
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [AdminChangePasswordComponent],
      imports: [FormsModule],
      providers: [
        { provide: SuperAdminAuthService, useValue: superAdminAuthServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminChangePasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should call changePassword on submit and show success message", fakeAsync(() => {
    superAdminAuthServiceSpy.changePassword.and.returnValue(
      of("Password changed successfully"),
    );
    component.oldPassword = "CurrentPassword123!";
    component.newPassword = "NewPassword123!";
    component.confirmPassword = "NewPassword123!";

    component.submit();
    tick();

    expect(superAdminAuthServiceSpy.changePassword).toHaveBeenCalledWith(
      "CurrentPassword123!",
      "NewPassword123!",
    );
    expect(component.successMessage).toBe("Wachtwoord succesvol gewijzigd.");
    expect(component.errorMessage).toBe("");
  }));

  it("should show error message on failed password change", fakeAsync(() => {
    superAdminAuthServiceSpy.changePassword.and.returnValue(
      throwError(() => new Error("Change failed")),
    );
    component.oldPassword = "CurrentPassword123!";
    component.newPassword = "NewPassword123!";
    component.confirmPassword = "NewPassword123!";

    component.submit();
    tick();

    expect(superAdminAuthServiceSpy.changePassword).toHaveBeenCalledWith(
      "CurrentPassword123!",
      "NewPassword123!",
    );
    expect(component.errorMessage).toBe("Wachtwoord wijzigen is mislukt.");
    expect(component.successMessage).toBe("");
  }));
});
