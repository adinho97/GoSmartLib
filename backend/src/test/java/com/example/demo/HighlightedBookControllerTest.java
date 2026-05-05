package com.example.demo;

import com.example.demo.config.HighlightedBook;
import com.example.demo.config.HighlightedBookController;
import com.example.demo.config.HighlightedBookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighlightedBookControllerTest {

    @Mock
    private HighlightedBookRepository repository;

    @InjectMocks
    private HighlightedBookController controller;

    @Test
    void getHighlightedBookIds_shouldMapItemsToBookIds() {
        HighlightedBook first = new HighlightedBook(13L, 5L);
        first.setId(1L);
        HighlightedBook second = new HighlightedBook(14L, 5L);
        second.setId(2L);

        when(repository.findBySchoolId(5L)).thenReturn(List.of(first, second));

        List<Long> result = controller.getHighlightedBookIds(5L);

        assertEquals(List.of(13L, 14L), result);
        verify(repository).findBySchoolId(5L);
    }

    @Test
    void toggleHighlight_shouldDeleteExistingItemAndReturnFalse() {
        HighlightedBook existing = new HighlightedBook(77L, 2L);
        existing.setId(8L);
        when(repository.findByBookIdAndSchoolId(77L, 2L)).thenReturn(Optional.of(existing));

        boolean result = controller.toggleHighlight(77L, 2L);

        assertFalse(result);
        verify(repository).delete(existing);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void toggleHighlight_shouldCreateNewItemAndReturnTrue() {
        when(repository.findByBookIdAndSchoolId(88L, 6L)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(HighlightedBook.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = controller.toggleHighlight(88L, 6L);

        assertTrue(result);
        ArgumentCaptor<HighlightedBook> captor = ArgumentCaptor.forClass(HighlightedBook.class);
        verify(repository).save(captor.capture());
        assertEquals(88L, captor.getValue().getBookId());
        assertEquals(6L, captor.getValue().getSchoolId());
    }

    @Test
    void isHighlighted_shouldReturnRepositoryStatus() {
        when(repository.existsByBookIdAndSchoolId(19L, 4L)).thenReturn(true);

        boolean result = controller.isHighlighted(19L, 4L);

        assertTrue(result);
        verify(repository).existsByBookIdAndSchoolId(19L, 4L);
    }
}