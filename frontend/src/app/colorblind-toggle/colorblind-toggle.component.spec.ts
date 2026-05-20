import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ColorblindToggleComponent } from "../colorblind-toggle/colorblind-toggle.component";
import {
  // Corrected path
  ColorblindService,
  ColorblindMode,
} from "../services/colorblind.service";
import { of } from "rxjs";

describe("ColorblindToggleComponent", () => {
  let component: ColorblindToggleComponent;
  let fixture: ComponentFixture<ColorblindToggleComponent>;
  let colorblindServiceSpy: jasmine.SpyObj<ColorblindService>;

  beforeEach(async () => {
    colorblindServiceSpy = jasmine.createSpyObj("ColorblindService", [
      "getMode",
      "toggle",
    ]);
    colorblindServiceSpy.getMode.and.returnValue("none");

    await TestBed.configureTestingModule({
      imports: [ColorblindToggleComponent],
      providers: [
        { provide: ColorblindService, useValue: colorblindServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ColorblindToggleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should toggle colorblind mode on click", () => {
    component.service.toggle();
    expect(colorblindServiceSpy.toggle).toHaveBeenCalled();
  });
});
