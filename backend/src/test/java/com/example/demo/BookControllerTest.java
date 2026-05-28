package com.example.demo;

import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.services.OpenLibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @MockBean
    private OpenLibraryService openLibraryService;

    private Long savedBookId;

    @BeforeEach
    void setUp() {
        School school = new School();
        school.setSubdomein("test-school");
        school.setSmartschoolUrl("https://test.smartschool.be");
        schoolRepository.save(school);

        Book book = new Book();
        book.setTitel("Dune");
        book.setAuteur("Frank Herbert");
        book.setIsbn("9780553808049");
        savedBookId = bookRepository.save(book).getId();
    }

    @Test
    @WithMockUser(roles = "BIBBEHEERDER")
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

    @Test
    @WithMockUser
    void getByIsbnShouldReturn404WhenNotInDb() throws Exception {
        mockMvc.perform(get("/api/boeken/isbn/0000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void previewByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
        when(openLibraryService.fetchBookFromOpenLibrary(any())).thenReturn(null);

        mockMvc.perform(get("/api/boeken/preview/0000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BIBBEHEERDER")
    void importByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
        when(openLibraryService.fetchBookFromOpenLibrary(any())).thenReturn(null);

        mockMvc.perform(post("/api/boeken/isbn/0000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BIBBEHEERDER")
    void importBulkByIsbnShouldReturnResultWhenUploadIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bulk.csv",
                "text/csv",
                "isbn\n9780553808049".getBytes());

        Book mockBook = new Book();
        mockBook.setTitel("Dune");
        mockBook.setAuteur("Frank Herbert");
        when(openLibraryService.fetchBookFromOpenLibrary("9780553808049")).thenReturn(mockBook);

        mockMvc.perform(multipart("/api/boeken/isbn/bulk").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.uniqueIsbnsProcessed").value(1))
                .andExpect(jsonPath("$.results[0].status").value("ADDED"));
    }

    @Test
    @WithMockUser(roles = "BIBBEHEERDER")
    void importBulkByIsbnShouldReturnBadRequestWhenServiceRejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bulk.txt",
                "text/plain",
                "abc".getBytes());

        mockMvc.perform(multipart("/api/boeken/isbn/bulk").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteShouldReturnForbiddenWhenRoleHeaderMissing() throws Exception {
        mockMvc.perform(delete("/api/boeken/" + savedBookId))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteShouldReturnForbiddenBeforeCheckingExistenceWhenRoleHeaderMissing() throws Exception {
        mockMvc.perform(delete("/api/boeken/999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BIBBEHEERDER")
    void deleteShouldReturnNoContentForLibrarianWhenBookExists() throws Exception {
        mockMvc.perform(delete("/api/boeken/" + savedBookId)
                        .header("X-User-Role", "bibbeheerder"))
                .andExpect(status().isNoContent());
    }

        @Test
        void importBulkByIsbnShouldReturnResultWhenUploadIsValid() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "bulk.csv",
                                "text/csv",
                                "isbn\n9780553808049".getBytes());

                var result = new com.example.demo.dto.ImportResultDto();
                result.setTotalRows(1);
                result.setUniqueIsbnsProcessed(1);
                result.setDuplicateRowsSkipped(0);
                result.setResults(List.of(new com.example.demo.dto.ImportResultDto.RowResult(
                                "9780553808049",
                                com.example.demo.dto.ImportResultDto.Status.ADDED,
                                "Boek toegevoegd.",
                                1L)));

                when(bookImportService.importBulkByIsbn(any(), any(), any(), any(), anyBoolean())).thenReturn(result);

                mockMvc.perform(multipart("/api/boeken/isbn/bulk")
                                .file(file)
                                .param("schoolId", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalRows").value(1))
                                .andExpect(jsonPath("$.uniqueIsbnsProcessed").value(1))
                                .andExpect(jsonPath("$.results[0].status").value("ADDED"));
        }

        @Test
        void importBulkByIsbnShouldReturnBadRequestWhenServiceRejectsFile() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "bulk.txt",
                                "text/plain",
                                "abc".getBytes());

                when(bookImportService.importBulkByIsbn(any(), any(), any(), any(), anyBoolean()))
                                .thenThrow(new IllegalArgumentException("invalid"));

                mockMvc.perform(multipart("/api/boeken/isbn/bulk").file(file))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void deleteShouldReturnForbiddenWhenRoleHeaderMissing() throws Exception {
                mockMvc.perform(delete("/api/boeken/1"))
                                .andExpect(status().isForbidden());

                verify(bookDeletionService, never()).deleteBook(any(), any());
        }

        @Test
        void deleteShouldReturnForbiddenBeforeCheckingExistenceWhenRoleHeaderMissing() throws Exception {
                mockMvc.perform(delete("/api/boeken/999"))
                                .andExpect(status().isForbidden());

                verify(bookDeletionService, never()).deleteBook(any(), any());
        }

        @Test
        void deleteShouldReturnNoContentForLibrarianWhenBookExists() throws Exception {
                doNothing().when(bookDeletionService).deleteBook(1L, null);

                mockMvc.perform(delete("/api/boeken/1")
                                .header("X-User-Role", "bibbeheerder"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getByIsbnShouldReturnBookWhenFoundInDb() throws Exception {
                when(bookLookupService.findByIsbn("9780553808049", null)).thenReturn(Optional.of(makeDto()));

                mockMvc.perform(get("/api/boeken/isbn/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.auteur").value("Frank Herbert"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }
    @Test
    @WithMockUser
    void getByIsbnShouldReturnBookWhenFoundInDb() throws Exception {
        mockMvc.perform(get("/api/boeken/isbn/9780553808049"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titel").value("Dune"))
                .andExpect(jsonPath("$.auteur").value("Frank Herbert"))
                .andExpect(jsonPath("$.isbn").value("9780553808049"));
    }
}
