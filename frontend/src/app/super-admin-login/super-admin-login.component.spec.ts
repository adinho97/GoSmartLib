import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SuperAdminLoginComponent } from './super-admin-login.component';
import { SuperAdminAuthService } from '../services/super-admin-auth.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('SuperAdminLoginComponent', () => {
  let component: SuperAdminLoginComponent;
  let fixture: ComponentFixture<SuperAdminLoginComponent>;
  let authService: SuperAdminAuthService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SuperAdminLoginComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [SuperAdminAuthService]
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdminLoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(SuperAdminAuthService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // TODO: Add tests
});
