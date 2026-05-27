import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { MijnTakenComponent } from "./mijn-taken.component";

describe("MijnTakenComponent", () => {
  let component: MijnTakenComponent;
  let fixture: ComponentFixture<MijnTakenComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MijnTakenComponent],
      imports: [RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(MijnTakenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("isLibrarian reflects the role in localStorage", () => {
    spyOn(localStorage, "getItem").and.returnValue("bibbeheerder");
    expect(component.isLibrarian).toBeTrue();
  });
});
