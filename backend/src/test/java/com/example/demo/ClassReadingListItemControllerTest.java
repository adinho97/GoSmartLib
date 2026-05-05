package com.example.demo;

import com.example.demo.config.ClassReadingListItem;
import com.example.demo.config.ClassReadingListItemController;
import com.example.demo.config.ClassReadingListItemRepository;
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
class ClassReadingListItemControllerTest {

    @Mock
    private ClassReadingListItemRepository repository;

    @InjectMocks
    private ClassReadingListItemController controller;

    @Test
    void getHighlightedBookIds_shouldMapItemsToBookIds() {
        ClassReadingListItem first = new ClassReadingListItem(11L, 3L);
        first.setId(1L);
        ClassReadingListItem second = new ClassReadingListItem(22L, 3L);
        second.setId(2L);

        when(repository.findBySchoolId(3L)).thenReturn(List.of(first, second));

        List<Long> result = controller.getHighlightedBookIds(3L);

        assertEquals(List.of(11L, 22L), result);
        verify(repository).findBySchoolId(3L);
    }

    @Test
    void toggleHighlight_shouldDeleteExistingItemAndReturnFalse() {
        ClassReadingListItem existing = new ClassReadingListItem(99L, 7L);
        existing.setId(8L);
        when(repository.findByBookIdAndSchoolId(99L, 7L)).thenReturn(Optional.of(existing));

        boolean result = controller.toggleHighlight(99L, 7L);

        assertFalse(result);
        verify(repository).delete(existing);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void toggleHighlight_shouldCreateNewItemAndReturnTrue() {
        when(repository.findByBookIdAndSchoolId(55L, 9L)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(ClassReadingListItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = controller.toggleHighlight(55L, 9L);

        assertTrue(result);
        ArgumentCaptor<ClassReadingListItem> captor = ArgumentCaptor.forClass(ClassReadingListItem.class);
        verify(repository).save(captor.capture());
        assertEquals(55L, captor.getValue().getBookId());
        assertEquals(9L, captor.getValue().getSchoolId());
    }

    @Test
    void isHighlighted_shouldReturnRepositoryStatus() {
        when(repository.existsByBookIdAndSchoolId(12L, 4L)).thenReturn(true);

        boolean result = controller.isHighlighted(12L, 4L);

        assertTrue(result);
        verify(repository).existsByBookIdAndSchoolId(12L, 4L);
    }
}