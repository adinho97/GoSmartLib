import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { BadgeToastComponent } from "./badge-toast.component";
import {
  // Corrected path
  BadgeNotificationService,
  BadgeUnlocked,
} from "../../services/badge-notification.service";
import { Subject } from "rxjs";

describe("BadgeToastComponent", () => {
  let component: BadgeToastComponent;
  let fixture: ComponentFixture<BadgeToastComponent>;
  let badgeNotificationService: BadgeNotificationService;
  let badgeUnlockedSubject: Subject<BadgeUnlocked>;

  beforeEach(async () => {
    badgeUnlockedSubject = new Subject<BadgeUnlocked>();
    await TestBed.configureTestingModule({
      imports: [BadgeToastComponent],
      providers: [
        {
          provide: BadgeNotificationService,
          useValue: { badgeUnlocked$: badgeUnlockedSubject.asObservable() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BadgeToastComponent);
    component = fixture.componentInstance;
    badgeNotificationService = TestBed.inject(BadgeNotificationService);
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should show toast when badgeUnlocked$ emits", fakeAsync(() => {
    const badge: BadgeUnlocked = {
      title: "New Badge",
      icon: "⭐",
      category: "loan",
    };
    badgeUnlockedSubject.next(badge);
    fixture.detectChanges();

    expect(component.visibleBadge).toEqual(badge);
    expect(component.visibleBadge?.title).toBe("New Badge");
    expect(component.visibleBadge?.icon).toBe("⭐");

    tick(4500); // Default duration for badge toast
    expect(component.visibleBadge).toBeNull();
  }));
});
