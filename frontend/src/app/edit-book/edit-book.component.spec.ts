import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { EditBookComponent } from './edit-book.component';
import { BookService } from '../services/book.service';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoanService } from '../services/loan.service';
import { AdminGenreService } from '../services/admin-genre.service';

describe('EditBookComponent', () => {
  let component: EditBookComponent;
  let fixture: ComponentFixture<EditBookComponent>;
  let bookServiceSpy: any;
  let router: Router;

  const mockBook = {
    id: 1,
    titel: 'Test Boek',
    auteur: 'Test Auteur',
    genre: 'Fictie',
    beschrijving: 'Lorum Ipsum',
    taal: 'Nederlands',
    paginas: 200,
    uitgaveDatum: '2023-01-01',
    uitgeverij: 'Test Uitgever',
    cover: ''
  };

  beforeEach(async () => {
    // Maak een spy voor de BookService
    bookServiceSpy = jasmine.createSpyObj('BookService', ['getBookById', 'updateBook']);
    bookServiceSpy.getBookById.and.returnValue(of(mockBook));
    bookServiceSpy.updateBook.and.returnValue(Promise.resolve());

    const loanServiceSpy = jasmine.createSpyObj('LoanService', ['getLoansByBookId']);
    loanServiceSpy.getLoansByBookId.and.returnValue(Promise.resolve([]));

    const adminGenreServiceSpy = jasmine.createSpyObj('AdminGenreService', ['getGenres', 'getAll']);
    adminGenreServiceSpy.getGenres.and.returnValue(Promise.resolve([]));
    adminGenreServiceSpy.getAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      // EditBookComponent importeren (ervan uitgaande dat het een standalone component is)
      imports: [
        FormsModule, 
        RouterTestingModule, 
        HttpClientTestingModule,
        EditBookComponent
      ],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
        { provide: AdminGenreService, useValue: adminGenreServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => '1' } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditBookComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);

    // CRUCIAL: Voorkom de 'Cannot match any routes' error door navigate te onderscheppen
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture.detectChanges();
  });

  it('zou het component moeten aanmaken', () => {
    expect(component).toBeTruthy();
  });

  it('zou het boek moeten laden bij initialisatie', () => {
    expect(bookServiceSpy.getBookById).toHaveBeenCalledWith(1);
    expect(component.book.titel).toBe('Test Boek');
  });

  it('zou de updateBook methode moeten aanroepen bij onSubmit', fakeAsync(() => {
    component.onSubmit();
    tick(); // Wacht op de async afhandeling van de promise

    expect(bookServiceSpy.updateBook).toHaveBeenCalled();
  }));

  it('zou naar /books moeten navigeren na een succesvolle update', fakeAsync(() => {
    component.onSubmit();
    tick();

    // We checken of de aanroep is gedaan, zonder dat de router echt op zoek gaat naar de route
    expect(router.navigate).toHaveBeenCalledWith(['/books']);
  }));

  it('zou terug moeten navigeren naar /books bij cancel()', () => {
    component.cancel();
    expect(router.navigate).toHaveBeenCalledWith(['/books']);
  });

  it('zou een foutmelding moeten tonen als het laden van het boek mislukt', () => {
    // Simuleer een API fout
    bookServiceSpy.getBookById.and.returnValue(throwError(() => new Error('API Error')));
    
    // Roep de laad-methode opnieuw aan
    component.loadBook();
    
    expect(component.errorMessage).toBe('Kon het boek niet laden.');
  });
});