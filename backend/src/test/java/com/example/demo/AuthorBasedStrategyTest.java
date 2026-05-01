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
import com.example.demo.strategies.AuthorBasedStrategy;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorBasedStrategy Tests")
class AuthorBasedStrategyTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private AuthorBasedStrategy authorBasedStrategy;

    private AppUser testUser;
    private Book rowlingBook1, rowlingBook2, rowlingBook3;
    private Book tolkienBook1, tolkienBook2;
    private Book herbertBook1;
    private BookCopy copy1, copy2, copy3, copy4, copy5, copy6;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setSub("user123");

        rowlingBook1 = createBook(1L, "Harry Potter 1", "J.K. Rowling", "Fantasy");
        rowlingBook2 = createBook(2L, "Harry Potter 2", "J.K. Rowling", "Fantasy");
        rowlingBook3 = createBook(3L, "The Casual Vacancy", "J.K. Rowling", "Fiction");

        tolkienBook1 = createBook(4L, "The Hobbit", "J.R.R. Tolkien", "Fantasy");
        tolkienBook2 = createBook(5L, "The Lord of The Rings", "J.R.R. Tolkien", "Fantasy");

        herbertBook1 = createBook(6L, "Dune", "Frank Herbert", "Sciencefiction");

        copy1 = createBookCopy(1L, rowlingBook1);
        copy2 = createBookCopy(2L, rowlingBook2);
        copy3 = createBookCopy(3L, rowlingBook3);
        copy4 = createBookCopy(4L, tolkienBook1);
        copy5 = createBookCopy(5L, tolkienBook2);
        copy6 = createBookCopy(6L, herbertBook1);
    }

    @Test
    @DisplayName("should return empty list when user not found")
    void testRecommendWhenUserNotFound() {
        when(appUserRepository.findBySub("unknownUser")).thenReturn(Optional.empty());

        List<RecommendedBook> result = authorBasedStrategy.recommend("unknownUser", 10);

        assertTrue(result.isEmpty());
        verify(appUserRepository).findBySub("unknownUser");
    }

    @Test
    @DisplayName("should return empty list when user has no reading history")
    void testRecommendWhenNoLoans() {
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(new ArrayList<>());

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10);

        assertTrue(result.isEmpty());
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("should recommend books from same author as reading history")
    void testRecommendWithSingleLoanSingleAuthor() {
        Loan loan = createLoan(1L, copy1, "user123");
        loan.setReturnedAt(LocalDate.now().minusDays(5));

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(loan));
        when(bookRepository.findAll()).thenReturn(List.of(
                rowlingBook1, rowlingBook2, rowlingBook3, tolkienBook1, tolkienBook2, herbertBook1));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10, true);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> "J.K. Rowling".equals(b.getAuteur())));
        assertEquals(2L, result.get(0).getBookId());
        assertEquals(3L, result.get(1).getBookId());
        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 1L));
    }

    @Test
    @DisplayName("should score books by author frequency in reading history")
    void testRecommendWithMultipleLoansMultipleAuthors() {
        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());
        Loan l2 = createLoan(2L, copy2, "user123");
        l2.setReturnedAt(LocalDate.now());
        Loan l3 = createLoan(3L, copy4, "user123");
        l3.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1, l2, l3));
        when(bookRepository.findAll()).thenReturn(List.of(
                rowlingBook1, rowlingBook2, rowlingBook3, tolkienBook1, tolkienBook2, herbertBook1));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10, true);

        assertTrue(result.size() >= 2);
        RecommendedBook firstRowling = result.stream()
                .filter(b -> "J.K. Rowling".equals(b.getAuteur()))
                .findFirst()
                .orElse(null);
        RecommendedBook firstTolkien = result.stream()
                .filter(b -> "J.R.R. Tolkien".equals(b.getAuteur()))
                .findFirst()
                .orElse(null);

        assertNotNull(firstRowling);
        assertNotNull(firstTolkien);
        assertTrue(firstRowling.getScore() > firstTolkien.getScore());
        assertEquals(66.67, firstRowling.getScore(), 0.1);
        assertEquals(33.33, firstTolkien.getScore(), 0.1);
    }

    @Test
    @DisplayName("should exclude books user already read when excludeRead=true")
    void testExcludeReadFiltering() {
        Loan returnedLoan = createLoan(1000L, copy3, "user123");
        returnedLoan.setReturnedAt(LocalDate.now().minusDays(5));

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(returnedLoan));
        when(bookRepository.findAll()).thenReturn(List.of(
                rowlingBook3, tolkienBook1, tolkienBook2, herbertBook1));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10, true);

        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 1L || b.getBookId() == 2L || b.getBookId() == 3L));
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("should include previously read books when excludeRead=false")
    void testIncludeReadWhenExcludeReadFalse() {
        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());
        Loan l2 = createLoan(2L, copy2, "user123");
        l2.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1, l2));
        when(bookRepository.findAll()).thenReturn(List.of(
                rowlingBook1, rowlingBook2, rowlingBook3, tolkienBook1, tolkienBook2, herbertBook1));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10, false);

        assertTrue(result.stream().anyMatch(b -> b.getBookId() == 2L || b.getBookId() == 3L));
    }

    @Test
    @DisplayName("should respect limit parameter")
    void testLimitRespected() {
        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1));
        when(bookRepository.findAll()).thenReturn(List.of(
                rowlingBook1, rowlingBook2, rowlingBook3, tolkienBook1, tolkienBook2, herbertBook1));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 1, true);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("should call findAllBySchool_Id when user has a school assigned")
    void testRecommendUsesSchoolScopedBooksWhenUserHasSchool() {
        School school = new School();
        school.setId(10L);
        testUser.setSchool(school);

        // User read rowlingBook1 (J.K. Rowling); school has one more Rowling book
        Book schoolRowlingBook = createBook(20L, "Rowling School Title", "J.K. Rowling", "Fantasy");
        Book schoolTolkienBook = createBook(21L, "Tolkien School Title", "J.R.R. Tolkien", "Fantasy");

        Loan loan = createLoan(1L, copy1, "user123"); // copy1 has rowlingBook1
        loan.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(loan));
        when(bookRepository.findAllBySchool_Id(10L)).thenReturn(List.of(schoolRowlingBook, schoolTolkienBook));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10, true);

        verify(bookRepository).findAllBySchool_Id(10L);
        verify(bookRepository, never()).findAll();
        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getBookId()); // only the Rowling book matches
    }

    @Test
    @DisplayName("should call findAll when user has no school assigned")
    void testRecommendUsesAllBooksWhenUserHasNoSchool() {
        // testUser has no school (setUp creates it without one)
        Loan loan = createLoan(1L, copy1, "user123"); // J.K. Rowling
        loan.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(loan));
        when(bookRepository.findAll()).thenReturn(List.of(rowlingBook2, tolkienBook1, herbertBook1));

        List<RecommendedBook> result = authorBasedStrategy.recommend("user123", 10, true);

        verify(bookRepository).findAll();
        verify(bookRepository, never()).findAllBySchool_Id(anyLong());
        assertEquals(1, result.size()); // only rowlingBook2 matches J.K. Rowling
        assertEquals(2L, result.get(0).getBookId());
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
        return loan;
    }
}
