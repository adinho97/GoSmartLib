package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookRepository;
import com.example.demo.services.BulkImportService;
import com.example.demo.services.BookService;
import com.example.demo.services.IsbnService;
import com.example.demo.services.OpenLibraryService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OpenLibraryService openLibraryService;

    @Spy
    private IsbnService isbnService;

    @Mock
    private BulkImportService bulkImportService;

    @InjectMocks
    private BookService bookService;

    @Mock
    private SchoolService schoolService;

    private School makeSchool() {
        School school = new School();
        school.setId(1L);
        school.setNaam("GO! Atheneum Antwerpen");
        return school;
    }

    private Book makeBook() {
        Book b = new Book();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
    }

    // ---- findByIsbn ---------------------------------------------------------

    @Test
    void findByIsbnShouldReturnDtoWhenBookIsInDb() {
        when(bookRepository.findByIsbn("9780553808049")).thenReturn(Optional.of(makeBook()));

        Optional<BookDto> result = bookService.findByIsbn("9780553808049", null);

        assertTrue(result.isPresent());
        assertEquals("Dune", result.get().getTitel());
        assertEquals("Frank Herbert", result.get().getAuteur());
        assertEquals("9780553808049", result.get().getIsbn());
    }

    @Test
    void findByIsbnShouldReturnEmptyWhenNotInDb() {
        when(bookRepository.findByIsbn("0000000000000")).thenReturn(Optional.empty());

        Optional<BookDto> result = bookService.findByIsbn("0000000000000", null);

        assertFalse(result.isPresent());
    }

    // ---- importByIsbn -------------------------------------------------------

    @Test
    void importByIsbnShouldReturnExistingBookWithoutCallingOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.of(makeBook()));

        BookDto result = bookService.importByIsbn("9780553808049", 1L);

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void importByIsbnShouldFetchFromOpenLibrarySaveAndReturnNewBook() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.empty());
        when(openLibraryService.fetchBookFromOpenLibrary("9780553808049")).thenReturn(makeBook());
        when(bookRepository.save(any(Book.class))).thenReturn(makeBook());

        BookDto result = bookService.importByIsbn("9780553808049", 1L);

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        verify(bookRepository).save(any(Book.class));
        verify(openLibraryService).fetchBookFromOpenLibrary("9780553808049");
    }

    @Test
    void importByIsbnShouldReturnNullWhenIsbnNotFoundInOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(bookRepository.findByIsbnAndSchool_Id("0000000000000", 1L)).thenReturn(Optional.empty());
        when(openLibraryService.fetchBookFromOpenLibrary("0000000000000")).thenReturn(null);

        BookDto result = bookService.importByIsbn("0000000000000", 1L);

        assertNull(result);
        verify(bookRepository, never()).save(any());
        verify(openLibraryService).fetchBookFromOpenLibrary("0000000000000");
    }

    // ---- fetchPreviewByIsbn -------------------------------------------------

    @Test
    void fetchPreviewByIsbnShouldReturnDtoWithoutPersisting() {
        Book preview = makeBook();
        preview.setTaal("Engels");
        when(openLibraryService.fetchBookFromOpenLibrary("9780553808049")).thenReturn(preview);

        BookDto result = bookService.fetchPreviewByIsbn("9780553808049");

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        assertEquals("Frank Herbert", result.getAuteur());
        assertEquals("Engels", result.getTaal());
        verify(bookRepository, never()).save(any());
        verify(openLibraryService).fetchBookFromOpenLibrary("9780553808049");
    }

    @Test
    void fetchPreviewByIsbnShouldMapFrenchLanguageToFrench() {
        Book preview = makeBook();
        preview.setTaal("Frans");
        when(openLibraryService.fetchBookFromOpenLibrary("9780156012195")).thenReturn(preview);

        BookDto result = bookService.fetchPreviewByIsbn("9780156012195");

        assertNotNull(result);
        assertEquals("Frans", result.getTaal());
        verify(openLibraryService).fetchBookFromOpenLibrary("9780156012195");
    }

    @Test
    void fetchPreviewByIsbnShouldReturnNullWhenNotFoundInOpenLibrary() {
        when(openLibraryService.fetchBookFromOpenLibrary("0000000000000")).thenReturn(null);

        BookDto result = bookService.fetchPreviewByIsbn("0000000000000");

        assertNull(result);
        verify(openLibraryService).fetchBookFromOpenLibrary("0000000000000");
    }
}
