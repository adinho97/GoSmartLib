package com.example.demo;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.entities.Genre;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.strategies.NewArrivalsStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewArrivalsStrategy Tests")
class NewArrivalsStrategyTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private NewArrivalsStrategy newArrivalsStrategy;

    private Book book1, book2, book3, book4, book5;

    @BeforeEach
    void setUp() {
        book1 = createBook(1L, "Book 1", "Author 1", "Genre 1");
        book2 = createBook(2L, "Book 2", "Author 2", "Genre 2");
        book3 = createBook(3L, "Book 3", "Author 3", "Genre 3");
        book4 = createBook(4L, "Book 4", "Author 4", "Genre 4");
        book5 = createBook(5L, "Book 5", "Author 5", "Genre 5");
    }

    @Test
    @DisplayName("should return books sorted by ID descending (newest first)")
    void testRecommendReturnsBooksNewestFirst() {
        List<Book> books = List.of(book1, book2, book3, book4, book5);
        when(bookRepository.findAll()).thenReturn(books);

        List<RecommendedBook> result = newArrivalsStrategy.recommend("user123", 10);

        assertEquals(5, result.size());
        assertEquals(5L, result.get(0).getBookId());
        assertEquals(4L, result.get(1).getBookId());
        assertEquals(3L, result.get(2).getBookId());
        assertEquals(2L, result.get(3).getBookId());
        assertEquals(1L, result.get(4).getBookId());
    }

    @Test
    @DisplayName("should respect limit parameter")
    void testRecommendRespectsLimit() {
        List<Book> books = List.of(book1, book2, book3, book4, book5);
        when(bookRepository.findAll()).thenReturn(books);

        List<RecommendedBook> result = newArrivalsStrategy.recommend("user123", 2);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).getBookId());
        assertEquals(4L, result.get(1).getBookId());
    }

    @Test
    @DisplayName("should assign max score (100.0) to all books")
    void testRecommendAssignsMaxScore() {
        List<Book> books = List.of(book1, book2, book3);
        when(bookRepository.findAll()).thenReturn(books);

        List<RecommendedBook> result = newArrivalsStrategy.recommend("user123", 10);

        assertTrue(result.stream().allMatch(b -> b.getScore() == 100.0));
    }

    @Test
    @DisplayName("should work the same regardless of excludeRead parameter")
    void testRecommendIgnoresExcludeReadFlag() {
        List<Book> books = List.of(book1, book2, book3);
        when(bookRepository.findAll()).thenReturn(books);

        List<RecommendedBook> resultExclude = newArrivalsStrategy.recommend("user123", 10, true);
        List<RecommendedBook> resultInclude = newArrivalsStrategy.recommend("user123", 10, false);

        assertEquals(resultExclude.size(), resultInclude.size());
        for (int i = 0; i < resultExclude.size(); i++) {
            assertEquals(resultExclude.get(i).getBookId(), resultInclude.get(i).getBookId());
        }
    }

    @Test
    @DisplayName("should call findAllBySchool_Id when user has a school assigned")
    void testRecommendUsesSchoolScopedBooksWhenUserHasSchool() {
        School school = new School();
        school.setId(5L);

        AppUser user = new AppUser();
        user.setSub("user123");
        user.setSchool(school);

        Book schoolBook1 = createBook(10L, "School Book 1", "Author A", "Fantasy");
        Book schoolBook2 = createBook(11L, "School Book 2", "Author B", "Fantasy");

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(user));
        when(bookRepository.findAllBySchool_Id(5L)).thenReturn(List.of(schoolBook1, schoolBook2));

        List<RecommendedBook> result = newArrivalsStrategy.recommend("user123", 10);

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).getBookId()); // sorted by ID desc
        assertEquals(10L, result.get(1).getBookId());
        verify(bookRepository).findAllBySchool_Id(5L);
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("should fall back to findAll when user has no school assigned")
    void testRecommendUsesAllBooksWhenUserHasNoSchool() {
        AppUser user = new AppUser();
        user.setSub("user123");
        // no school set

        when(appUserRepository.findBySub("user123")).thenReturn(Optional.of(user));
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3));

        List<RecommendedBook> result = newArrivalsStrategy.recommend("user123", 10);

        assertEquals(3, result.size());
        assertEquals(3L, result.get(0).getBookId()); // sorted by ID desc
        verify(bookRepository).findAll();
        verify(bookRepository, never()).findAllBySchool_Id(anyLong());
    }

    private Book createBook(Long id, String titel, String auteur, String genre) {
        Book book = new Book();
        book.setId(id);
        book.setTitel(titel);
        book.setAuteur(auteur);
        // Create a Genre object and add it to a Set
        Genre newGenre = new Genre();
        newGenre.setNaam(genre);
        book.setGenres(Set.of(newGenre));
        return book;
    }
}
