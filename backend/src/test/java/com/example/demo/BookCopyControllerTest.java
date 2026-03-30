package com.example.demo;

import com.example.demo.controllers.BookCopyController;
import com.example.demo.dto.CopyDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookCopyControllerTest {

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private BookCopyController bookCopyController;

    private @NonNull Book makeBook() {
        Book b = new Book();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
    }

    // ---- addCopy Tests for Single ISBN and Barcode Scanner ------------------

    @Test
    void addCopyShouldCreateOneAvailableCopy() {
        Long bookId = 1L;
        Book book = makeBook();
        BookCopy savedCopy = new BookCopy();
        savedCopy.setId(10L);
        savedCopy.setBook(book);
        savedCopy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(savedCopy);

        ResponseEntity<CopyDto> response = bookCopyController.addCopy(bookId, "bibbeheerder");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getId());
        assertEquals(BookCopy.CopyStatus.AVAILABLE, response.getBody().getStatus());
        
        // Verify copy was saved with correct book
        ArgumentCaptor<BookCopy> captor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(captor.capture());
        assertEquals(book.getId(), captor.getValue().getBook().getId());
    }

    @Test
    void addCopyShouldSetStatusToAvailable() {
        Long bookId = 1L;
        Book book = makeBook();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(invocation -> {
            BookCopy copy = invocation.getArgument(0);
            assertEquals(BookCopy.CopyStatus.AVAILABLE, copy.getStatus());
            copy.setId(99L);
            return copy;
        });

        bookCopyController.addCopy(bookId, "leerkracht");

        verify(bookCopyRepository).save(any(BookCopy.class));
    }

    @Test
    void addCopyShouldReturnForbiddenWhenUserRoleInvalid() {
        Long bookId = 1L;

        ResponseEntity<CopyDto> response = bookCopyController.addCopy(bookId, "student");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(bookRepository, never()).findById(any());
        verify(bookCopyRepository, never()).save(any());
    }

    @Test
    void addCopyShouldReturnNotFoundWhenBookDoesNotExist() {
        Long bookId = 999L;

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        ResponseEntity<CopyDto> response = bookCopyController.addCopy(bookId, "bibbeheerder");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(bookCopyRepository, never()).save(any());
    }

    @Test
    void addCopyShouldAcceptLeerkrachtRole() {
        Long bookId = 1L;
        Book book = makeBook();
        BookCopy savedCopy = new BookCopy();
        savedCopy.setId(20L);
        savedCopy.setBook(book);
        savedCopy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(savedCopy);

        ResponseEntity<CopyDto> response = bookCopyController.addCopy(bookId, "leerkracht");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookCopyRepository).save(any(BookCopy.class));
    }

    @Test
    void addCopyShouldAcceptBibbeheerderRole() {
        Long bookId = 1L;
        Book book = makeBook();
        BookCopy savedCopy = new BookCopy();
        savedCopy.setId(20L);
        savedCopy.setBook(book);
        savedCopy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(savedCopy);

        ResponseEntity<CopyDto> response = bookCopyController.addCopy(bookId, "bibbeheerder");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookCopyRepository).save(any(BookCopy.class));
    }

    @Test
    void addCopyShouldLinkCopyToCorrectBook() {
        Long bookId = 5L;
        Book book = new Book();
        book.setId(5L);
        book.setTitel("1984");
        
        BookCopy savedCopy = new BookCopy();
        savedCopy.setId(50L);
        savedCopy.setBook(book);
        savedCopy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(savedCopy);

        bookCopyController.addCopy(bookId, "bibbeheerder");

        ArgumentCaptor<BookCopy> captor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getBook().getId());
    }

    @Test
    void addCopyShouldReturnCreatedStatusCode() {
        Long bookId = 1L;
        Book book = makeBook();
        BookCopy savedCopy = new BookCopy();
        savedCopy.setId(10L);
        savedCopy.setBook(book);
        savedCopy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(savedCopy);

        ResponseEntity<CopyDto> response = bookCopyController.addCopy(bookId, "bibbeheerder");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}
