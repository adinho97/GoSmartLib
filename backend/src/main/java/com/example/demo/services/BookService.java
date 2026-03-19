package com.example.demo.services;

import com.example.demo.repositories.BookRepository;
import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.mappers.BookMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BookService {

    private static final int BULK_IMPORT_MAX_ROWS = 200;

    private final BookRepository bookRepository;
    private final SchoolService schoolService;
    private final OpenLibraryService openLibraryService;

    public BookService(BookRepository bookRepository, SchoolService schoolService,
            OpenLibraryService openLibraryService) {
        this.bookRepository = bookRepository;
        this.schoolService = schoolService;
        this.openLibraryService = openLibraryService;
    }

    public Optional<BookDto> findByIsbn(String isbn, Long schoolId) {
        String normalizedIsbn = requireValidNormalizedIsbn(isbn);

        if (schoolId == null) {
            return bookRepository.findByIsbn(normalizedIsbn)
                    .map(BookMapper::toDto);
        }

        return bookRepository.findByIsbnAndSchool_Id(normalizedIsbn, schoolId)
                .map(BookMapper::toDto);
    }

    public BookDto fetchPreviewByIsbn(String isbn) {
        String normalizedIsbn = requireValidNormalizedIsbn(isbn);
        Book fetched = openLibraryService.fetchBookFromOpenLibrary(normalizedIsbn);
        if (fetched == null) {
            return null;
        }
        return BookMapper.toDto(fetched);
    }

    @Transactional
    public BookDto importByIsbn(String isbn, Long schoolId) {
        String normalizedIsbn = requireValidNormalizedIsbn(isbn);
        School school = schoolService.getByIdOrDefault(schoolId);
        ImportOutcome outcome = importByIsbnInternal(normalizedIsbn, school);
        return outcome.bookDto();
    }

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name is required");
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);
        boolean isCsv = lowerName.endsWith(".csv");
        boolean isXls = lowerName.endsWith(".xls");
        boolean isXlsx = lowerName.endsWith(".xlsx");
        if (!isCsv && !isXls && !isXlsx) {
            throw new IllegalArgumentException("Unsupported file type. Use CSV, XLS or XLSX.");
        }

        // Resolve school once; bulk uses the same school-scoped behavior as single ISBN
        // import.
        School school = schoolService.getByIdOrDefault(schoolId);

        ParsedBulkIsbn parsed;
        if (isCsv) {
            parsed = parseCsvIsbns(file);
        } else {
            parsed = parseExcelIsbns(file);
        }

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(parsed.totalRows());
        result.setUniqueIsbnsProcessed(parsed.uniqueIsbns().size());
        result.setDuplicateRowsSkipped(parsed.duplicateRowsSkipped());

        if (parsed.totalRows() > BULK_IMPORT_MAX_ROWS) {
            throw new IllegalArgumentException(
                    "Te veel rijen in upload: maximaal " + BULK_IMPORT_MAX_ROWS + " ISBN's per bestand.");
        }

        List<ImportResultDto.RowResult> rows = new ArrayList<>(parsed.invalidRows());
        for (String isbn : parsed.uniqueIsbns()) {
            try {
                ImportOutcome outcome = importByIsbnInternal(isbn, school);
                if (outcome.status() == ImportStatus.ALREADY_EXISTS) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ALREADY_EXISTS,
                            "Boek bestaat al in de bibliotheek.",
                            outcome.bookDto() != null ? outcome.bookDto().getId() : null));
                    continue;
                }

                if (outcome.status() == ImportStatus.NOT_FOUND) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.NOT_FOUND,
                            "Geen boek gevonden voor dit ISBN.",
                            null));
                    continue;
                }

                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ADDED,
                        "Boek toegevoegd.",
                        outcome.bookDto() != null ? outcome.bookDto().getId() : null));
            } catch (Exception ex) {
                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ERROR,
                        "Fout bij verwerken van ISBN.",
                        null));
            }
        }

        result.setResults(rows);
        return result;
    }

    private ImportOutcome importByIsbnInternal(String isbn, School school) {
        Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId());
        if (existing.isPresent()) {
            return new ImportOutcome(ImportStatus.ALREADY_EXISTS, BookMapper.toDto(existing.get()));
        }

        Book fetched = openLibraryService.fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return new ImportOutcome(ImportStatus.NOT_FOUND, null);
        }

        fetched.setIsbn(normalizeAndValidateIsbn(fetched.getIsbn())
                .orElse(fetched.getIsbn()));

        fetched.setSchool(school);
        Book saved = bookRepository.save(fetched);
        return new ImportOutcome(ImportStatus.ADDED, BookMapper.toDto(saved));
    }

    private ParsedBulkIsbn parseCsvIsbns(MultipartFile file) {
        Set<String> uniqueIsbns = new LinkedHashSet<>();
        List<ImportResultDto.RowResult> invalidRows = new ArrayList<>();
        int totalRows = 0;
        int duplicateRowsSkipped = 0;

        try (InputStream in = file.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String rawFirstColumn = splitFirstColumn(trimmedLine).trim();
                if (lineNumber == 1 && rawFirstColumn.equalsIgnoreCase("isbn")) {
                    continue;
                }

                totalRows++;
                RowProcessResult rowResult = processParsedIsbnValue(
                        rawFirstColumn,
                        lineNumber,
                        uniqueIsbns,
                        invalidRows,
                        true);
                if (rowResult == RowProcessResult.DUPLICATE) {
                    duplicateRowsSkipped++;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file", e);
        }

        return new ParsedBulkIsbn(new ArrayList<>(uniqueIsbns), invalidRows, totalRows, duplicateRowsSkipped);
    }

    private ParsedBulkIsbn parseExcelIsbns(MultipartFile file) {
        Set<String> uniqueIsbns = new LinkedHashSet<>();
        List<ImportResultDto.RowResult> invalidRows = new ArrayList<>();
        int totalRows = 0;
        int duplicateRowsSkipped = 0;
        DataFormatter formatter = new DataFormatter();

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel file contains no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Cell cell = row.getCell(0);
                String rawFirstColumn = cell == null ? "" : formatter.formatCellValue(cell).trim();
                if (rawFirstColumn.isEmpty()) {
                    continue;
                }

                int lineNumber = rowIndex + 1;
                if (lineNumber == 1 && rawFirstColumn.equalsIgnoreCase("isbn")) {
                    continue;
                }

                totalRows++;
                RowProcessResult rowResult = processParsedIsbnValue(
                        rawFirstColumn,
                        lineNumber,
                        uniqueIsbns,
                        invalidRows,
                        false);
                if (rowResult == RowProcessResult.DUPLICATE) {
                    duplicateRowsSkipped++;
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read Excel file", ex);
        }

        return new ParsedBulkIsbn(new ArrayList<>(uniqueIsbns), invalidRows, totalRows, duplicateRowsSkipped);
    }

    private String splitFirstColumn(String line) {
        int comma = line.indexOf(',');
        int semicolon = line.indexOf(';');
        int tab = line.indexOf('\t');

        int splitAt = Integer.MAX_VALUE;
        if (comma >= 0) {
            splitAt = Math.min(splitAt, comma);
        }
        if (semicolon >= 0) {
            splitAt = Math.min(splitAt, semicolon);
        }
        if (tab >= 0) {
            splitAt = Math.min(splitAt, tab);
        }

        if (splitAt == Integer.MAX_VALUE) {
            return line;
        }
        return line.substring(0, splitAt);
    }

    private RowProcessResult processParsedIsbnValue(
            String rawFirstColumn,
            int lineNumber,
            Set<String> uniqueIsbns,
            List<ImportResultDto.RowResult> invalidRows,
            boolean treatEmptyAsInvalid) {
        if (rawFirstColumn.isEmpty()) {
            if (treatEmptyAsInvalid) {
                invalidRows.add(new ImportResultDto.RowResult(
                        "",
                        ImportResultDto.Status.INVALID_ISBN,
                        "Lege ISBN-waarde op rij " + lineNumber,
                        null));
            }
            return RowProcessResult.INVALID;
        }

        Optional<String> normalizedIsbn = normalizeAndValidateIsbn(rawFirstColumn);
        if (normalizedIsbn.isEmpty()) {
            invalidRows.add(new ImportResultDto.RowResult(
                    rawFirstColumn,
                    ImportResultDto.Status.INVALID_ISBN,
                    "Ongeldig ISBN-formaat",
                    null));
            return RowProcessResult.INVALID;
        }

        boolean added = uniqueIsbns.add(normalizedIsbn.get());
        return added ? RowProcessResult.ADDED : RowProcessResult.DUPLICATE;
    }

    private Optional<String> normalizeAndValidateIsbn(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String normalized = rawValue
                .replace("-", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        if (normalized.length() == 10) {
            if (!normalized.matches("\\d{9}[\\dX]")) {
                return Optional.empty();
            }
            return isValidIsbn10(normalized) ? Optional.of(normalized) : Optional.empty();
        }

        if (normalized.length() == 13) {
            if (!normalized.matches("\\d{13}")) {
                return Optional.empty();
            }
            return isValidIsbn13(normalized) ? Optional.of(normalized) : Optional.empty();
        }

        return Optional.empty();
    }

    private String requireValidNormalizedIsbn(String rawValue) {
        return normalizeAndValidateIsbn(rawValue)
                .orElseThrow(() -> new IllegalArgumentException("Ongeldig ISBN-formaat"));
    }

    private boolean isValidIsbn10(String isbn10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn10.charAt(i);
            int digit = (c == 'X') ? 10 : Character.getNumericValue(c);
            sum += (10 - i) * digit;
        }
        return sum % 11 == 0;
    }

    private boolean isValidIsbn13(String isbn13) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(isbn13.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int expectedCheck = (10 - (sum % 10)) % 10;
        int actualCheck = Character.getNumericValue(isbn13.charAt(12));
        return expectedCheck == actualCheck;
    }

    private record ParsedBulkIsbn(
            List<String> uniqueIsbns,
            List<ImportResultDto.RowResult> invalidRows,
            int totalRows,
            int duplicateRowsSkipped) {
    }

    private enum RowProcessResult {
        ADDED,
        DUPLICATE,
        INVALID
    }

    private enum ImportStatus {
        ADDED,
        ALREADY_EXISTS,
        NOT_FOUND
    }

    private record ImportOutcome(ImportStatus status, BookDto bookDto) {
    }
}
