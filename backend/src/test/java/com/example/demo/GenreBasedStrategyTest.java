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
import com.example.demo.strategies.GenreBasedStrategy;
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
@DisplayName("GenreBasedStrategy Tests")
class GenreBasedStrategyTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private GenreBasedStrategy genreBasedStrategy;

    private AppUser testUser;
    private Book fantasyBook1, fantasyBook2, fantasyBook3;
    private Book romanceBook1, romanceBook2;
    private Book scifiBook1;
    private BookCopy copy1, copy2, copy3, copy4, copy5, copy6;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setSub("user123");

        fantasyBook1 = createBook(1L, "Harry Potter", "J.K. Rowling", "Fantasy");
        fantasyBook2 = createBook(2L, "The Hobbit", "J.R.R. Tolkien", "Fantasy");
        fantasyBook3 = createBook(3L, "Percy Jackson", "Rick Riordan", "Fantasy");

        romanceBook1 = createBook(4L, "Pride and Prejudice", "Jane Austen", "Romantiek");
        romanceBook2 = createBook(5L, "Jane Eyre", "Charlotte Bronte", "Romantiek");

        scifiBook1 = createBook(6L, "Dune", "Frank Herbert", "Sciencefiction");

        copy1 = createBookCopy(1L, fantasyBook1);
        copy2 = createBookCopy(2L, fantasyBook2);
        copy3 = createBookCopy(3L, fantasyBook3);
        copy4 = createBookCopy(4L, romanceBook1);
        copy5 = createBookCopy(5L, romanceBook2);
        copy6 = createBookCopy(6L, scifiBook1);
    }

    @Test
    @DisplayName("should return empty list when user not found")
    void testRecommendWhenUserNotFound() {
        when(appUserRepository.findBySub("unknownUser")).thenReturn(Optional.empty());

        List<RecommendedBook> result = genreBasedStrategy.recommend("unknownUser", 10);

        assertTrue(result.isEmpty());
        verify(appUserRepository).findBySub("unknownUser");
    }

    @Test
    @DisplayName("should return empty list when user has no reading history")
    void testRecommendWhenNoLoans() {
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(new ArrayList<>());

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10);

        assertTrue(result.isEmpty());
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("should recommend books from same genre as reading history")
    void testRecommendWithSingleLoanSingleGenre() {
        Loan loan = createLoan(1L, copy1, "user123");
        loan.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(loan));
        when(bookRepository.findAll()).thenReturn(List.of(
                fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> "Fantasy".equals(b.getGenre())));
        assertEquals(2L, result.get(0).getBookId());
        assertEquals(3L, result.get(1).getBookId());
        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 1L));
    }

    @Test
    @DisplayName("should score books by genre frequency in reading history")
    void testRecommendWithMultipleLoansMultipleGenres() {
        Loan l1 = createLoan(1L, copy1, "user123");
        l1.setReturnedAt(LocalDate.now());
        Loan l2 = createLoan(2L, copy2, "user123");
        l2.setReturnedAt(LocalDate.now());
        Loan l3 = createLoan(3L, copy4, "user123");
        l3.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(l1, l2, l3));
        when(bookRepository.findAll()).thenReturn(List.of(
                fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        assertTrue(result.size() >= 2);
        RecommendedBook firstFantasy = result.stream()
                .filter(b -> "Fantasy".equals(b.getGenre()))
                .findFirst()
                .orElse(null);
        RecommendedBook firstRomance = result.stream()
                .filter(b -> "Romantiek".equals(b.getGenre()))
                .findFirst()
                .orElse(null);

        assertNotNull(firstFantasy);
        assertNotNull(firstRomance);
        assertTrue(firstFantasy.getScore() > firstRomance.getScore());
        assertEquals(66.67, firstFantasy.getScore(), 0.1);
        assertEquals(33.33, firstRomance.getScore(), 0.1);
    }

    @Test
    @DisplayName("should exclude books user already read when excludeRead=true")
    void testExcludeReadFiltering() {
        Loan returnedLoan = createLoan(1L, copy3, "user123"); // fantasyBook3 (id=3) was read
        returnedLoan.setReturnedAt(LocalDate.now().minusDays(5));

        List<Loan> loans = List.of(returnedLoan);

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(loans);
        when(bookRepository.findAll()).thenReturn(List.of(
                fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        // Book 3 (already read) must be excluded; books 1 and 2 match Fantasy and are not read
        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 3L));
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> "Fantasy".equals(b.getGenre())));
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
                fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, false);

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
                fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 1, true);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("should call findAllBySchool_Id when user has a school assigned")
    void testRecommendUsesSchoolScopedBooksWhenUserHasSchool() {
        School school = new School();
        school.setId(10L);
        testUser.setSchool(school);

        Book schoolFantasyBook = createBook(20L, "School Fantasy Book", "Author A", "Fantasy");
        Book schoolRomanceBook = createBook(21L, "School Romance Book", "Author B", "Romantiek");

        Loan loan = createLoan(1L, copy1, "user123"); // copy1 has fantasyBook1 (Fantasy genre)
        loan.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(loan));
        when(bookRepository.findAllBySchool_Id(10L)).thenReturn(List.of(schoolFantasyBook, schoolRomanceBook));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        verify(bookRepository).findAllBySchool_Id(10L);
        verify(bookRepository, never()).findAll();
        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getBookId()); // only the Fantasy book matches
    }

    @Test
    @DisplayName("should call findAll when user has no school assigned")
    void testRecommendUsesAllBooksWhenUserHasNoSchool() {
        // testUser has no school (setUp creates it without one)
        Loan loan = createLoan(1L, copy1, "user123"); // Fantasy
        loan.setReturnedAt(LocalDate.now());

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(loan));
        when(bookRepository.findAll()).thenReturn(List.of(fantasyBook2, romanceBook1, scifiBook1));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        verify(bookRepository).findAll();
        verify(bookRepository, never()).findAllBySchool_Id(anyLong());
        assertEquals(1, result.size()); // only fantasyBook2 matches Fantasy
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
