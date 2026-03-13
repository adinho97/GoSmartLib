package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.services.BookService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
        void deleteShouldReturnNoContentWhenBookExists() throws Exception {
                when(bookRepository.existsById(1L)).thenReturn(true);
                doNothing().when(bookRepository).deleteById(1L);

                mockMvc.perform(delete("/api/boeken/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteShouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
                when(bookRepository.existsById(999L)).thenReturn(false);

                mockMvc.perform(delete("/api/boeken/999"))
                                .andExpect(status().isNotFound());
        }
}
