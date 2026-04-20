import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";

import { LeerlingInfoComponent } from "./leerling-info.component";

describe("LeerlingInfoComponent", () => {
  let component: LeerlingInfoComponent;
  let fixture: ComponentFixture<LeerlingInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LeerlingInfoComponent],
      imports: [RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LeerlingInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("renders quick actions from component config", () => {
    const actionLinks = fixture.nativeElement.querySelectorAll(".quick-action");

    expect(actionLinks.length).toBe(component.quickActions.length);
    expect(actionLinks[0].textContent).toContain("Boekencatalogus");
    expect(actionLinks[1].textContent).toContain("Dashboard");
  });

  it("shows first FAQ open by default", () => {
    const answers = fixture.nativeElement.querySelectorAll(".faq-answer");

    expect(component.openFaqIndex).toBe(0);
    expect(answers.length).toBe(1);
    expect(answers[0].textContent).toContain(component.faqItems[0].answer);
  });

  it("toggles FAQ open and close", () => {
    component.toggleFaq(1);
    fixture.detectChanges();

    let answers = fixture.nativeElement.querySelectorAll(".faq-answer");
    expect(component.isFaqOpen(1)).toBeTrue();
    expect(answers.length).toBe(1);
    expect(answers[0].textContent).toContain(component.faqItems[1].answer);

    component.toggleFaq(1);
    fixture.detectChanges();

    answers = fixture.nativeElement.querySelectorAll(".faq-answer");
    expect(component.openFaqIndex).toBeNull();
    expect(answers.length).toBe(0);
  });
});
