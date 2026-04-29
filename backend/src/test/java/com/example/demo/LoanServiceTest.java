package com.example.demo;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.BookAvailabilityNotificationService;
import com.example.demo.services.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BookAvailabilityNotificationService bookAvailabilityNotificationService;

    @Mock
    private com.example.demo.config.SmartschoolMessageService smartschoolMessageService;

    @Mock
    private com.example.demo.config.AuthService authService;

    @Mock
    private com.example.demo.config.SmartschoolProperties smartschoolProperties;

    @InjectMocks
    private LoanService loanService;

    @Test
    void testCreateLoan_Success() {
        // 1. Arrange
        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(1L);
        request.setUserSub("ABCD-1234");
        request.setDueDate(LocalDate.now().plusDays(14));

        // We bouwen de keten van achter naar voren op:
        
        // A. Het Boek
        Book mockBook = new Book();
        mockBook.setId(1L);

        // B. De Kopie (moet het boek kennen!)
        BookCopy mockCopy = new BookCopy();
        mockCopy.setId(101L);
        mockCopy.setBook(mockBook); // CRUCIAAL: Voorkomt de Book.getId() NullPointer

        // C. De Lening (moet de kopie kennen!)
        Loan savedLoan = new Loan();
        savedLoan.setId(500L);
        savedLoan.setCopy(mockCopy); // CRUCIAAL: Voorkomt de BookCopy.getId() NullPointer
        savedLoan.setDueDate(request.getDueDate());
        savedLoan.setUserSub(request.getUserSub());
        savedLoan.setLoanedAt(LocalDate.now());

        // Mock de repository calls
        when(bookCopyRepository.findByBook_Id(anyLong()))
            .thenReturn(Collections.singletonList(mockCopy));
        
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);
        
        // Mock de save van de bookCopy (als de service de status van de kopie aanpast naar 'uitgeleend')
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(mockCopy);

        // 2. Act & Assert
        assertDoesNotThrow(() -> {
            loanService.createLoan(request);
        });

        // Verificatie
        verify(loanRepository, times(1)).save(any(Loan.class));
        verify(bookCopyRepository, times(1)).findByBook_Id(1L);
    }

    @Test
    void getConditionOverview_ComputesCopyNumbersAndKeepsOverviewData() {
        Book bookA = buildBook(1L, "Boek A");
        Book bookB = buildBook(2L, "Boek B");

        // For book A, copy numbers should follow sorted copy ids: 5 -> #1, 7 -> #2, 10 -> #3
        BookCopy copyA3 = buildCopy(10L, bookA, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);
        BookCopy copyA1 = buildCopy(5L, bookA, BookCopy.CopyStatus.LOST, BookCopy.CopyCondition.BAD);
        BookCopy copyA2 = buildCopy(7L, bookA, BookCopy.CopyStatus.DAMAGED, BookCopy.CopyCondition.MODERATE);

        BookCopy copyB1 = buildCopy(20L, bookB, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        when(bookCopyRepository.findAll()).thenReturn(Arrays.asList(copyA3, copyA1, copyA2, copyB1));

        Loan worsenedLoan = buildReturnedLoan(
                100L,
                copyA3,
                BookCopy.CopyCondition.GOOD,
                BookCopy.CopyCondition.BAD,
                BookCopy.CopyStatus.DAMAGED,
                LocalDate.of(2026, 4, 20));

        Loan lostLoan = buildReturnedLoan(
                101L,
                copyA2,
                BookCopy.CopyCondition.GOOD,
                BookCopy.CopyCondition.MODERATE,
                BookCopy.CopyStatus.LOST,
                LocalDate.of(2026, 4, 22));

        when(loanRepository.findByReturnedAtIsNotNull()).thenReturn(Arrays.asList(worsenedLoan, lostLoan));

        LoanConditionOverviewDto overview = loanService.getConditionOverview();

        assertEquals(2, overview.getWorsenedReturns().size());

        // Sorted by returnedAt descending, so lostLoan entry comes first.
        LoanConditionOverviewDto.WorsenedReturnDto first = overview.getWorsenedReturns().get(0);
        assertEquals(101L, first.getLoanId());
        assertEquals(2, first.getCopyNumber());

        LoanConditionOverviewDto.WorsenedReturnDto second = overview.getWorsenedReturns().get(1);
        assertEquals(100L, second.getLoanId());
        assertEquals(3, second.getCopyNumber());

        assertEquals(1, overview.getLostCopies().size());
        LoanConditionOverviewDto.LostCopyDto lostCopy = overview.getLostCopies().get(0);
        assertEquals(5L, lostCopy.getCopyId());
        assertEquals(1, lostCopy.getCopyNumber());

        LoanConditionOverviewDto.BookStateDto bookAState = overview.getBookStates().stream()
                .filter(state -> state.getBookId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertEquals(3, bookAState.getTotalCopies());
        assertEquals(1, bookAState.getAvailableCopies());
        assertEquals(1, bookAState.getDamagedCopies());
        assertEquals(1, bookAState.getLostCopies());
        assertEquals(1, bookAState.getGoodConditionCopies());
        assertEquals(1, bookAState.getModerateConditionCopies());
        assertEquals(1, bookAState.getBadConditionCopies());
    }

    @Test
    void getConditionOverview_ReadsCopiesOnce() {
        Book book = buildBook(1L, "Boek");
        BookCopy copy = buildCopy(1L, book, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        when(bookCopyRepository.findAll()).thenReturn(List.of(copy));
        when(loanRepository.findByReturnedAtIsNotNull()).thenReturn(Collections.emptyList());

        loanService.getConditionOverview();

        verify(bookCopyRepository, times(1)).findAll();
        verify(loanRepository, times(1)).findByReturnedAtIsNotNull();
    }

    @Test
    void getAllActiveLoansReturnsDtosForOpenLoans() {
        Book book = buildBook(1L, "Dune");
        BookCopy copy = buildCopy(11L, book, BookCopy.CopyStatus.LOANED, BookCopy.CopyCondition.GOOD);

        Loan activeLoan = new Loan();
        activeLoan.setId(99L);
        activeLoan.setCopy(copy);
        activeLoan.setUserSub("student-1");
        activeLoan.setLoanedAt(LocalDate.of(2026, 4, 10));
        activeLoan.setDueDate(LocalDate.of(2026, 4, 24));

        when(loanRepository.findByReturnedAtIsNull()).thenReturn(List.of(activeLoan));

        List<LoanDto> result = loanService.getAllActiveLoans();

        assertEquals(1, result.size());
        LoanDto dto = result.get(0);
        assertEquals(99L, dto.getId());
        assertEquals(11L, dto.getCopyId());
        assertEquals(1L, dto.getBookId());
        assertEquals("Dune", dto.getBookTitel());
        assertEquals("student-1", dto.getUserSub());
        assertEquals(LocalDate.of(2026, 4, 10), dto.getLoanedAt());
        assertEquals(LocalDate.of(2026, 4, 24), dto.getDueDate());

        verify(loanRepository, times(1)).findByReturnedAtIsNull();
    }

    private Book buildBook(Long id, String title) {
        Book book = new Book();
        book.setId(id);
        book.setTitel(title);
        book.setCover("cover");
        return book;
    }

    private BookCopy buildCopy(
            Long id,
            Book book,
            BookCopy.CopyStatus status,
            BookCopy.CopyCondition condition) {
        BookCopy copy = new BookCopy();
        copy.setId(id);
        copy.setBook(book);
        copy.setStatus(status);
        copy.setCondition(condition);
        return copy;
    }

    private Loan buildReturnedLoan(
            Long id,
            BookCopy copy,
            BookCopy.CopyCondition loanedCondition,
            BookCopy.CopyCondition returnedCondition,
            BookCopy.CopyStatus returnedStatus,
            LocalDate returnedAt) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setCopy(copy);
        loan.setUserSub("sub");
        loan.setLoanedAt(returnedAt.minusDays(7));
        loan.setDueDate(returnedAt.plusDays(7));
        loan.setReturnedAt(returnedAt);
        loan.setLoanedCondition(loanedCondition);
        loan.setReturnedCondition(returnedCondition);
        loan.setReturnedStatus(returnedStatus);
        return loan;
    }
}