package com.example.demo;

import com.example.demo.controllers.BookController;
import com.example.demo.entities.Book;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.BookDeletionService;
import com.example.demo.services.BookImportService;
import com.example.demo.services.BookLookupService;
import com.example.demo.services.BookQueryService;
import com.example.demo.services.BookStatsService;
import com.example.demo.services.BookWriteService;
import com.example.demo.services.LestipService;
import com.example.demo.services.ReviewService;
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
        private BookMapper bookMapper;
        private BookStatsService bookStatsService;

        @BeforeEach
        void setUp() {
                bookRepository = mock(BookRepository.class);
                loanRepository = mock(LoanRepository.class);
                bookMapper = mock(BookMapper.class);
                when(bookMapper.toDto(org.mockito.ArgumentMatchers.any(Book.class))).thenAnswer(invocation -> {
                        Book book = invocation.getArgument(0);
                        var dto = new com.example.demo.dto.BookDto();
                        dto.setId(book.getId());
                        dto.setTitel(book.getTitel());
                        dto.setAuteur(book.getAuteur());
                        return dto;
                });

                bookStatsService = new BookStatsService(
                                bookRepository,
                                loanRepository,
                                bookMapper);

                AppUserRepository appUserRepository = mock(AppUserRepository.class);
                BookLookupService bookLookupService = mock(BookLookupService.class);
                BookImportService bookImportService = mock(BookImportService.class);
                BookDeletionService bookDeletionService = mock(BookDeletionService.class);
                BookQueryService bookQueryService = mock(BookQueryService.class);
                BookWriteService bookWriteService = mock(BookWriteService.class);
                ReviewService reviewService = mock(ReviewService.class);
                LestipService lestipService = mock(LestipService.class);

                BookController controller = new BookController(
                                appUserRepository,
                                bookLookupService,
                                bookImportService,
                                bookStatsService,
                                bookDeletionService,
                                bookQueryService,
                                bookWriteService,
                                reviewService,
                                lestipService);

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
