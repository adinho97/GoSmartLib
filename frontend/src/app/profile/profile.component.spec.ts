import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileComponent } from './profile.component';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { By } from '@angular/platform-browser';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;

  let routerSpy = jasmine.createSpyObj('Router', ['navigate']);
  let locationSpy = jasmine.createSpyObj('Location', ['back']);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ProfileComponent],
      imports: [FormsModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle settings menu', () => {
    expect(component.settingsOpen).toBeFalse();

    component.toggleSettings();
    expect(component.settingsOpen).toBeTrue();

    component.toggleSettings();
    expect(component.settingsOpen).toBeFalse();
  });

  it('should call location.back when goBack is clicked', () => {
    component.goBack();
    expect(locationSpy.back).toHaveBeenCalled();
  });

  it('should navigate to book detail', () => {
    component.goToDetail(5);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/detail', 5]);
  });

  it('should save dashboard settings to localStorage', () => {
    spyOn(localStorage, 'setItem');

    component.saveDashboardSettings();

    expect(localStorage.setItem).toHaveBeenCalledWith(
      'dashboardSettings',
      JSON.stringify(component.dashboardSettings)
    );
    expect(component.settingsOpen).toBeFalse();
  });

  it('should load dashboard settings from localStorage on init', () => {
    const mockSettings = {
      showFavorites: false,
      showReadingHistory: false,
      showBorrowed: false,
      showHighlighted: false,
      showDeadline: false
    };

    spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(mockSettings));

    component.ngOnInit();

    expect(component.dashboardSettings.showFavorites).toBeFalse();
  });

  it('should close settings when clicking outside', () => {
    component.settingsOpen = true;

    const event = new MouseEvent('click');
    spyOn(event, 'target').and.returnValue(document.createElement('div'));

    component.clickOutside(event);

    expect(component.settingsOpen).toBeFalse();
  });

  it('should render borrowed books', () => {
    fixture.detectChanges();

    const books = fixture.debugElement.queryAll(By.css('.profile-book-card'));
    expect(books.length).toBeGreaterThan(0);
  });
});