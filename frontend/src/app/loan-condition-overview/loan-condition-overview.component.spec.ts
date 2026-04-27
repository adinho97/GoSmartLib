import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoanConditionOverviewComponent } from './loan-condition-overview.component';
import { LoanService, LoanConditionOverview, LostCopyOverview } from '../services/loan.service';

describe('LoanConditionOverviewComponent', () => {
  let component: LoanConditionOverviewComponent;
  let fixture: ComponentFixture<LoanConditionOverviewComponent>;

  const sampleLost: LostCopyOverview = {
    copyId: 42,
    copyNumber: 3,
    bookId: 7,
    bookTitel: 'Test Boek',
    bookCover: '',
    condition: 'GOOD',
  };

  const mockOverview: LoanConditionOverview = {
    worsenedReturns: [],
    bookStates: [],
    lostCopies: [sampleLost],
  };

  const mockLoanService = {
    getConditionOverview: jasmine
      .createSpy('getConditionOverview')
      .and.returnValue(Promise.resolve(mockOverview)),
    updateCopyState: jasmine.createSpy('updateCopyState').and.returnValue(Promise.resolve()),
  } as Partial<LoanService> as LoanService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoanConditionOverviewComponent],
      providers: [{ provide: LoanService, useValue: mockLoanService }],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanConditionOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('openFoundCopyDialog sets dialog state and selected item', () => {
    component.openFoundCopyDialog(sampleLost);
    expect(component.foundCopyDialogOpen).toBeTrue();
    expect(component.foundCopyDialogItem).toBe(sampleLost);
  });

  it('confirmFoundCopyDialog calls updateCopyState and reloads overview', async () => {
    // Ensure overview reload will resolve
    (mockLoanService.getConditionOverview as jasmine.Spy).and.returnValue(Promise.resolve(mockOverview));

    component.openFoundCopyDialog(sampleLost);
    component.foundCopyCondition = 'MODERATE';

    (mockLoanService.updateCopyState as jasmine.Spy).calls.reset();
    (mockLoanService.getConditionOverview as jasmine.Spy).calls.reset();

    await component.confirmFoundCopyDialog();

    expect(mockLoanService.updateCopyState).toHaveBeenCalledWith(sampleLost.copyId, jasmine.objectContaining({ status: 'AVAILABLE', condition: 'MODERATE' }));
    expect(mockLoanService.getConditionOverview).toHaveBeenCalled();
    expect(component.foundCopyDialogOpen).toBeFalse();
  });
});
