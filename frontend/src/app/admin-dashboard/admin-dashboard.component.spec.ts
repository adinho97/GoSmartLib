import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { Router } from "@angular/router";
import { AdminDashboardComponent } from "../admin-dashboard/admin-dashboard.component";
import { AdminSchoolService } from "../services/admin-school.service";
import { of } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("AdminDashboardComponent", () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminSchoolServiceSpy: jasmine.SpyObj<AdminSchoolService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    adminSchoolServiceSpy = jasmine.createSpyObj("AdminSchoolService", [
      "getSchools",
      "getSchoolDetail", // Added to prevent potential errors from sub-components or indirect calls
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
    // Provide a default return value for getSchoolDetail if it's called
    adminSchoolServiceSpy.getSchoolDetail.and.returnValue(of({} as any));
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [AdminDashboardComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AdminSchoolService, useValue: adminSchoolServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", fakeAsync(() => {
    expect(component).toBeTruthy();
    tick(); // Resolve any pending promises
  }));

  it("should load schools on init", fakeAsync(() => {
    expect(adminSchoolServiceSpy.getSchools).toHaveBeenCalled();
    expect(component.schools.length).toBe(2);
    expect(component.schools[0].naam).toBe("School A");
    tick(); // Resolve any pending promises
  }));

  it("should navigate to school detail on viewSchool", fakeAsync(() => {
    const schoolId = 1;
    component.goToSchoolDetail(schoolId);
    expect(routerSpy.navigate).toHaveBeenCalledWith([
      "/admin/schools",
      schoolId,
    ]);
    tick(); // Resolve any pending promises
  }));
});
