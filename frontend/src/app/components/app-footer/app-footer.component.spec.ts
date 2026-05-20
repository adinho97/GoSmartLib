import { ComponentFixture, TestBed } from "@angular/core/testing";
import { AppFooterComponent } from "./app-footer.component";
import { RouterTestingModule } from "@angular/router/testing";

describe("AppFooterComponent", () => {
  let component: AppFooterComponent;
  let fixture: ComponentFixture<AppFooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppFooterComponent, RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(AppFooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
