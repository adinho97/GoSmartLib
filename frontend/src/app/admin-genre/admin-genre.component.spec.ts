import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { of } from "rxjs";

import { AdminGenreComponent } from "./admin-genre.component";
import { AdminGenreService } from "../services/admin-genre.service";

class MockAdminGenreService {
  getAll = jasmine.createSpy("getAll").and.returnValue(of([]));
  create = jasmine
    .createSpy("create")
    .and.returnValue(of({ id: 1, naam: "New Genre", subgenres: [] }));
  update = jasmine
    .createSpy("update")
    .and.returnValue(of({ id: 1, naam: "Updated Genre", subgenres: [] }));
  delete = jasmine.createSpy("delete").and.returnValue(of(undefined));
  createSubgenre = jasmine
    .createSpy("createSubgenre")
    .and.returnValue(
      of({ id: 1, naam: "Parent", subgenres: [{ id: 101, naam: "New Sub" }] }),
    );
  updateSubgenre = jasmine.createSpy("updateSubgenre").and.returnValue(
    of({
      id: 1,
      naam: "Parent",
      subgenres: [{ id: 101, naam: "Updated Sub" }],
    }),
  );
  deleteSubgenre = jasmine
    .createSpy("deleteSubgenre")
    .and.returnValue(of(undefined));
}

describe("AdminGenreComponent", () => {
  let component: AdminGenreComponent;
  let fixture: ComponentFixture<AdminGenreComponent>;
  let mockAdminGenreService: MockAdminGenreService;

  beforeEach(async () => {
    mockAdminGenreService = new MockAdminGenreService();
    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [AdminGenreComponent],
      providers: [
        { provide: AdminGenreService, useValue: mockAdminGenreService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminGenreComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
