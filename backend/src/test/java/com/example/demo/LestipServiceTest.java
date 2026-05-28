package com.example.demo;

import com.example.demo.dto.LestipDto;
import com.example.demo.dto.UpdateLestipRequest;
import com.example.demo.entities.Book;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookRepository;
import com.example.demo.services.LestipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LestipServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private LestipService lestipService;

    private Book book(Long id, String tip, String author) {
        Book b = new Book();
        b.setId(id);
        b.setLestip(tip);
        b.setLestipAuteur(author);
        return b;
    }

    @Test
    void getLestipShouldThrowNotFoundWhenBookMissing() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.getLestip(1L, "all", "Alice"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getLestipShouldReturnLocalTipWhenPresent() {
        Book local = book(1L, "Lees klassikaal", "Alice");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(local));

        LestipDto dto = lestipService.getLestip(1L, "all", "Alice");

        assertEquals("Lees klassikaal", dto.getLestip());
        assertEquals("Alice", dto.getAuteurNaam());
        assertTrue(dto.getMagVerwijderen(), "author can delete own tip");
    }

    @Test
    void getLestipShouldReturnEmptyDtoWhenScopeIsSchoolAndLocalHasNoTip() {
        Book local = book(1L, null, null);
        local.setIsbn("9780123456789");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(local));

        LestipDto dto = lestipService.getLestip(1L, "school", "Alice");

        assertEquals("", dto.getLestip());
        assertFalse(dto.getMagVerwijderen());
        verify(bookRepository, never()).findByIsbn(any());
    }

    @Test
    void getLestipShouldFallBackToSharedTipByIsbn() {
        Book local = book(1L, null, null);
        local.setIsbn("9780123456789");
        Book sibling = book(99L, "Geweldig boek", "Bob");
        sibling.setIsbn("9780123456789");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(local));
        when(bookRepository.findAllByIsbn("9780123456789")).thenReturn(List.of(sibling));

        LestipDto dto = lestipService.getLestip(1L, "all", "Alice");

        assertEquals("Geweldig boek", dto.getLestip());
        assertEquals("Bob", dto.getAuteurNaam());
        assertFalse(dto.getMagVerwijderen(), "shared tip from another school is read-only");
    }

    @Test
    void getLestipShouldFallBackToSharedTipByGoNumber() {
        Book local = book(1L, null, null);
        local.setGoNumber("GO-00000001");
        Book sibling = book(99L, "GO-tip", "Carol");
        sibling.setGoNumber("GO-00000001");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(local));
        when(bookRepository.findByGoNumber("GO-00000001")).thenReturn(Optional.of(sibling));

        LestipDto dto = lestipService.getLestip(1L, "all", "Carol");

        assertEquals("GO-tip", dto.getLestip());
        assertFalse(dto.getMagVerwijderen());
    }

    @Test
    void updateLestipShouldThrowNotFoundWhenBookMissing() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        req.setLestip("text");
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.updateLestip(1L, req, "Alice"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateLestipShouldThrowConflictWhenAlreadySet() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        req.setLestip("nieuw");
        Book existing = book(1L, "oud", "Bob");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.updateLestip(1L, req, "Alice"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateLestipShouldThrowBadRequestWhenLestipBlank() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        req.setLestip("   ");
        Book existing = book(1L, null, null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.updateLestip(1L, req, "Alice"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateLestipShouldPersistTrimmedTipWithAuthor() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        req.setLestip("  Lees hoofdstuk 1  ");
        Book existing = book(1L, null, null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        LestipDto dto = lestipService.updateLestip(1L, req, "  Alice  ");

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertEquals("Lees hoofdstuk 1", captor.getValue().getLestip());
        assertEquals("Alice", captor.getValue().getLestipAuteur());
        assertEquals("Lees hoofdstuk 1", dto.getLestip());
    }

    @Test
    void deleteLestipShouldThrowNotFoundWhenBookMissing() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.deleteLestip(1L, "Alice"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void deleteLestipShouldThrowNotFoundWhenNoLestip() {
        Book existing = book(1L, null, null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.deleteLestip(1L, "Alice"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void deleteLestipShouldThrowForbiddenWhenNotAuthor() {
        Book existing = book(1L, "tip", "Bob");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.deleteLestip(1L, "Alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void deleteLestipShouldClearLestipWhenAuthor() {
        Book existing = book(1L, "tip", "Alice");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        lestipService.deleteLestip(1L, "Alice");

        assertNull(existing.getLestip());
        assertNull(existing.getLestipAuteur());
        verify(bookRepository).save(existing);
    }

    @Test
    void deleteLestipShouldAllowWhenLegacyLestipHasNoAuthor() {
        Book existing = book(1L, "tip", null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        lestipService.deleteLestip(1L, "Alice");

        assertNull(existing.getLestip());
        verify(bookRepository).save(existing);
    }

    @Test
    void deleteLestipShouldAllowWhenAuthorFormattingDiffers() {
        Book existing = book(1L, "tip", "alice  Wonder");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        lestipService.deleteLestip(1L, "  Alice Wonder  ");

        assertNull(existing.getLestip());
        verify(bookRepository).save(existing);
    }

    @Test
    void updateLestipShouldRejectFileTooLarge() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        // Large data string to exceed the 25MB limit
        String bigData = "YQ==".repeat(30 * 1024 * 1024); // Creating a massive Base64 string
        req.setLestip("{\"text\":\"tip\",\"fileName\":\"test.pdf\",\"fileData\":\"data:application/pdf;base64," + bigData + "\"}");
        
        Book existing = book(1L, null, null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.updateLestip(1L, req, "Alice"));
        assertEquals("FILE_TOO_LARGE", ex.getCode());
    }

    @Test
    void updateLestipShouldRejectInvalidExtension() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        req.setLestip("{\"text\":\"tip\",\"fileName\":\"malicious.exe\",\"fileData\":\"data:application/x-msdownload;base64,YmFk\"}");

        Book existing = book(1L, null, null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> lestipService.updateLestip(1L, req, "Alice"));
        assertEquals("INVALID_FILE_TYPE", ex.getCode());
    }

    @Test
    void updateLestipShouldPersistValidJsonPayload() {
        UpdateLestipRequest req = new UpdateLestipRequest();
        String payload = "{\"text\":\"Check this out\",\"fileName\":\"lesson.pdf\",\"fileData\":\"data:application/pdf;base64,U01PTA==\"}";
        req.setLestip(payload);

        Book existing = book(1L, null, null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        LestipDto dto = lestipService.updateLestip(1L, req, "Alice");

        assertEquals("Check this out", dto.getLestip());
        assertEquals("lesson.pdf", dto.getFileName());
    }
}
