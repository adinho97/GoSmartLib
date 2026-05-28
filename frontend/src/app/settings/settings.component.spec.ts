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

  it("should mark as dirty and open confirm modal when requesting message deletion", () => {
    const mockMsg = { id: 'm1', title: 'Test', body: 'Body', enabled: true } as any;
    component.messages = [mockMsg];
    component.requestDeleteMessage(mockMsg);
    
    expect(component.confirmTarget).toBeTruthy();
    expect(component.confirmTarget?.title).toContain("verwijderen");
  });

  it("should successfully delete a custom reading level", () => {
    const customLevel = { id: 'custom-1', name: 'Expert', isProtected: false, active: true } as any;
    component.levels = [customLevel];
    
    component.requestDeleteLevel(customLevel);
    // Trigger confirmation logic
    component.confirmTarget?.onConfirm();

    expect(component.levels.find(l => l.id === 'custom-1')).toBeUndefined();
  });
});
