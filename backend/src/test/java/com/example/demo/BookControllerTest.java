package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.controllers.BookController;
import com.example.demo.entities.Book;
import com.example.demo.entities.Review;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.ReviewModerationService;
import com.example.demo.services.BookService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookRepository bookRepository;

        @MockBean
        private BookService bookService;

        @MockBean
        private SchoolService schoolService;

        @MockBean
        private ReviewRepository reviewRepository;

        @MockBean
        private ReviewModerationService reviewModerationService;

        @Test
        void getAllShouldReturnBooks() throws Exception {
                Book book = new Book();
                book.setId(1L);
                book.setTitel("Dune");
                book.setAuteur("Frank Herbert");
                book.setGenre("Sciencefiction");

                when(bookRepository.findAll()).thenReturn(List.of(book));

                mockMvc.perform(get("/api/boeken"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].titel").value("Dune"))
                                .andExpect(jsonPath("$[0].auteur").value("Frank Herbert"));
        }

        @Test
        @SuppressWarnings("null")
        void createShouldPersistAndReturnBookWhenPayloadIsValid() throws Exception {
                School school = new School();
                school.setId(1L);
                school.setNaam("GO! Atheneum Antwerpen");

                Book saved = new Book();
                saved.setId(7L);
                saved.setTitel("Clean Code");
                saved.setAuteur("Robert C. Martin");
                saved.setCover("data:image/png;base64,abc");
                saved.setBeschrijving("Software craftsmanship");
                saved.setGenre("Programming");
                saved.setIsbn("9780132350884");
                saved.setUitgaveDatum(LocalDate.of(2008, 8, 1));
                saved.setPaginas(464);
                saved.setTaal("English");
                saved.setUitgeverij("Prentice Hall");

                when(schoolService.getByIdOrDefault(any())).thenReturn(school);
                when(bookRepository.findByIsbnAndSchool_Id("9780132350884", 1L)).thenReturn(Optional.empty());
                when(bookRepository.save(any(Book.class))).thenReturn(saved);

                String json = """
                                {
                                  "titel": "Clean Code",
                                  "auteur": "Robert C. Martin",
                                  "isbn": "9780132350884",
                                  "cover": "data:image/png;base64,abc",
                                  "beschrijving": "Software craftsmanship",
                                  "genre": "Programming",
                                  "uitgaveDatum": "2008-08-01",
                                  "paginas": 464,
                                  "taal": "English",
                                  "uitgeverij": "Prentice Hall"
                                }
                                """;

                mockMvc.perform(post("/api/boeken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(7))
                                .andExpect(jsonPath("$.titel").value("Clean Code"))
                                .andExpect(jsonPath("$.auteur").value("Robert C. Martin"))
                                .andExpect(jsonPath("$.isbn").value("9780132350884"));
        }

        @Test
        @SuppressWarnings("null")
        void createShouldReturnConflictWhenIsbnAlreadyExists() throws Exception {
                School school = new School();
                school.setId(1L);

                Book existing = new Book();
                existing.setId(5L);
                existing.setTitel("Existing Book");
                existing.setAuteur("Existing Author");
                existing.setIsbn("9780132350884");

                when(schoolService.getByIdOrDefault(any())).thenReturn(school);
                when(bookRepository.findByIsbnAndSchool_Id("9780132350884", 1L)).thenReturn(Optional.of(existing));

                String json = """
                                {
                                  "titel": "Clean Code",
                                  "auteur": "Robert C. Martin",
                                  "isbn": "9780132350884"
                                }
                                """;

                mockMvc.perform(post("/api/boeken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isConflict());
        }

        @Test
        @SuppressWarnings("null")
        void createShouldReturnBadRequestWhenRequiredFieldIsMissing() throws Exception {
                String invalidJson = """
                                {
                                  "titel": "",
                                  "auteur": "Auteur"
                                }
                                """;

                mockMvc.perform(post("/api/boeken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                                .andExpect(status().isBadRequest());
        }

        // ---- GET /api/boeken/isbn/{isbn} ----------------------------------------

        @Test
        void getByIsbnShouldReturnBookWhenFoundInDb() throws Exception {
                when(bookService.findByIsbn("9780553808049", null)).thenReturn(Optional.of(makeDto()));

                mockMvc.perform(get("/api/boeken/isbn/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.auteur").value("Frank Herbert"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }

        @Test
        void getByIsbnShouldReturn404WhenNotInDb() throws Exception {
                when(bookService.findByIsbn("0000000000000", null)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/boeken/isbn/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        // ---- GET /api/boeken/preview/{isbn} -------------------------------------

        @Test
        void previewByIsbnShouldReturnBookFromOpenLibrary() throws Exception {
                when(bookService.fetchPreviewByIsbn("9780553808049")).thenReturn(makeDto());

                mockMvc.perform(get("/api/boeken/preview/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }

        @Test
        void previewByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
                when(bookService.fetchPreviewByIsbn("0000000000000")).thenReturn(null);

                mockMvc.perform(get("/api/boeken/preview/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        // ---- POST /api/boeken/isbn/{isbn} ---------------------------------------

        @Test
        void importByIsbnShouldReturnSavedBook() throws Exception {
                when(bookService.importByIsbn("9780553808049", null)).thenReturn(makeDto());

                mockMvc.perform(post("/api/boeken/isbn/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }

        @Test
        void importByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
                when(bookService.importByIsbn("0000000000000", null)).thenReturn(null);

                mockMvc.perform(post("/api/boeken/isbn/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        // ---- helpers ------------------------------------------------------------

        private BookDto makeDto() {
                BookDto dto = new BookDto();
                dto.setId(1L);
                dto.setTitel("Dune");
                dto.setAuteur("Frank Herbert");
                dto.setIsbn("9780553808049");
                return dto;
        }

        @Test
        void deleteShouldReturnForbiddenWhenRoleHeaderMissing() throws Exception {
                when(bookRepository.existsById(1L)).thenReturn(true);
                doNothing().when(bookRepository).deleteById(1L);

                mockMvc.perform(delete("/api/boeken/1"))
                                .andExpect(status().isForbidden());

                verify(bookRepository, never()).deleteById(1L);
        }

        @Test
        void deleteShouldReturnForbiddenBeforeCheckingExistenceWhenRoleHeaderMissing() throws Exception {
                when(bookRepository.existsById(999L)).thenReturn(false);

                mockMvc.perform(delete("/api/boeken/999"))
                                .andExpect(status().isForbidden());

                verify(bookRepository, never()).existsById(999L);
        }

        @Test
        void deleteShouldReturnNoContentForLibrarianWhenBookExists() throws Exception {
                when(bookRepository.existsById(1L)).thenReturn(true);
                doNothing().when(bookRepository).deleteById(1L);

                mockMvc.perform(delete("/api/boeken/1")
                                .header("X-User-Role", "bibbeheerder"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getReviewsShouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
                when(bookRepository.existsById(1L)).thenReturn(false);

                mockMvc.perform(get("/api/boeken/1/reviews"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getReviewsShouldReturnMappedReviewsForBook() throws Exception {
                Review first = new Review();
                first.setId(3L);
                first.setRating(5);
                first.setComment("Topboek");
                first.setCreatedAt(LocalDateTime.of(2026, 3, 18, 12, 30));

                Review second = new Review();
                second.setId(2L);
                second.setRating(4);
                second.setComment("Goed");
                second.setCreatedAt(LocalDateTime.of(2026, 3, 17, 11, 15));

                when(bookRepository.existsById(1L)).thenReturn(true);
                when(reviewRepository.findByBook_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(first, second));

                mockMvc.perform(get("/api/boeken/1/reviews"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(3))
                                .andExpect(jsonPath("$[0].rating").value(5))
                                .andExpect(jsonPath("$[0].comment").value("Topboek"))
                                .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        void createReviewShouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
                when(bookRepository.findById(404L)).thenReturn(Optional.empty());

                String json = """
                                {
                                  "rating": 5,
                                  "comment": "Sterk boek"
                                }
                                """;

                mockMvc.perform(post("/api/boeken/404/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isNotFound());
        }

        @Test
        void createReviewShouldPersistTrimmedCommentAndReturnCreated() throws Exception {
                Book book = new Book();
                book.setId(1L);

                Review saved = new Review();
                saved.setId(9L);
                saved.setBook(book);
                saved.setRating(5);
                saved.setComment("Heel goed boek");
                saved.setCreatedAt(LocalDateTime.of(2026, 3, 18, 14, 0));

                when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
                when(reviewRepository.save(any(Review.class))).thenReturn(saved);

                String json = """
                                {
                                  "rating": 5,
                                  "comment": "  Heel goed boek  "
                                }
                                """;

                mockMvc.perform(post("/api/boeken/1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(9))
                                .andExpect(jsonPath("$.rating").value(5))
                                .andExpect(jsonPath("$.comment").value("Heel goed boek"));

                ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
                verify(reviewRepository).save(captor.capture());
                verify(reviewModerationService).validateReviewComment("Heel goed boek");

                Review persisted = captor.getValue();
                assertSame(book, persisted.getBook());
                assertEquals(5, persisted.getRating());
                assertEquals("Heel goed boek", persisted.getComment());
        }

        @Test
        void createReviewShouldReturnBadRequestForInvalidPayload() throws Exception {
                String invalidJson = """
                                {
                                  "rating": 0,
                                  "comment": ""
                                }
                                """;

                mockMvc.perform(post("/api/boeken/1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                                .andExpect(status().isBadRequest());

                verify(bookRepository, never()).findById(1L);
        }

        @Test
        void deleteReviewShouldReturnForbiddenForNonLibrarian() throws Exception {
                mockMvc.perform(delete("/api/boeken/1/reviews/2"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void deleteReviewShouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
                when(bookRepository.existsById(1L)).thenReturn(false);

                mockMvc.perform(delete("/api/boeken/1/reviews/2")
                                .header("X-User-Role", "bibbeheerder"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void deleteReviewShouldReturnNotFoundWhenReviewDoesNotBelongToBook() throws Exception {
                Book otherBook = new Book();
                otherBook.setId(99L);

                Review review = new Review();
                review.setId(2L);
                review.setBook(otherBook);

                when(bookRepository.existsById(1L)).thenReturn(true);
                when(reviewRepository.findById(2L)).thenReturn(Optional.of(review));

                mockMvc.perform(delete("/api/boeken/1/reviews/2")
                                .header("X-User-Role", "bibbeheerder"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void deleteReviewShouldReturnNoContentWhenReviewBelongsToBook() throws Exception {
                Book book = new Book();
                book.setId(1L);

                Review review = new Review();
                review.setId(2L);
                review.setBook(book);

                when(bookRepository.existsById(1L)).thenReturn(true);
                when(reviewRepository.findById(2L)).thenReturn(Optional.of(review));

                mockMvc.perform(delete("/api/boeken/1/reviews/2")
                                .header("X-User-Role", "bibbeheerder"))
                                .andExpect(status().isNoContent());

                verify(reviewRepository).delete(review);
        }
}
