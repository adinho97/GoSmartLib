import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { Router } from "@angular/router";
import { SuperAdminDashboardComponent } from "../super-admin-dashboard/super-admin-dashboard.component";
import { AdminSchoolService } from "../services/admin-school.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { registerLocaleData } from "@angular/common";
import localeNl from "@angular/common/locales/nl";

// Register Dutch locale for Decimal/Number pipes
registerLocaleData(localeNl);

describe("SuperAdminDashboardComponent", () => {
  let component: SuperAdminDashboardComponent;
  let fixture: ComponentFixture<SuperAdminDashboardComponent>;
  let adminSchoolServiceSpy: jasmine.SpyObj<AdminSchoolService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    adminSchoolServiceSpy = jasmine.createSpyObj("AdminSchoolService", [
      "getSchools",
      "getSchoolDetail",
    ]);
    adminSchoolServiceSpy.getSchools.and.returnValue(
      of([
        {
          id: 1,
          naam: "School A",
          subdomain: "schoola",
          status: "ACTIVE",
          userCount: 10,
          klasCount: 2,
        },
        {
          id: 2,
          naam: "School B",
          subdomain: "schoolb",
          status: "PENDING",
          userCount: 5,
          klasCount: 1,
        },
      ]),
    );
    adminSchoolServiceSpy.getSchoolDetail.and.returnValue(of({} as any));
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [SuperAdminDashboardComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AdminSchoolService, useValue: adminSchoolServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load schools on init", () => {
    expect(adminSchoolServiceSpy.getSchools).toHaveBeenCalled();
    expect(component.schools.length).toBe(2);
    expect(component.schools[0].naam).toBe("School A");
  });

  it("should navigate to school detail on viewSchool", () => {
    const schoolId = 1;
    component.goToSchoolDetail(schoolId);
    expect(routerSpy.navigate).toHaveBeenCalledWith([
      "/admin/schools",
      schoolId,
    ]);
  });
});
