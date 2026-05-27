package com.example.demo;

import com.example.demo.dto.CopyDto;
import com.example.demo.dto.UpdateCopyStateRequest;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.BookCopyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookCopyServiceTest {

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private BookCopyService bookCopyService;

    private @NonNull Book makeBook() {
        Book b = new Book();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
    }

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

        CopyDto dto = bookCopyService.addCopy(bookId, BookCopy.CopyCondition.GOOD);

        assertEquals(10L, dto.getId());
        assertEquals(BookCopy.CopyStatus.AVAILABLE, dto.getStatus());

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

        bookCopyService.addCopy(bookId, BookCopy.CopyCondition.GOOD);

        verify(bookCopyRepository).save(any(BookCopy.class));
    }

    @Test
    void addCopyShouldThrowNotFoundWhenBookDoesNotExist() {
        Long bookId = 999L;

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> bookCopyService.addCopy(bookId, BookCopy.CopyCondition.GOOD));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(bookCopyRepository, never()).save(any());
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

        bookCopyService.addCopy(bookId, BookCopy.CopyCondition.GOOD);

        ArgumentCaptor<BookCopy> captor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getBook().getId());
    }

    @Test
    void getSummaryShouldCountAvailableAndDamagedAsAvailable() {
        Long bookId = 1L;
        BookCopy available = new BookCopy();
        available.setStatus(BookCopy.CopyStatus.AVAILABLE);
        BookCopy damaged = new BookCopy();
        damaged.setStatus(BookCopy.CopyStatus.DAMAGED);
        BookCopy loaned = new BookCopy();
        loaned.setStatus(BookCopy.CopyStatus.LOANED);

        when(bookCopyRepository.countByBook_Id(bookId)).thenReturn(3L);
        when(bookCopyRepository.findByBook_Id(bookId)).thenReturn(List.of(available, damaged, loaned));

        Map<String, Long> summary = bookCopyService.getSummary(bookId);

        assertEquals(3L, summary.get("total"));
        assertEquals(2L, summary.get("available"));
    }

    @Test
    void updateCopyStateShouldRejectLoanedSourceWithConflict() {
        Long copyId = 7L;
        BookCopy copy = new BookCopy();
        copy.setId(copyId);
        copy.setStatus(BookCopy.CopyStatus.LOANED);

        UpdateCopyStateRequest request = new UpdateCopyStateRequest();
        request.setStatus(BookCopy.CopyStatus.AVAILABLE);
        request.setCondition(BookCopy.CopyCondition.GOOD);

        when(bookCopyRepository.findById(copyId)).thenReturn(Optional.of(copy));

        ApiException ex = assertThrows(ApiException.class,
                () -> bookCopyService.updateCopyState(copyId, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(bookCopyRepository, never()).save(any());
    }

    @Test
    void updateCopyStateShouldRejectLoanedTargetWithBadRequest() {
        Long copyId = 7L;
        UpdateCopyStateRequest request = new UpdateCopyStateRequest();
        request.setStatus(BookCopy.CopyStatus.LOANED);
        request.setCondition(BookCopy.CopyCondition.GOOD);

        ApiException ex = assertThrows(ApiException.class,
                () -> bookCopyService.updateCopyState(copyId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(bookCopyRepository);
    }

    @Test
    void deleteCopyShouldRejectLoanedWithConflict() {
        Long copyId = 7L;
        BookCopy copy = new BookCopy();
        copy.setId(copyId);
        copy.setStatus(BookCopy.CopyStatus.LOANED);

        when(bookCopyRepository.findById(copyId)).thenReturn(Optional.of(copy));

        ApiException ex = assertThrows(ApiException.class, () -> bookCopyService.deleteCopy(copyId));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(bookCopyRepository, never()).deleteById(any());
        verify(loanRepository, never()).deleteByCopy_Id(any());
    }

    @Test
    void deleteCopyShouldRemoveLoansThenCopy() {
        Long copyId = 7L;
        BookCopy copy = new BookCopy();
        copy.setId(copyId);
        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(bookCopyRepository.findById(copyId)).thenReturn(Optional.of(copy));

        bookCopyService.deleteCopy(copyId);

        verify(loanRepository).deleteByCopy_Id(copyId);
        verify(bookCopyRepository).deleteById(copyId);
    }
}
