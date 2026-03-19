package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookRepository;
import com.example.demo.services.ImportCoreService;
import com.example.demo.services.IsbnService;
import com.example.demo.services.OpenLibraryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportCoreServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OpenLibraryService openLibraryService;

    @Spy
    private IsbnService isbnService;

    @InjectMocks
    private ImportCoreService importCoreService;

    private @NonNull School makeSchool() {
        School school = new School();
        school.setId(1L);
        school.setNaam("GO! Atheneum Antwerpen");
        return school;
    }

    private @NonNull Book makeBook() {
        Book b = new Book();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
    }

    @Test
    void importByNormalizedIsbnShouldReturnExistingBookWithoutCallingOpenLibrary() {
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.of(makeBook()));

        ImportCoreService.ImportOutcome result = importCoreService.importByNormalizedIsbn("9780553808049",
                makeSchool());

        assertEquals(ImportCoreService.ImportStatus.ALREADY_EXISTS, result.status());
        assertNotNull(result.bookDto());
        assertEquals("Dune", result.bookDto().getTitel());
        verify(openLibraryService, never()).fetchBookFromOpenLibrary(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void importByNormalizedIsbnShouldFetchSaveAndReturnAddedBook() {
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.empty());
        when(openLibraryService.fetchBookFromOpenLibrary("9780553808049")).thenReturn(makeBook());
        when(bookRepository.save(any(Book.class))).thenReturn(makeBook());

        ImportCoreService.ImportOutcome result = importCoreService.importByNormalizedIsbn("9780553808049",
                makeSchool());

        assertEquals(ImportCoreService.ImportStatus.ADDED, result.status());
        assertNotNull(result.bookDto());
        assertEquals("Dune", result.bookDto().getTitel());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void importByNormalizedIsbnShouldReturnNotFoundWhenMissingInOpenLibrary() {
        when(bookRepository.findByIsbnAndSchool_Id("0000000000000", 1L)).thenReturn(Optional.empty());
        when(openLibraryService.fetchBookFromOpenLibrary("0000000000000")).thenReturn(null);

        ImportCoreService.ImportOutcome result = importCoreService.importByNormalizedIsbn("0000000000000",
                makeSchool());

        assertEquals(ImportCoreService.ImportStatus.NOT_FOUND, result.status());
        assertNull(result.bookDto());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void importByNormalizedIsbnShouldStoreCanonicalIsbn() {
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.empty());

        Book fetched = makeBook();
        fetched.setIsbn("978-0-553-80804-9");
        when(openLibraryService.fetchBookFromOpenLibrary("9780553808049")).thenReturn(fetched);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportCoreService.ImportOutcome result = importCoreService.importByNormalizedIsbn("9780553808049",
                makeSchool());

        assertEquals(ImportCoreService.ImportStatus.ADDED, result.status());
        BookDto dto = result.bookDto();
        assertNotNull(dto);
        assertEquals("9780553808049", dto.getIsbn());
    }
}
