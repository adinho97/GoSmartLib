package com.example.demo;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.strategies.TrendingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendingStrategy Tests")
class TrendingStrategyTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private TrendingStrategy trendingStrategy;

    private AppUser testUser;
    private Book book1, book2, book3, book4, book5;
    private BookCopy copy1, copy2, copy3, copy4, copy5;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setSub("user123");
        testUser.setRole("leerling");

        book1 = createBook(1L, "The Hobbit", "J.R.R. Tolkien", "Fantasy");
        book2 = createBook(2L, "1984", "George Orwell", "Dystopie");
        book3 = createBook(3L, "Harry Potter", "J.K. Rowling", "Fantasy");
        book4 = createBook(4L, "To Kill a Mockingbird", "Harper Lee", "Literaire roman");
        book5 = createBook(5L, "Pride and Prejudice", "Jane Austen", "Romantiek");

        copy1 = createBookCopy(1L, book1);
        copy2 = createBookCopy(2L, book2);
        copy3 = createBookCopy(3L, book3);
        copy4 = createBookCopy(4L, book4);
        copy5 = createBookCopy(5L, book5);
    }

    @Test
    @DisplayName("should return empty list when user not found")
    void testRecommendWhenUserNotFound() {
        when(appUserRepository.findBySub("unknownUser")).thenReturn(Optional.empty());

        List<RecommendedBook> result = trendingStrategy.recommend("unknownUser", 10);

        assertTrue(result.isEmpty());
        verify(appUserRepository).findBySub("unknownUser");
    }

    @Test
    @DisplayName("should return empty list when no loans exist in system")
    void testRecommendWhenNoLoansInSystem() {
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(new ArrayList<>());
        when(loanRepository.findAll()).thenReturn(new ArrayList<>());

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10);

        assertTrue(result.isEmpty());
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("should recommend books based on popularity when user has reading history")
    void testRecommendWithReadingHistory() {
        List<Loan> allLoans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            allLoans.add(createLoan(Long.valueOf(100 + i), copy2, "otherUser" + i));
        }
        for (int i = 0; i < 3; i++) {
            allLoans.add(createLoan(Long.valueOf(200 + i), copy3, "otherUser" + i));
        }
        allLoans.add(createLoan(105L, copy4, "otherUser"));

        Loan userHistory = createLoan(1L, copy1, "user123");
        userHistory.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(userHistory));
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3, book4, book5));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10, true);

        assertEquals(3, result.size());
        assertEquals(2L, result.get(0).getBookId()); // book2 with 5 loans (score 100)
        assertEquals(3L, result.get(1).getBookId()); // book3 with 3 loans (score 60)
        assertEquals(4L, result.get(2).getBookId()); // book4 with 1 loan (score 20)

        assertTrue(result.get(0).getScore() > result.get(1).getScore());
        assertTrue(result.get(1).getScore() > result.get(2).getScore());
    }

    @Test
    @DisplayName("should properly score books based on relative loan frequency from reading history")
    void testRecommendWithMultipleLoans() {
        List<Loan> allLoans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            allLoans.add(createLoan(Long.valueOf(300 + i), copy3, "user" + i));
        }
        for (int i = 0; i < 5; i++) {
            allLoans.add(createLoan(Long.valueOf(400 + i), copy4, "user" + i));
        }

        allLoans.add(createLoan(500L, copy5, "user1"));

        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());
        Loan l2 = createLoan(2L, copy2, "user123");
        l2.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1, l2));
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3, book4, book5));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10, true);

        assertEquals(3, result.size());
        assertEquals(100.0, result.get(0).getScore(), 0.01); // book3: 10/10 * 100
        assertEquals(50.0, result.get(1).getScore(), 0.01); // book4: 5/10 * 100
        assertEquals(10.0, result.get(2).getScore(), 0.01); // book5: 1/10 * 100
    }

    @Test
    @DisplayName("should exclude books user has already read when excludeRead=true")
    void testExcludeReadFiltering() {
        Loan returnedLoan = createLoan(1L, copy2, "user123");
        returnedLoan.setReturnedAt(LocalDate.now().minusDays(10));

        List<Loan> userLoans = List.of(returnedLoan);
        List<Loan> allLoans = createLoansForBooks();

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(userLoans);
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3, book4, book5));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10, true);

        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 1L || b.getBookId() == 2L));
    }

    @Test
    @DisplayName("should include books user has already read when excludeRead=false")
    void testIncludeReadWhenExcludeReadFalse() {
        Loan returnedLoan = createLoan(1L, copy2, "user123");
        returnedLoan.setReturnedAt(LocalDate.now().minusDays(10));

        List<Loan> allLoans = createLoansForBooks();

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3, book4, book5));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10, false);

        assertTrue(result.stream().anyMatch(b -> b.getBookId() == 2L));
    }

    @Test
    @DisplayName("should respect limit parameter and return max N books")
    void testLimitRespectsMaxBooks() {
        List<Loan> allLoans = createLoansForBooks();
        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1));
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3, book4, book5));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 2, true);

        assertTrue(result.size() <= 2);
    }

    @Test
    @DisplayName("should return zero books when limit is zero")
    void testLimitZero() {
        List<Loan> allLoans = createLoansForBooks();
        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1));
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3, book4, book5));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 0, true);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("should call findAllBySchool_Id when user has a school assigned")
    void testRecommendUsesSchoolScopedBooksWhenUserHasSchool() {
        School school = new School();
        school.setId(7L);
        testUser.setSchool(school);

        Book schoolBook = createBook(20L, "Trending School Book", "Author", "Fantasy");
        BookCopy schoolCopy = createBookCopy(20L, schoolBook);

        // System-wide loan on the school book to make it trending
        Loan systemLoan = createLoan(500L, schoolCopy, "other-user");

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findAll()).thenReturn(List.of(systemLoan));
        when(bookRepository.findAllBySchool_Id(7L)).thenReturn(List.of(schoolBook));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10, false);

        verify(bookRepository).findAllBySchool_Id(7L);
        verify(bookRepository, never()).findAll();
        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getBookId());
        assertEquals(100.0, result.get(0).getScore(), 0.01);
    }

    @Test
    @DisplayName("should call findAll when user has no school assigned")
    void testRecommendUsesAllBooksWhenUserHasNoSchool() {
        // testUser has no school (setUp creates it without one)
        Book sysBook = createBook(10L, "System Wide Book", "Author", "Fantasy");
        BookCopy sysCopy = createBookCopy(10L, sysBook);
        Loan systemLoan = createLoan(600L, sysCopy, "other-user");

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findAll()).thenReturn(List.of(systemLoan));
        when(bookRepository.findAll()).thenReturn(List.of(sysBook));

        List<RecommendedBook> result = trendingStrategy.recommend("user123", 10, false);

        verify(bookRepository).findAll();
        verify(bookRepository, never()).findAllBySchool_Id(anyLong());
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getBookId());
    }

    private Book createBook(Long id, String titel, String auteur, String genre) {
        Book book = new Book();
        book.setId(id);
        book.setTitel(titel);
        book.setAuteur(auteur);
        book.setGenre(genre);
        return book;
    }

    private BookCopy createBookCopy(Long id, Book book) {
        BookCopy copy = new BookCopy();
        copy.setId(id);
        copy.setBook(book);
        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
        return copy;
    }

    private Loan createLoan(Long id, BookCopy copy, String userSub) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setCopy(copy);
        loan.setUserSub(userSub);
        loan.setLoanedAt(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(7));
        loan.setReturnedAt(LocalDate.now().minusDays(1)); // Returned
        return loan;
    }

    private List<Loan> createLoansForBooks() {
        List<Loan> loans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            loans.add(createLoan(Long.valueOf(2000 + i), copy2, "user" + i));
        }
        for (int i = 0; i < 3; i++) {
            loans.add(createLoan(Long.valueOf(3000 + i), copy3, "user" + i));
        }
        loans.add(createLoan(4000L, copy4, "user0"));
        for (int i = 0; i < 2; i++) {
            loans.add(createLoan(Long.valueOf(5000 + i), copy5, "user" + i));
        }
        return loans;
    }
}
