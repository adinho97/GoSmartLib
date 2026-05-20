import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router"; // Corrected path
import { AdminSchoolWizardComponent } from "./admin-school-wizard.component";
import { AdminSchoolService } from "../services/admin-school.service";
import { of, throwError } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("AdminSchoolWizardComponent", () => {
  let component: AdminSchoolWizardComponent;
  let fixture: ComponentFixture<AdminSchoolWizardComponent>;
  let adminSchoolServiceSpy: jasmine.SpyObj<AdminSchoolService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    adminSchoolServiceSpy = jasmine.createSpyObj("AdminSchoolService", [
      "createSchool",
    ]);
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [AdminSchoolWizardComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: AdminSchoolService, useValue: adminSchoolServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSchoolWizardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should call createSchool on submit and navigate to school detail on success", fakeAsync(() => {
    adminSchoolServiceSpy.createSchool.and.returnValue(
      of({
        id: 1,
        subdomain: "test",
        smartschoolUrl: "url",
        status: "PENDING",
        createdAt: "now",
      }),
    );
    component.subdomain = "test";
    component.naam = "Test School";

    component.confirmCreate();
    tick();

    expect(adminSchoolServiceSpy.createSchool).toHaveBeenCalledWith(
      jasmine.objectContaining({ subdomain: "test", naam: "Test School" }),
    );
    expect(routerSpy.navigate).toHaveBeenCalledWith(["/admin/schools", 1]);
    expect(component.errorMessage).toBe("");
  }));

  it("should show error message on failed school creation", fakeAsync(() => {
    adminSchoolServiceSpy.createSchool.and.returnValue(
      throwError(() => new Error("Creation failed")),
    );
    component.subdomain = "test";
    component.naam = "Test School";

    component.confirmCreate();
    tick();

    expect(adminSchoolServiceSpy.createSchool).toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(component.errorMessage).toBe("Fout bij het aanmaken van de school.");
  }));
});
