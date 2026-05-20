import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { UiToastComponent } from "./ui-toast.component";
import {
  // Corrected path
  UiToastService,
  UiToastMessage,
} from "../../services/ui-toast.service";
import { Subject } from "rxjs";

describe("UiToastComponent", () => {
  let component: UiToastComponent;
  let fixture: ComponentFixture<UiToastComponent>;
  let uiToastService: UiToastService;
  let toastSubject: Subject<UiToastMessage>;

  beforeEach(async () => {
    toastSubject = new Subject<UiToastMessage>();
    await TestBed.configureTestingModule({
      imports: [UiToastComponent],
      providers: [
        {
          provide: UiToastService,
          useValue: { toasts$: toastSubject.asObservable() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UiToastComponent);
    component = fixture.componentInstance;
    uiToastService = TestBed.inject(UiToastService);
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should show toast message when service emits", fakeAsync(() => {
    const message: UiToastMessage = {
      text: "Test message",
      kind: "success",
      durationMs: 1000,
    };
    toastSubject.next(message);
    fixture.detectChanges();

    expect(component.currentToast).toEqual({
      text: "Test message",
      kind: "success",
    });
    expect(component.currentToast?.text).toBe("Test message");
    expect(component.currentToast?.kind).toBe("success");

    tick(1000);
    expect(component.currentToast).toBeNull();
  }));
});
