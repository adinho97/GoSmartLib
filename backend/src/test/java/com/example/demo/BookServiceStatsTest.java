package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.GenreRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.BookService;
import com.example.demo.mappers.BookMapper;
import com.example.demo.services.ImportCoreService;
import com.example.demo.services.IsbnService;
import com.example.demo.services.OpenLibraryService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookServiceStatsTest {

    private BookRepository bookRepository;
    private LoanRepository loanRepository;
    private BookService bookService;
    private GenreRepository genreRepository;
    private IsbnService isbnService;
    private BookMapper bookMapper;
    private ImportCoreService importCoreService;
    private SchoolService schoolService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        loanRepository = mock(LoanRepository.class);
        BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
        genreRepository = mock(GenreRepository.class);
        isbnService = mock(IsbnService.class);
        bookMapper = mock(BookMapper.class);
        importCoreService = mock(ImportCoreService.class);
        schoolService = mock(SchoolService.class);

        bookService = new BookService(
                bookRepository,
                bookCopyRepository,
                loanRepository,
                schoolService,
                new OpenLibraryService(genreRepository, isbnService),
                isbnService,
                null,
                bookMapper,
                importCoreService);
    }

    @Test
    void getBooksWithStatsShouldReturnBooksWithLoanCountsSortedDescending() {
        Book dune = new Book();
        dune.setId(1L);
        dune.setTitel("Dune");
        dune.setAuteur("Frank Herbert");

        Book foundation = new Book();
        foundation.setId(2L);
        foundation.setTitel("Foundation");
        foundation.setAuteur("Isaac Asimov");

        Map<String, Object> duneStats = new HashMap<>();
        duneStats.put("bookId", 1L);
        duneStats.put("loanCount", 15L);

        Map<String, Object> foundationStats = new HashMap<>();
        foundationStats.put("bookId", 2L);
        foundationStats.put("loanCount", 10L);

        when(bookRepository.findAll()).thenReturn(List.of(dune, foundation));
        when(loanRepository.getLoanCountsByBook()).thenReturn(List.of(duneStats, foundationStats));

        List<BookDto> result = bookService.getBooksWithStats();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(15L, result.get(0).getLoanCount());
        assertEquals(2L, result.get(1).getId());
        assertEquals(10L, result.get(1).getLoanCount());
    }

    @Test
    void getBooksWithStatsShouldSetZeroWhenBookHasNoLoans() {
        Book newBook = new Book();
        newBook.setId(5L);
        newBook.setTitel("New Book");
        newBook.setAuteur("Unknown");

        when(bookRepository.findAll()).thenReturn(List.of(newBook));
        when(loanRepository.getLoanCountsByBook()).thenReturn(List.of());

        List<BookDto> result = bookService.getBooksWithStats();

        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getLoanCount());
    }

    @Test
    void getBooksWithStatsShouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(bookRepository.findAll()).thenReturn(List.of());
        when(loanRepository.getLoanCountsByBook()).thenReturn(List.of());

        List<BookDto> result = bookService.getBooksWithStats();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getBooksWithStatsShouldHandleNumericLoanCountsFromDifferentNumberTypes() {
        Book book = new Book();
        book.setId(3L);
        book.setTitel("Numeric Book");

        Map<String, Object> stats = new HashMap<>();
        stats.put("bookId", Integer.valueOf(3));
        stats.put("loanCount", Integer.valueOf(7));

        when(bookRepository.findAll()).thenReturn(List.of(book));
        when(loanRepository.getLoanCountsByBook()).thenReturn(List.of(stats));

        List<BookDto> result = bookService.getBooksWithStats();

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getLoanCount());
    }
}
