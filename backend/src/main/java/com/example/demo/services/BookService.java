package com.example.demo.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final int BULK_IMPORT_MAX_ROWS = 200;

    private static final String LANGUAGE_MAPPING_RESOURCE = "language-mapping.json";
    private static final Map<String, String> LANGUAGE_TRANSLATIONS = loadLanguageTranslations();

    private static final String GENRE_MAPPING_RESOURCE = "genre-mapping.json";
    private static final Map<String, String> GENRE_TRANSLATIONS = loadJsonMapping(GENRE_MAPPING_RESOURCE);

    private final BookRepository bookRepository;
    private final SchoolService schoolService;

    public BookService(BookRepository bookRepository, SchoolService schoolService) {
        this.bookRepository = bookRepository;
        this.schoolService = schoolService;
    }

    public Optional<BookDto> findByIsbn(String isbn, Long schoolId) {
        if (schoolId == null) {
            return bookRepository.findByIsbn(isbn)
                    .map(BookMapper::toDto);
        }

        return bookRepository.findByIsbnAndSchool_Id(isbn, schoolId)
                .map(BookMapper::toDto);
    }

    public BookDto fetchPreviewByIsbn(String isbn) {
        Book fetched = fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return null;
        }
        return BookMapper.toDto(fetched);
    }

    @Transactional
    public BookDto importByIsbn(String isbn, Long schoolId) {
        School school = schoolService.getByIdOrDefault(schoolId);

        Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId());
        if (existing.isPresent()) {
            return BookMapper.toDto(existing.get());
        }

        Book fetched = fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return null;
        }

        fetched.setSchool(school);
        Book saved = bookRepository.save(fetched);
        return BookMapper.toDto(saved);
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
                Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId());
                if (existing.isPresent()) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ALREADY_EXISTS,
                            "Boek bestaat al in de bibliotheek.",
                            existing.get().getId()));
                    continue;
                }

                BookDto imported = importByIsbn(isbn, school.getId());
                if (imported == null) {
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
                        imported.getId()));
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
                if (rawFirstColumn.isEmpty()) {
                    invalidRows.add(new ImportResultDto.RowResult(
                            "",
                            ImportResultDto.Status.INVALID_ISBN,
                            "Lege ISBN-waarde op rij " + lineNumber,
                            null));
                    continue;
                }

                Optional<String> normalizedIsbn = normalizeAndValidateIsbn(rawFirstColumn);
                if (normalizedIsbn.isEmpty()) {
                    invalidRows.add(new ImportResultDto.RowResult(
                            rawFirstColumn,
                            ImportResultDto.Status.INVALID_ISBN,
                            "Ongeldig ISBN-formaat",
                            null));
                    continue;
                }

                boolean added = uniqueIsbns.add(normalizedIsbn.get());
                if (!added) {
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
                Optional<String> normalizedIsbn = normalizeAndValidateIsbn(rawFirstColumn);
                if (normalizedIsbn.isEmpty()) {
                    invalidRows.add(new ImportResultDto.RowResult(
                            rawFirstColumn,
                            ImportResultDto.Status.INVALID_ISBN,
                            "Ongeldig ISBN-formaat",
                            null));
                    continue;
                }

                boolean added = uniqueIsbns.add(normalizedIsbn.get());
                if (!added) {
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

    private Book fetchBookFromOpenLibrary(String isbn) {
        String url = "https://openlibrary.org/isbn/" + isbn + ".json";
        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> body = fetchJsonMap(url, restTemplate);
            if (body == null) {
                return null;
            }

            Book book = new Book();
            book.setIsbn(isbn);

            Object title = body.get("title");
            book.setTitel(title instanceof String ? (String) title : "Unknown title");

            // Pages
            Object pages = body.get("number_of_pages");
            if (pages instanceof Number) {
                book.setPaginas(((Number) pages).intValue());
            }

            // Description (can be string or object in OpenLibrary)
            Object description = body.get("description");
            if (description instanceof String) {
                book.setBeschrijving((String) description);
            } else if (description instanceof Map) {
                Object value = ((Map<?, ?>) description).get("value");
                if (value instanceof String) {
                    book.setBeschrijving((String) value);
                }
            }

            // Publish date (best-effort parsing)
            Object publishDate = body.get("publish_date");
            if (publishDate instanceof String publishDateStr) {
                LocalDate parsed = tryParsePublishDate(publishDateStr);
                if (parsed != null) {
                    book.setUitgaveDatum(parsed);
                }
            }

            book.setTaal(resolveLanguage(body));

            // Cover image
            Object covers = body.get("covers");
            if (covers instanceof List<?> coverList && !coverList.isEmpty()) {
                Object first = coverList.get(0);
                if (first instanceof Number) {
                    int coverId = ((Number) first).intValue();
                    String coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
                    book.setCover(coverUrl);
                }
            }

            // Publisher
            Object publishers = body.get("publishers");
            if (publishers instanceof List<?> publisherList && !publisherList.isEmpty()) {
                Object first = publisherList.get(0);
                if (first instanceof String) {
                    book.setUitgeverij((String) first);
                }
            }

            // Author: fetch from OpenLibrary authors API
            book.setAuteur(fetchAuthorName(body, restTemplate));

            // Genre: derived from subjects on edition or work
            book.setGenre(resolveGenre(body, restTemplate));

            return book;
        } catch (HttpClientErrorException.NotFound e) {
            // ISBN not found in OpenLibrary
            return null;
        } catch (Exception e) {
            // Network or parsing error etc.
            return null;
        }
    }

    private String fetchAuthorName(Map<String, Object> editionBody, RestTemplate restTemplate) {
        try {
            // Edition-level authors
            Object authors = editionBody.get("authors");
            if (authors instanceof List<?> authorList && !authorList.isEmpty()) {
                Object firstAuthor = authorList.get(0);
                if (firstAuthor instanceof Map<?, ?> authorMap) {
                    Object key = authorMap.get("key");
                    if (key instanceof String authorKey) {
                        String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                        Map<String, Object> authorBody = fetchJsonMap(authorUrl, restTemplate);
                        if (authorBody != null) {
                            Object name = authorBody.get("name");
                            if (name instanceof String) {
                                return (String) name;
                            }
                        }
                    }
                }
            }

            // Fallback: fetch via works endpoint
            Object works = editionBody.get("works");
            if (works instanceof List<?> workList && !workList.isEmpty()) {
                Object firstWork = workList.get(0);
                if (firstWork instanceof Map<?, ?> workMap) {
                    Object key = workMap.get("key");
                    if (key instanceof String workKey) {
                        String workUrl = "https://openlibrary.org" + workKey + ".json";
                        Map<String, Object> workBody = fetchJsonMap(workUrl, restTemplate);
                        if (workBody != null) {
                            Object workAuthors = workBody.get("authors");
                            if (workAuthors instanceof List<?> waList && !waList.isEmpty()) {
                                Object wa = waList.get(0);
                                if (wa instanceof Map<?, ?> waMap) {
                                    Object authorRef = waMap.get("author");
                                    if (authorRef instanceof Map<?, ?> authorRefMap) {
                                        Object aKey = authorRefMap.get("key");
                                        if (aKey instanceof String authorKey) {
                                            String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                                            Map<String, Object> authorBody = fetchJsonMap(authorUrl, restTemplate);
                                            if (authorBody != null) {
                                                Object name = authorBody.get("name");
                                                if (name instanceof String) {
                                                    return (String) name;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Author lookup failed, fall through
        }
        return "Onbekende auteur";
    }

    private String resolveGenre(Map<String, Object> body, RestTemplate restTemplate) {
        List<String> subjects = extractSubjects(body);

        if (subjects.isEmpty()) {
            // Fall back to subjects on the linked work
            try {
                Object works = body.get("works");
                if (works instanceof List<?> workList && !workList.isEmpty()) {
                    Object firstWork = workList.get(0);
                    if (firstWork instanceof Map<?, ?> workMap) {
                        Object key = workMap.get("key");
                        if (key instanceof String workKey) {
                            String workUrl = "https://openlibrary.org" + workKey + ".json";
                            Map<String, Object> workBody = fetchJsonMap(workUrl, restTemplate);
                            if (workBody != null) {
                                subjects = extractSubjects(workBody);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (subjects.isEmpty())
            return null;

        String genre = subjects.stream()
                .limit(3)
                .map(this::translateSubject)
                .collect(Collectors.joining(", "));
        return genre.length() > 100 ? genre.substring(0, 100) : genre;
    }

    private List<String> extractSubjects(Map<?, ?> body) {
        Object subjects = body.get("subjects");
        if (subjects instanceof List<?> list) {
            return list.stream()
                    .filter(s -> s instanceof String)
                    .map(s -> (String) s)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Map<String, Object> fetchJsonMap(String url, RestTemplate restTemplate) {
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return null;
        }

        Object responseBody = response.getBody();
        if (!(responseBody instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private LocalDate tryParsePublishDate(String value) {
        String[] patterns = { "yyyy-MM-dd", "yyyy", "MMMM d, yyyy", "MMM d, yyyy" };
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String resolveLanguage(Map<String, Object> body) {
        Object languages = body.get("languages");
        if (languages instanceof List<?> languageList && !languageList.isEmpty()) {
            Object first = languageList.get(0);
            if (first instanceof Map<?, ?> langMap) {
                Object key = langMap.get("key");
                if (key instanceof String langKey) {
                    return mapLanguageCodeToDutch(langKey);
                }
            } else if (first instanceof String langCode) {
                return mapLanguageCodeToDutch(langCode);
            }
        }

        // Fallbacks sometimes present in third-party payloads
        Object language = body.get("language");
        if (language instanceof String lang) {
            return mapLanguageCodeToDutch(lang);
        }

        return "Onbekend";
    }

    private String mapLanguageCodeToDutch(String rawCode) {
        if (rawCode == null)
            return "Onbekend";
        String code = rawCode.trim().toLowerCase(Locale.ROOT);
        int slash = code.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < code.length())
            code = code.substring(slash + 1);
        return code.isEmpty() ? "Onbekend" : LANGUAGE_TRANSLATIONS.getOrDefault(code, "Onbekend");
    }

    private String translateSubject(String subject) {
        if (subject == null)
            return "";
        String key = subject.trim().toLowerCase(Locale.ROOT);
        return GENRE_TRANSLATIONS.getOrDefault(key, subject);
    }

    private static Map<String, String> loadLanguageTranslations() {
        return loadJsonMapping(LANGUAGE_MAPPING_RESOURCE);
    }

    private static Map<String, String> loadJsonMapping(String resource) {
        try (InputStream in = BookService.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null)
                return Collections.emptyMap();
            Map<String, String> raw = new ObjectMapper().readValue(in, new TypeReference<>() {
            });
            Map<String, String> result = new HashMap<>();
            raw.forEach((k, v) -> {
                if (k != null && !k.isBlank())
                    result.put(k.trim().toLowerCase(Locale.ROOT), v != null ? v : "");
            });
            return Collections.unmodifiableMap(result);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
