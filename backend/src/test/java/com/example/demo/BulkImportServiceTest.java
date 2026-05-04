package com.example.demo;

import com.example.demo.dto.ImportResultDto;
import com.example.demo.services.BulkImportService;
import com.example.demo.services.IsbnService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BulkImportServiceTest {

        private final BulkImportService bulkImportService = new BulkImportService(new IsbnService());

        @Test
        void parseAndValidateShouldParseCsvNormalizeDetectDuplicateAndInvalid() {
                String csv = String.join("\n",
                                "isbn",
                                "978-0-553-80804-9",
                                "9780553808049",
                                "NOT-AN-ISBN");

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "bulk.csv",
                                "text/csv",
                                csv.getBytes(StandardCharsets.UTF_8));

                BulkImportService.ParsedBulkIsbn parsed = bulkImportService.parseAndValidate(file);

                assertEquals(3, parsed.totalRows());
                assertEquals(1, parsed.isbnQuantityPairs().size());
                assertEquals("9780553808049", parsed.isbnQuantityPairs().get(0).isbn());
                assertEquals(1, parsed.isbnQuantityPairs().get(0).quantity());
                assertEquals(1, parsed.duplicateRowsSkipped());
                assertEquals(1, parsed.invalidRows().size());
                assertEquals(ImportResultDto.Status.INVALID_ISBN, parsed.invalidRows().get(0).status());
        }

        @Test
        void parseAndValidateShouldRejectUnsupportedFileType() {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "bulk.txt",
                                "text/plain",
                                "9780553808049".getBytes(StandardCharsets.UTF_8));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> bulkImportService.parseAndValidate(file));

                assertEquals("Unsupported file type. Use CSV, XLS or XLSX.", ex.getMessage());
        }

        @Test
        void parseAndValidateShouldRejectFilesWithMoreThan200Rows() {
                StringBuilder csv = new StringBuilder("isbn\n");
                for (int i = 0; i < 201; i++) {
                        csv.append("9780553808049\n");
                }

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "bulk.csv",
                                "text/csv",
                                csv.toString().getBytes(StandardCharsets.UTF_8));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> bulkImportService.parseAndValidate(file));

                assertEquals("Te veel rijen in upload: maximaal 200 ISBN's per bestand.", ex.getMessage());
        }
}
