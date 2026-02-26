import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, NgForm } from "@angular/forms";
import { AddBookComponent } from "./add-book-component";
import { ItemService } from "./item.service";

class MockItemService {
  addBoek = jasmine.createSpy("addBoek").and.returnValue(Promise.resolve());
}

describe("AddBookComponent", () => {
  let component: AddBookComponent;
  let fixture: ComponentFixture<AddBookComponent>;
  let itemService: MockItemService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AddBookComponent],
      imports: [FormsModule],
      providers: [{ provide: ItemService, useClass: MockItemService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AddBookComponent);
    component = fixture.componentInstance;
    itemService = TestBed.inject(ItemService) as any;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should mark form invalid and show error message", async () => {
    const mockForm = {
      invalid: true,
      control: { markAllAsTouched: jasmine.createSpy() },
    } as unknown as NgForm;

    await component.onSubmit(mockForm);

    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toContain("Controleer het formulier");
  });

  it("should call service on valid submit", async () => {
    const mockForm = {
      invalid: false,
      resetForm: jasmine.createSpy(),
    } as unknown as NgForm;

    component.book.titel = "Test Titel";
    component.book.auteur = "Test Auteur";

    spyOn<any>(component, "toBase64").and.returnValue(
      Promise.resolve("base64data"),
    );

    component.selectedCoverFile = new File([""], "test.png");

    await component.onSubmit(mockForm);

    expect(itemService.addBoek).toHaveBeenCalled();
    expect(component.submitState).toBe("success");
    expect(component.isSaving).toBeFalse();
  });

  it("should handle service error", async () => {
    itemService.addBoek.and.returnValue(Promise.reject());

    const mockForm = {
      invalid: false,
      resetForm: jasmine.createSpy(),
    } as unknown as NgForm;

    await component.onSubmit(mockForm);

    expect(component.submitState).toBe("error");
    expect(component.submitMessage).toContain("Opslaan mislukt");
  });

  it("should handle cover selection", () => {
    const file = new File(["data"], "cover.png", { type: "image/png" });

    const event = {
      target: {
        files: [file],
      },
    } as unknown as Event;

    spyOn(URL, "createObjectURL").and.returnValue("blob:url");

    component.onCoverSelected(event);

    expect(component.selectedCoverFile).toBe(file);
    expect(component.coverPreviewUrl).toBe("blob:url");
    expect(component.book.cover).toBe("cover.png");
  });
});
