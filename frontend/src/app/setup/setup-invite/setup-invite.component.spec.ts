import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetupInviteComponent } from './setup-invite.component';

describe('SetupInviteComponent', () => {
  let component: SetupInviteComponent;
  let fixture: ComponentFixture<SetupInviteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SetupInviteComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SetupInviteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
