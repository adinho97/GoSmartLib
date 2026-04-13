import { TestBed } from "@angular/core/testing";
import { ExperienceService } from "./experience.service";

describe("ExperienceService", () => {
  let service: ExperienceService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [ExperienceService],
    });

    service = TestBed.inject(ExperienceService);
    service.resetExperience();
  });

  it("should grant 50 XP for creating a loan", () => {
    service.addExperienceForLoaningBook();

    let levelInfo = service["levelInfoSubject"].value;
    expect(levelInfo.totalExperience).toBe(50);
  });

  it("should add and remove review XP", () => {
    service.addExperienceForReview();
    service.addExperienceForReview();
    service.removeExperienceForReview();

    let levelInfo = service["levelInfoSubject"].value;
    expect(levelInfo.totalExperience).toBe(10);
  });

  it("should clamp XP to zero when removing more XP than available", () => {
    service.removeExperience(1000);

    let levelInfo = service["levelInfoSubject"].value;
    expect(levelInfo.totalExperience).toBe(0);
  });

  it("should keep loan badge XP at double review badge XP", () => {
    const reviewBadgeXp = service.getBadgeExperienceWorth(20, "review");
    const loanBadgeXp = service.getBadgeExperienceWorth(20, "loan");

    expect(loanBadgeXp).toBe(reviewBadgeXp * 2);
  });

  it("should award badge XP only once per badge", () => {
    const firstReward = service.addExperienceForBadge(10, "review");
    const secondReward = service.addExperienceForBadge(10, "review");

    let levelInfo = service["levelInfoSubject"].value;
    expect(firstReward).toBeGreaterThan(0);
    expect(secondReward).toBe(0);
    expect(levelInfo.totalExperience).toBe(firstReward);
  });

  it("should increase level after enough XP is gained", () => {
    service.addExperience(260);

    let levelInfo = service["levelInfoSubject"].value;
    expect(levelInfo.level).toBeGreaterThan(1);
    expect(levelInfo.experienceRequiredForLevel).toBe(225);
  });
});
