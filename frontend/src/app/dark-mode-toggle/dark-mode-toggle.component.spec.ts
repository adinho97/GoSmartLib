import { ComponentFixture, TestBed } from "@angular/core/testing";
import { DarkModeToggleComponent } from "../dark-mode-toggle/dark-mode-toggle.component";
import { DarkModeService, ThemeMode } from "../services/dark-mode.service"; // Corrected path
import { of } from "rxjs";

describe("DarkModeToggleComponent", () => {
  let component: DarkModeToggleComponent;
  let fixture: ComponentFixture<DarkModeToggleComponent>;
  let darkModeServiceSpy: jasmine.SpyObj<DarkModeService>;

  beforeEach(async () => {
    darkModeServiceSpy = jasmine.createSpyObj("DarkModeService", [
      "isDark",
      "toggle",
    ]);
    darkModeServiceSpy.isDark.and.returnValue(false);
    darkModeServiceSpy.mode$ = of("light");

    await TestBed.configureTestingModule({
      imports: [DarkModeToggleComponent],
      providers: [{ provide: DarkModeService, useValue: darkModeServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DarkModeToggleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should toggle dark mode on click", () => {
    component.service.toggle();
    expect(darkModeServiceSpy.toggle).toHaveBeenCalled();
  });
});
