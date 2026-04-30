import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { MapScreenComponent } from "./map-screen.component";
import { SchoolService } from "../services/school.service";
import { Router } from "@angular/router";
import { of, throwError } from "rxjs";
import { FormsModule } from "@angular/forms";
import * as L from "leaflet";

describe("MapScreenComponent", () => {
  let component: MapScreenComponent;
  let fixture: ComponentFixture<MapScreenComponent>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockSchools = [
    {
      id: 1,
      naam: "School 1",
      adres: "Adres 1",
      latitude: 51.2198,
      longitude: 4.4181,
    },
    {
      id: 2,
      naam: "School 2",
      adres: "Adres 2",
      latitude: 51.2268,
      longitude: 4.4199,
    },
  ];

  beforeEach(async () => {
    schoolServiceSpy = jasmine.createSpyObj("SchoolService", [
      "getPagedSchools",
      "setSelectedSchoolId",
    ]);
    routerSpy = jasmine.createSpyObj("Router", ["navigate"]);

    // Default mock response for pagination
    schoolServiceSpy.getPagedSchools.and.returnValue(
      of({ items: mockSchools, total: 2 }),
    );

    await TestBed.configureTestingModule({
      imports: [MapScreenComponent, FormsModule],
      providers: [
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MapScreenComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("should initialize the Leaflet map in ngAfterViewInit", fakeAsync(() => {
    fixture.detectChanges();
    tick(100); // Wait for the invalidateSize timeout in initMap
    expect(component.map).toBeDefined();
    expect(component.map instanceof L.Map).toBeTrue();
  }));

  it("should load schools and populate markers on init", () => {
    fixture.detectChanges();
    expect(schoolServiceSpy.getPagedSchools).toHaveBeenCalledWith(0, 10, "");
    expect(component.schools.length).toBe(2);
    expect(component.totalSchools).toBe(2);
  });

  it("should handle errors when loading schools fails", () => {
    schoolServiceSpy.getPagedSchools.and.returnValue(
      throwError(() => new Error("Network error")),
    );
    spyOn(console, "error");
    fixture.detectChanges();
    expect(component.isLoading).toBeFalse();
    expect(console.error).toHaveBeenCalled();
  });

  it("should reset to page 0 and reload when onSearch is called", () => {
    fixture.detectChanges();
    component.searchQuery = "Antwerpen";
    component.onSearch();
    expect(component.currentPage).toBe(0);
    expect(schoolServiceSpy.getPagedSchools).toHaveBeenCalledWith(
      0,
      10,
      "Antwerpen",
    );
  });

  it("should fetch new data when navigating pages", () => {
    fixture.detectChanges();
    component.goToPage(1);
    expect(component.currentPage).toBe(1);
    expect(schoolServiceSpy.getPagedSchools).toHaveBeenCalledWith(1, 10, "");
  });

  it("should zoom the map and open popup when zoomToSchool is called", () => {
    fixture.detectChanges();
    const flyToSpy = spyOn(component.map, "flyTo").and.callThrough();
    const targetSchool = mockSchools[0];

    component.zoomToSchool(targetSchool);

    expect(component.selectedSchool).toBe(targetSchool);
    expect(flyToSpy).toHaveBeenCalled();
  });

  it("should destroy the map when component is destroyed", () => {
    fixture.detectChanges();
    const removeSpy = spyOn(component.map, "remove").and.callThrough();
    component.ngOnDestroy();
    expect(removeSpy).toHaveBeenCalled();
  });
});
