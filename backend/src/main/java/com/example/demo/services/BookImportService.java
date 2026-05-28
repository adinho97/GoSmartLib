package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BookImportService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final SchoolService schoolService;
    private final IsbnService isbnService;
    private final BulkImportService bulkImportService;
    private final ImportCoreService importCoreService;

    public BookImportService(BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            SchoolService schoolService,
            IsbnService isbnService,
            BulkImportService bulkImportService,
            ImportCoreService importCoreService) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.schoolService = schoolService;
        this.isbnService = isbnService;
        this.bulkImportService = bulkImportService;
        this.importCoreService = importCoreService;
    }

    @Transactional
    public BookDto importByIsbn(String isbn, Long schoolId, boolean isDidactisch) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);
        School school = schoolService.getByIdOrDefault(schoolId);
        ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(normalizedIsbn, school);
        
        // If didactisch flag is set and book was imported, set the didactisch genre
        if (isDidactisch && outcome.bookDto() != null && outcome.bookDto().getId() != null) {
            Book book = bookRepository.findById(outcome.bookDto().getId()).orElse(null);
            if (book != null) {
                setDidactischGenre(book);
            }
        }
        
        return outcome.bookDto();
    }

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId, String defaultCondition, String booksConfig, boolean isDidactisch) {
        School school = schoolService.getByIdOrDefault(schoolId);
        BulkImportService.ParsedBulkIsbn parsed = bulkImportService.parseAndValidate(file, defaultCondition);

        // Parse booksConfig if provided to override conditions
        Map<String, List<String>> configMap = parseBooksConfig(booksConfig);

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(parsed.totalRows());
        result.setUniqueIsbnsProcessed(parsed.isbnQuantityPairs().size());
        result.setDuplicateRowsSkipped(parsed.duplicateRowsSkipped());

        List<ImportResultDto.RowResult> rows = new ArrayList<>(parsed.invalidRows());
        int totalCopiesAdded = 0;
        for (BulkImportService.IsbnQuantityPair pair : parsed.isbnQuantityPairs()) {
            String isbn = pair.isbn();
            int quantity = pair.getQuantity();
            List<BookCopy.CopyCondition> conditions = pair.conditions();

            // Override conditions from booksConfig if provided
            if (!configMap.isEmpty() && configMap.containsKey(isbn)) {
                List<String> configConditions = configMap.get(isbn);
                conditions = new ArrayList<>();
                for (String condStr : configConditions) {
                    try {
                        conditions.add(BookCopy.CopyCondition.valueOf(condStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        conditions.add(BookCopy.CopyCondition.GOOD);
                    }
                }
            }
            try {
                ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(isbn, school);
                Book bookToAssociateCopies = null;
                Long bookIdForDiagnostic = null;

                if (outcome.status() == ImportCoreService.ImportStatus.ALREADY_EXISTS) {
                    Book book = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId()).orElse(null);
                    if (book != null) {
                        bookToAssociateCopies = book;
                        bookIdForDiagnostic = book.getId();
                        for (int i = 0; i < quantity; i++) {
                            BookCopy copy = new BookCopy();
                            copy.setBook(bookToAssociateCopies);
                            copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                            copy.setCondition(conditions.get(i));
                            bookCopyRepository.save(copy);
                        }
                        totalCopiesAdded += quantity;
                        rows.add(new ImportResultDto.RowResult(
                                isbn,
                                ImportResultDto.Status.ADDED,
                                "Boek al in bibliotheek - " + quantity + " exemplaren toegevoegd.",
                                bookIdForDiagnostic));
                    } else {
                        rows.add(new ImportResultDto.RowResult(
                                isbn,
                                ImportResultDto.Status.ERROR,
                                "Boek bestaat maar kon niet worden gevonden.",
                                null));
                    }
                    continue;
                }

                if (outcome.status() == ImportCoreService.ImportStatus.NOT_FOUND) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.NOT_FOUND,
                            "Geen boek gevonden voor dit ISBN.",
                            null));
                    continue;
                }

                BookDto newlyAddedBookDto = outcome.bookDto();
                if (newlyAddedBookDto != null && newlyAddedBookDto.getId() != null) {
                    bookToAssociateCopies = bookRepository.findById(newlyAddedBookDto.getId()).orElse(null);
                    bookIdForDiagnostic = newlyAddedBookDto.getId();
                    
                    // Apply didactisch genre if requested
                    if (isDidactisch && bookToAssociateCopies != null) {
                        setDidactischGenre(bookToAssociateCopies);
                    }
                }

                if (bookToAssociateCopies != null) {
                    for (int i = 0; i < quantity; i++) {
                        BookCopy copy = new BookCopy();
                        copy.setBook(bookToAssociateCopies);
                        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                        copy.setCondition(conditions.get(i));
                        bookCopyRepository.save(copy);
                    }
                    totalCopiesAdded += quantity;
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ADDED,
                            "Boek toegevoegd met " + quantity + " exemplaar(en).",
                            bookIdForDiagnostic));
                } else {
                        rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ERROR,
                            "Boek is geimporteerd, maar er is een fout opgetreden bij het toevoegen van exemplaren.",
                            bookIdForDiagnostic));
                }
            } catch (Exception ex) {
                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ERROR,
                        "Fout bij verwerken van ISBN: " + ex.getMessage(),
                        null));
            }
        }

        result.setTotalCopiesAdded(totalCopiesAdded);
        result.setResults(rows);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> parseBooksConfig(String booksConfigJson) {
        Map<String, List<String>> configMap = new java.util.HashMap<>();
        if (booksConfigJson == null || booksConfigJson.isEmpty()) {
            return configMap;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> books = mapper.readValue(booksConfigJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            for (Map<String, Object> book : books) {
                String isbn = (String) book.get("isbn");
                if (isbn != null) {
                    List<Map<String, String>> copies = (List<Map<String, String>>) book.get("copies");
                    if (copies != null) {
                        List<String> conditions = new ArrayList<>();
                        for (Map<String, String> copy : copies) {
                            String condition = copy.get("condition");
                            conditions.add(condition != null ? condition : "GOOD");
                        }
                        configMap.put(isbn, conditions);
                    }
                }
            }
        } catch (Exception e) {
            // If parsing fails, just return empty map and use defaults
        }

        return configMap;
    }

    private void setDidactischGenre(Book book) {
        // Find or create "Didactiek" genre
        // For now, we'll just set the genres to include "Didactiek"
        // This assumes you have Genre entities and a relationship set up
        // If you need to actually add to genres set, you'd do:
        // book.getGenres().add(didactiekGenre);
        // For MVP, we'll keep this simple and just mark for future enhancement
    }
}
