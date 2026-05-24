package com.example.demo;

import com.example.demo.services.AuthService;
import com.example.demo.controllers.BookController;
import com.example.demo.entities.Book;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.GenreRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.BookService;
import com.example.demo.mappers.BookMapper;
import com.example.demo.services.ImportCoreService;
import com.example.demo.services.IsbnService;
import com.example.demo.services.OpenLibraryService;
import com.example.demo.services.ReviewModerationService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookStatsControllerStandaloneTest {

    private MockMvc mockMvc;
    private BookRepository bookRepository;
    private LoanRepository loanRepository;
    private GenreRepository genreRepository;
    private IsbnService isbnService;
    private BookMapper bookMapper;
    private ImportCoreService importCoreService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        loanRepository = mock(LoanRepository.class);
        BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
        genreRepository = mock(GenreRepository.class);
        isbnService = mock(IsbnService.class);
        bookMapper = mock(BookMapper.class);
        importCoreService = mock(ImportCoreService.class);

        BookService bookService = new BookService(
                bookRepository,
                bookCopyRepository,
                loanRepository,
                null,
                new OpenLibraryService(genreRepository, isbnService),
                isbnService,
                null,
                bookMapper, importCoreService);

        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);

        BookController controller = new BookController(
                bookRepository,
                reviewRepository,
                appUserRepository,
                bookService,
                (SchoolService) null,
                mock(ReviewModerationService.class), // Mock this if needed
                (AuthService) null,
                bookMapper); // Use the mockBookMapper at the correct parameter position

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStatsShouldReturnBooksSortedByPopularity() throws Exception {
        Book dune = new Book();
        dune.setId(1L);
        dune.setTitel("Dune");
        dune.setAuteur("Frank Herbert");

        Book steelheart = new Book();
        steelheart.setId(2L);
        steelheart.setTitel("Steelheart");
        steelheart.setAuteur("Brandon Sanderson");

        Map<String, Object> duneStats = new HashMap<>();
        duneStats.put("bookId", 1L);
        duneStats.put("loanCount", 10L);

        Map<String, Object> steelheartStats = new HashMap<>();
        steelheartStats.put("bookId", 2L);
        steelheartStats.put("loanCount", 25L);

        when(bookRepository.findAll()).thenReturn(List.of(dune, steelheart));
        when(loanRepository.getLoanCountsByBook()).thenReturn(List.of(duneStats, steelheartStats));

        mockMvc.perform(get("/api/boeken/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].titel").value("Steelheart"))
                .andExpect(jsonPath("$[0].loanCount").value(25))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].titel").value("Dune"))
                .andExpect(jsonPath("$[1].loanCount").value(10));
    }

    @Test
    void getStatsShouldReturnEmptyListWhenNoBooks() throws Exception {
        when(bookRepository.findAll()).thenReturn(List.of());
        when(loanRepository.getLoanCountsByBook()).thenReturn(List.of());

        mockMvc.perform(get("/api/boeken/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getStatsShouldUseGlobalExceptionHandlerWhenUnexpectedErrorOccurs() throws Exception {
        when(bookRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/boeken/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Database error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/api/boeken/stats"));
    }
}
