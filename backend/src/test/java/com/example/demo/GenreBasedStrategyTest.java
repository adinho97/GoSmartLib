package com.example.demo;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Favorite;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.FavoriteRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenreBasedStrategy Tests")
class GenreBasedStrategyTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

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
        verify(favoriteRepository, never()).findByUser(any());
    }

    @Test
    @DisplayName("should return empty list when user has no favorites")
    void testRecommendWhenNoFavorites() {
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUser(testUser)).thenReturn(new ArrayList<>());

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10);

        assertTrue(result.isEmpty());
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("should recommend books from same genre as favorite")
    void testRecommendWithSingleFavoriteSingleGenre() {
        Favorite fav = createFavorite(1L, testUser, fantasyBook1);
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUser(testUser)).thenReturn(List.of(fav));
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(new ArrayList<>());
        when(bookRepository.findAll()).thenReturn(List.of(
            fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1
        ));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> "Fantasy".equals(b.getGenre())));
        assertEquals(2L, result.get(0).getBookId());
        assertEquals(3L, result.get(1).getBookId());
        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 1L));
    }

    @Test
    @DisplayName("should score books by genre frequency in favorites")
    void testRecommendWithMultipleFavoritesMultipleGenres() {
        List<Favorite> favorites = List.of(
            createFavorite(1L, testUser, fantasyBook1),
            createFavorite(2L, testUser, fantasyBook2),
            createFavorite(3L, testUser, romanceBook1)
        );
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUser(testUser)).thenReturn(favorites);
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(new ArrayList<>());
        when(bookRepository.findAll()).thenReturn(List.of(
            fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1
        ));

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
    @DisplayName("should exclude books user already favorited when excludeRead=true")
    void testExcludeReadFiltering() {
        Favorite fav1 = createFavorite(1L, testUser, fantasyBook1);
        Favorite fav2 = createFavorite(2L, testUser, fantasyBook2);
        List<Favorite> favorites = List.of(fav1, fav2);

        Loan returnedLoan = createLoan(1000L, copy3, "user123");
        returnedLoan.setReturnedAt(LocalDate.now().minusDays(5));

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUser(testUser)).thenReturn(favorites);
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(List.of(returnedLoan));
        when(bookRepository.findAll()).thenReturn(List.of(
            fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1
        ));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, true);

        assertTrue(result.stream().noneMatch(b -> b.getBookId() == 1L || b.getBookId() == 2L || b.getBookId() == 3L));
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("should include previously read books when excludeRead=false")
    void testIncludeReadWhenExcludeReadFalse() {
        Favorite fav = createFavorite(1L, testUser, fantasyBook1);
        List<Favorite> favorites = List.of(fav);

        Loan returnedLoan = createLoan(1000L, copy2, "user123");
        returnedLoan.setReturnedAt(LocalDate.now().minusDays(5));

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUser(testUser)).thenReturn(favorites);
        when(bookRepository.findAll()).thenReturn(List.of(
            fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1
        ));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 10, false);

        assertTrue(result.stream().anyMatch(b -> b.getBookId() == 2L || b.getBookId() == 3L));
    }

    @Test
    @DisplayName("should respect limit parameter")
    void testLimitRespected() {
        List<Favorite> favorites = List.of(
            createFavorite(1L, testUser, fantasyBook1),
            createFavorite(2L, testUser, fantasyBook2)
        );
        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUser(testUser)).thenReturn(favorites);
        when(loanRepository.findByUserSubAndReturnedAtIsNotNull("user123")).thenReturn(new ArrayList<>());
        when(bookRepository.findAll()).thenReturn(List.of(
            fantasyBook1, fantasyBook2, fantasyBook3, romanceBook1, romanceBook2, scifiBook1
        ));

        List<RecommendedBook> result = genreBasedStrategy.recommend("user123", 1, true);

        assertEquals(1, result.size());
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

    private Favorite createFavorite(Long id, AppUser user, Book book) {
        Favorite fav = new Favorite();
        fav.setId(id);
        fav.setUser(user);
        fav.setBook(book);
        return fav;
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
