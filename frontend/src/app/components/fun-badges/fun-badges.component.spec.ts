import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FunBadgesComponent } from "./fun-badges.component";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { LoanService } from "../../services/loan.service";
import { BookService } from "../../services/book.service";
import { ExperienceService } from "../../services/experience.service";

describe("FunBadgesComponent", () => {
  let component: FunBadgesComponent;
  let fixture: ComponentFixture<FunBadgesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FunBadgesComponent, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(FunBadgesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
