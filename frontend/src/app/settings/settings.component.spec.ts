import { ComponentFixture, TestBed } from "@angular/core/testing";
import { SettingsComponent } from "./settings.component";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { SettingsService } from "../services/settings.service";
import { SchoolService } from "../services/school.service";
import { AdminGenreService } from "../services/admin-genre.service";
import { UiToastService } from "../services/ui-toast.service";
import { of } from "rxjs";

describe("SettingsComponent", () => {
  let component: SettingsComponent;
  let fixture: ComponentFixture<SettingsComponent>;

  beforeEach(async () => {
    const settingsMock = {
      getSchoolName: () => of({ name: "Test School" }),
      getSettings: () => of({ messages: [], hours: null, levels: null }),
    };
    const schoolMock = {
      getSelectedSchoolId: () => 1,
      getDefaultLoanDays: () => of(14),
    };
    const genreMock = {
      getAll: () => of([]),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsComponent, HttpClientTestingModule],
      providers: [
        { provide: SettingsService, useValue: settingsMock },
        { provide: SchoolService, useValue: schoolMock },
        { provide: AdminGenreService, useValue: genreMock },
        {
          provide: UiToastService,
          useValue: { error: () => {}, success: () => {}, info: () => {} },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
