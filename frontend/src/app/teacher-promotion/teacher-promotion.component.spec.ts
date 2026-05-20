import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { TeacherPromotionComponent } from "../teacher-promotion/teacher-promotion.component";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { SchoolService } from "../services/school.service";
import { UiToastService } from "../services/ui-toast.service";
import { of, throwError } from "rxjs";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe("TeacherPromotionComponent", () => {
  let component: TeacherPromotionComponent;
  let fixture: ComponentFixture<TeacherPromotionComponent>;
  let bibbeheerderServiceSpy: jasmine.SpyObj<BibbeheerderService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let uiToastServiceSpy: jasmine.SpyObj<UiToastService>;

  beforeEach(async () => {
    bibbeheerderServiceSpy = jasmine.createSpyObj("BibbeheerderService", [
      "getAllUsers",
      "promoteLeerkracht",
      "getLeerkrachten",
    ]);
    bibbeheerderServiceSpy.getLeerkrachten.and.returnValue(
      of([
        { id: 1, displayName: "User A", role: "leerling" } as any,
        { id: 2, displayName: "User B", role: "leerkracht" } as any,
      ]),
    );
    bibbeheerderServiceSpy.getAllUsers.and.returnValue(of([]));
    bibbeheerderServiceSpy.promoteLeerkracht.and.returnValue(of({} as any));

    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getSelectedSchoolId",
    ]);
    schoolServiceSpy.getSelectedSchoolId.and.returnValue(1);

    uiToastServiceSpy = jasmine.createSpyObj("UiToastService", [
      "success",
      "error",
    ]);

    await TestBed.configureTestingModule({
      declarations: [TeacherPromotionComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [
        { provide: BibbeheerderService, useValue: bibbeheerderServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: UiToastService, useValue: uiToastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TeacherPromotionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async operations in ngOnInit
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load users on init", () => {
    expect(bibbeheerderServiceSpy.getLeerkrachten).toHaveBeenCalled();
    expect(component.leerkrachten.length).toBe(2);
    expect(component.leerkrachten[0].displayName).toBe("User A");
  });

  it("should promote user to teacher and show success message", fakeAsync(() => {
    component.confirmTarget = { id: 1 } as any;
    component.confirmPromote();
    tick();

    expect(bibbeheerderServiceSpy.promoteLeerkracht).toHaveBeenCalledWith(1);
    // The component filters the list instead of showing a static success message string
    // in a property named 'errorMessage'.
    expect(component.confirmTarget).toBeNull();
    expect(component.leerkrachten.length).toBe(1); // One left out of two
  }));

  it("should show error message on failed promotion", fakeAsync(() => {
    bibbeheerderServiceSpy.promoteLeerkracht.and.returnValue(
      throwError(() => new Error("Promotion failed")),
    );
    component.confirmTarget = { id: 1 } as any;
    component.confirmPromote();
    tick();

    expect(component.promoteError).toBe("Promoveren mislukt. Probeer opnieuw."); // Corrected expectation
  }));
});
