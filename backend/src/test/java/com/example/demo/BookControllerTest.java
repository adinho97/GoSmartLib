package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.controllers.BookController;
import com.example.demo.config.ConnectionPoolMonitor;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.services.BookDeletionService;
import com.example.demo.services.BookImportService;
import com.example.demo.services.BookLookupService;
import com.example.demo.services.BookQueryService;
import com.example.demo.services.BookStatsService;
import com.example.demo.services.BookWriteService;
import com.example.demo.services.LestipService;
import com.example.demo.services.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost")
@SuppressWarnings("null")
class BookControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AppUserRepository appUserRepository;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private SuperAdminRepository superAdminRepository;

        @MockBean
        private BookLookupService bookLookupService;

        @MockBean
        private BookImportService bookImportService;

        @MockBean
        private BookStatsService bookStatsService;

        @MockBean
        private BookDeletionService bookDeletionService;

        @MockBean
        private BookQueryService bookQueryService;

        @MockBean
        private BookWriteService bookWriteService;

        @MockBean
        private ReviewService reviewService;

        @MockBean
        private LestipService lestipService;

        @MockBean
        private ConnectionPoolMonitor connectionPoolMonitor;

        private BookDto makeDto() {
                BookDto dto = new BookDto();
                dto.setId(1L);
                dto.setTitel("Dune");
                dto.setAuteur("Frank Herbert");
                dto.setIsbn("9780553808049");
                dto.setGenres(List.of("Sciencefiction"));
                return dto;
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

        @Test
        void getByIsbnShouldReturn404WhenNotInDb() throws Exception {
                when(bookLookupService.findByIsbn("0000000000000", null)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/boeken/isbn/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void previewByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
                when(bookLookupService.fetchPreviewByIsbn("0000000000000")).thenReturn(null);

                mockMvc.perform(get("/api/boeken/preview/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void importByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
                when(bookImportService.importByIsbn("0000000000000", null, false)).thenReturn(null);

                mockMvc.perform(post("/api/boeken/isbn/0000000000000"))
                                .andExpect(status().isNotFound());
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

                when(bookImportService.importBulkByIsbn(any(), any(), any(), anyBoolean())).thenReturn(result);

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

                when(bookImportService.importBulkByIsbn(any(), any(), any(), anyBoolean()))
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
}
