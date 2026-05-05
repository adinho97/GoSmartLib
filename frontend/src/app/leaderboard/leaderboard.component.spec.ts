import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { Router } from "@angular/router";
import { of, throwError } from "rxjs";
import { LeaderboardComponent, LeaderboardData } from "./leaderboard.component";
import { LeaderboardService } from "../services/leaderboard.service";
import { AuthContextService } from "../services/auth-context.service";
import axios from "axios";

describe("LeaderboardComponent", () => {
  let component: LeaderboardComponent;
  let fixture: ComponentFixture<LeaderboardComponent>;
  let leaderboardService: jasmine.SpyObj<LeaderboardService>;
  let authContextService: jasmine.SpyObj<AuthContextService>;
  let router: jasmine.SpyObj<Router>;

  const mockLeaderboardData: LeaderboardData = {
    topClassReaders: [
      { rank: 1, displayName: "User 1", count: 10, isCurrentUser: true },
      { rank: 2, displayName: "User 2", count: 8, isCurrentUser: false },
    ],
    topSchoolReaders: [
      { rank: 1, displayName: "User 1", count: 10, isCurrentUser: true },
    ],
    userClassRank: {
      rank: 1,
      displayName: "User 1",
      count: 10,
      isCurrentUser: true,
    },
    userSchoolRank: {
      rank: 1,
      displayName: "User 1",
      count: 10,
      isCurrentUser: true,
    },
    availableClasses: [{ id: 1, naam: "6A" }],
  };

  beforeEach(async () => {
    const lbServiceSpy = jasmine.createSpyObj("LeaderboardService", [
      "getLeaderboardData",
    ]);
    const authSpy = jasmine.createSpyObj("AuthContextService", [
      "getEffectiveBearerToken",
      "getEffectiveSub",
    ]);
    const routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    await TestBed.configureTestingModule({
      declarations: [LeaderboardComponent],
      providers: [
        { provide: LeaderboardService, useValue: lbServiceSpy },
        { provide: AuthContextService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeaderboardComponent);
    component = fixture.componentInstance;
    leaderboardService = TestBed.inject(
      LeaderboardService,
    ) as jasmine.SpyObj<LeaderboardService>;
    authContextService = TestBed.inject(
      AuthContextService,
    ) as jasmine.SpyObj<AuthContextService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  describe("ngOnInit", () => {
    it("should set isTeacherOrLibrarian to true for leerkracht", () => {
      spyOn(localStorage, "getItem").and.returnValue("leerkracht");
      leaderboardService.getLeaderboardData.and.returnValue(
        of(mockLeaderboardData),
      );

      component.ngOnInit();

      expect(component.isTeacherOrLibrarian).toBeTrue();
    });

    it("should set isTeacherOrLibrarian to false for leerling", () => {
      spyOn(localStorage, "getItem").and.returnValue("leerling");
      leaderboardService.getLeaderboardData.and.returnValue(
        of(mockLeaderboardData),
      );

      component.ngOnInit();

      expect(component.isTeacherOrLibrarian).toBeFalse();
    });
  });

  describe("fetchLeaderboardData", () => {
    it("should fetch data and resolve names on success", fakeAsync(() => {
      leaderboardService.getLeaderboardData.and.returnValue(
        of(mockLeaderboardData),
      );
      spyOn(axios, "get").and.returnValue(
        Promise.resolve({ data: { firstName: "Resolved", lastName: "Name" } }),
      );
      authContextService.getEffectiveSub.and.returnValue("sub");
      authContextService.getEffectiveBearerToken.and.returnValue("token");

      component.fetchLeaderboardData();
      tick(); // resolve async resolveNames

      expect(component.leaderboardData).toBeDefined();
      expect(component.isLoading).toBeFalse();
      expect(component.error).toBeNull();
    }));

    it("should handle error when fetching data", () => {
      leaderboardService.getLeaderboardData.and.returnValue(
        throwError(() => new Error("API Error")),
      );

      component.fetchLeaderboardData();

      expect(component.isLoading).toBeFalse();
      expect(component.error).toContain("Er is een fout opgetreden");
    });
  });

  describe("Helper methods", () => {
    it("getInitials should return correct initials", () => {
      expect(component.getInitials("John Doe")).toBe("JD");
      expect(component.getInitials("Single")).toBe("S");
      expect(component.getInitials("")).toBe("?");
      expect(component.getInitials("  ")).toBe("?");
      expect(component.getInitials("First Middle Last")).toBe("FL");
    });

    it("isUserInTopClass should return true if rank <= 10", () => {
      const entry = { rank: 5, displayName: "Test", count: 5 };
      expect(component.isUserInTopClass(entry)).toBeTrue();
    });

    it("isUserInTopClass should return false if rank > 10", () => {
      const entry = { rank: 11, displayName: "Test", count: 5 };
      expect(component.isUserInTopClass(entry)).toBeFalse();
    });

    it("isUserInTopSchool should return true if rank <= 10", () => {
      const entry = { rank: 3, displayName: "Test", count: 5 };
      expect(component.isUserInTopSchool(entry)).toBeTrue();
    });
  });

  describe("Interactions", () => {
    it("onKlasChange should update selectedKlasId and refetch data", () => {
      spyOn(component, "fetchLeaderboardData");
      const event = { target: { value: "123" } };

      component.onKlasChange(event);

      expect(component.selectedKlasId).toBe(123);
      expect(component.fetchLeaderboardData).toHaveBeenCalled();
    });

    it("onKlasChange should handle null value", () => {
      spyOn(component, "fetchLeaderboardData");
      const event = { target: { value: "" } };

      component.onKlasChange(event);

      expect(component.selectedKlasId).toBeNull();
      expect(component.fetchLeaderboardData).toHaveBeenCalled();
    });

    it("goBack should navigate to dashboard", () => {
      component.goBack();
      expect(router.navigate).toHaveBeenCalledWith(["/dashboard"]);
    });
  });
});
