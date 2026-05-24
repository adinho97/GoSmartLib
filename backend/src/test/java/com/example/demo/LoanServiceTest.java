package com.example.demo;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.AuthService;
import com.example.demo.services.BookAvailabilityNotificationService;
import com.example.demo.services.LoanService;
import com.example.demo.services.SmartschoolMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BookAvailabilityNotificationService bookAvailabilityNotificationService;

    @Mock
    private SmartschoolMessageService smartschoolMessageService;

    @Mock
    private AuthService authService;

    @Mock
    private SmartschoolProperties smartschoolProperties;

    @Mock
    private AppUserRepository appUserRepository;

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

        ArgumentCaptor<Loan> savedLoanCaptor = ArgumentCaptor.forClass(Loan.class);
        when(loanRepository.save(savedLoanCaptor.capture())).thenReturn(savedLoan);

        // Mock de save van de bookCopy (als de service de status van de kopie aanpast
        // naar 'uitgeleend')
        ArgumentCaptor<BookCopy> savedCopyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        when(bookCopyRepository.save(savedCopyCaptor.capture())).thenReturn(mockCopy);

        // 2. Act & Assert
        assertDoesNotThrow(() -> {
            loanService.createLoan(request, null);
        });

        // Verificatie
        verify(loanRepository, times(1)).save(savedLoanCaptor.capture());
        verify(bookCopyRepository, times(1)).findByBook_Id(1L);
    }

    @Test
    void testCreateLoans_SendsOneCombinedSmartschoolMessage() {
        CreateLoanRequest request1 = new CreateLoanRequest();
        request1.setBookId(1L);
        request1.setUserSub("student-1");
        request1.setDueDate(LocalDate.of(2026, 5, 10));

        CreateLoanRequest request2 = new CreateLoanRequest();
        request2.setBookId(2L);
        request2.setUserSub("student-1");
        request2.setDueDate(LocalDate.of(2026, 5, 10));

        Book book1 = buildBook(1L, "Dune");
        Book book2 = buildBook(2L, "Foundation");

        BookCopy copy1 = buildCopy(101L, book1, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);
        BookCopy copy2 = buildCopy(102L, book2, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        when(bookCopyRepository.findByBook_Id(1L)).thenReturn(Collections.singletonList(copy1));
        when(bookCopyRepository.findByBook_Id(2L)).thenReturn(Collections.singletonList(copy2));
        ArgumentCaptor<BookCopy> batchCopyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        when(bookCopyRepository.save(batchCopyCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Loan> batchLoanCaptor = ArgumentCaptor.forClass(Loan.class);
        when(loanRepository.save(batchLoanCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        SmartschoolUserInfo userInfo = new SmartschoolUserInfo();
        userInfo.setName("Test Leerling");
        userInfo.setAccessToken("access-token");
        userInfo.setPlatform("https://school.example");

        when(authService.getUserInfoBySub("student-1")).thenReturn(Mono.just(userInfo));
        when(smartschoolMessageService.sendMessage(eq("access-token"), any(SmartschoolMessageRequest.class)))
                .thenReturn(Mono.just("ok"));

        List<LoanDto> loans = loanService.createLoans(List.of(request1, request2), null);

        assertEquals(2, loans.size());
        verify(smartschoolMessageService, times(1)).sendMessage(eq("access-token"),
                any(SmartschoolMessageRequest.class));

        ArgumentCaptor<SmartschoolMessageRequest> requestCaptor = ArgumentCaptor
                .forClass(SmartschoolMessageRequest.class);
        verify(smartschoolMessageService).sendMessage(eq("access-token"), requestCaptor.capture());

        String body = requestCaptor.getValue().getBody();
        assertTrue(body.contains("Dune"));
        assertTrue(body.contains("Foundation"));
        assertTrue(body.contains("2026-05-10"));
        assertTrue(requestCaptor.getValue().getSubject().contains("2 boeken"));
    }

    @Test
    void getConditionOverview_ComputesCopyNumbersAndKeepsOverviewData() {
        Book bookA = buildBook(1L, "Boek A");
        Book bookB = buildBook(2L, "Boek B");

        // For book A, copy numbers should follow sorted copy ids: 5 -> #1, 7 -> #2, 10
        // -> #3
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

    @Test
    void createLoan_BlocksLeerlingFromDifferentSchoolBook() {
        School schoolA = buildSchool(1L);
        School schoolB = buildSchool(2L);

        Book book = buildBook(10L, "Harry Potter");
        book.setSchool(schoolB);

        BookCopy copy = buildCopy(201L, book, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        AppUser leerling = new AppUser();
        leerling.setSub("leerling-sub");
        leerling.setRole("leerling");
        leerling.setSchool(schoolA);

        when(bookCopyRepository.findByBook_Id(10L)).thenReturn(Collections.singletonList(copy));
        when(appUserRepository.findBySub("leerling-sub")).thenReturn(java.util.Optional.of(leerling));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(10L);
        request.setUserSub("leerling-sub");
        request.setDueDate(LocalDate.now().plusDays(14));

        assertThrows(IllegalArgumentException.class, () -> loanService.createLoan(request, null));
    }

    @Test
    void createLoan_AllowsLeerlingFromSameSchoolBook() {
        School school = buildSchool(1L);

        Book book = buildBook(10L, "Harry Potter");
        book.setSchool(school);

        BookCopy copy = buildCopy(201L, book, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        AppUser leerling = new AppUser();
        leerling.setSub("leerling-sub");
        leerling.setRole("leerling");
        leerling.setSchool(school);

        when(bookCopyRepository.findByBook_Id(10L)).thenReturn(Collections.singletonList(copy));
        ArgumentCaptor<BookCopy> sameSchoolCopyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        when(bookCopyRepository.save(sameSchoolCopyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Loan> sameSchoolLoanCaptor = ArgumentCaptor.forClass(Loan.class);
        when(loanRepository.save(sameSchoolLoanCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findBySub("leerling-sub")).thenReturn(java.util.Optional.of(leerling));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(10L);
        request.setUserSub("leerling-sub");
        request.setDueDate(LocalDate.now().plusDays(14));

        assertDoesNotThrow(() -> loanService.createLoan(request, null));
        ArgumentCaptor<Loan> savedLoanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository, times(1)).save(savedLoanCaptor.capture());
    }

    @Test
    void createLoan_BlocksLeerkrachtFromDifferentSchoolBook() {
        School schoolA = buildSchool(1L);
        School schoolB = buildSchool(2L);

        Book book = buildBook(10L, "Dune");
        book.setSchool(schoolB);

        BookCopy copy = buildCopy(201L, book, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        AppUser leerkracht = new AppUser();
        leerkracht.setSub("teacher-sub");
        leerkracht.setRole("leerkracht");
        leerkracht.setSchool(schoolA);

        when(bookCopyRepository.findByBook_Id(10L)).thenReturn(Collections.singletonList(copy));
        when(appUserRepository.findBySub("teacher-sub")).thenReturn(java.util.Optional.of(leerkracht));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(10L);
        request.setUserSub("teacher-sub");
        request.setDueDate(LocalDate.now().plusDays(14));

        assertThrows(IllegalArgumentException.class, () -> loanService.createLoan(request, "teacher-sub"));
    }

    @Test
    void createLoan_BlocksBibbeheerderFromDifferentSchoolBook() {
        School schoolA = buildSchool(1L);
        School schoolB = buildSchool(2L);

        Book book = buildBook(10L, "1984");
        book.setSchool(schoolB);

        BookCopy copy = buildCopy(202L, book, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        AppUser bibbeheerder = new AppUser();
        bibbeheerder.setSub("librarian-sub");
        bibbeheerder.setRole("bibbeheerder");
        bibbeheerder.setSchool(schoolA);

        when(bookCopyRepository.findByBook_Id(10L)).thenReturn(Collections.singletonList(copy));
        when(appUserRepository.findBySub("librarian-sub")).thenReturn(java.util.Optional.of(bibbeheerder));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(10L);
        request.setUserSub("librarian-sub");
        request.setDueDate(LocalDate.now().plusDays(14));

        assertThrows(IllegalArgumentException.class, () -> loanService.createLoan(request, "librarian-sub"));
    }

    @Test
    void createLoan_AllowsLeerlingWithNoSchoolToAttemptBorrow() {
        School bookSchool = buildSchool(2L);

        Book book = buildBook(10L, "Harry Potter");
        book.setSchool(bookSchool);

        BookCopy copy = buildCopy(203L, book, BookCopy.CopyStatus.AVAILABLE, BookCopy.CopyCondition.GOOD);

        AppUser leerling = new AppUser();
        leerling.setSub("student-no-school");
        leerling.setRole("leerling");
        leerling.setSchool(null);

        when(bookCopyRepository.findByBook_Id(10L)).thenReturn(Collections.singletonList(copy));
        ArgumentCaptor<BookCopy> noSchoolCopyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        when(bookCopyRepository.save(noSchoolCopyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Loan> noSchoolLoanCaptor = ArgumentCaptor.forClass(Loan.class);
        when(loanRepository.save(noSchoolLoanCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findBySub("student-no-school")).thenReturn(java.util.Optional.of(leerling));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(10L);
        request.setUserSub("student-no-school");
        request.setDueDate(LocalDate.now().plusDays(14));

        assertDoesNotThrow(() -> loanService.createLoan(request, null));
    }

    private School buildSchool(Long id) {
        School school = new School();
        school.setId(id);
        return school;
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