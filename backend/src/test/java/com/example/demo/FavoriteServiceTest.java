package com.example.demo;

import com.example.demo.dto.FavoriteDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Favorite;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.services.FavoriteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService Tests")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private AppUser testUser;
    private Book testBook;
    private Book testBook2;
    private Favorite testFavorite;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setSub("test-sub-123");
        testUser.setRole("leerling");

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitel("Test Book");
        testBook.setAuteur("Test Author");
        testBook.setCover("test-cover.jpg");

        testBook2 = new Book();
        testBook2.setId(2L);
        testBook2.setTitel("Another Book");
        testBook2.setAuteur("Another Author");
        testBook2.setCover("another-cover.jpg");

        testFavorite = new Favorite();
        testFavorite.setId(1L);
        testFavorite.setUser(testUser);
        testFavorite.setBook(testBook);
        testFavorite.setAddedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully add book to favorites")
    void testAddToFavoritesSuccess() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(testFavorite);

        assertDoesNotThrow(() -> favoriteService.addToFavorites(1L, testUser));

        verify(bookRepository, times(1)).findById(1L);
        verify(favoriteRepository, times(1)).findByUserAndBook(testUser, testBook);
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw exception when book not found for add")
    void testAddToFavoritesBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> favoriteService.addToFavorites(999L, testUser));

        assertEquals("Boek niet gevonden", exception.getMessage());
        verify(bookRepository, times(1)).findById(999L);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw exception when book already in favorites")
    void testAddToFavoritesDuplicate() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.of(testFavorite));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> favoriteService.addToFavorites(1L, testUser));

        assertEquals("Boek staat al in favorieten", exception.getMessage());
        verify(bookRepository, times(1)).findById(1L);
        verify(favoriteRepository, times(1)).findByUserAndBook(testUser, testBook);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should successfully remove book from favorites")
    void testRemoveFromFavoritesSuccess() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        assertDoesNotThrow(() -> favoriteService.removeFromFavorites(1L, testUser));

        verify(bookRepository, times(1)).findById(1L);
        verify(favoriteRepository, times(1)).deleteByUserAndBook(testUser, testBook);
    }

    @Test
    @DisplayName("Should throw exception when book not found for remove")
    void testRemoveFromFavoritesBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> favoriteService.removeFromFavorites(999L, testUser));

        assertEquals("Boek niet gevonden", exception.getMessage());
        verify(bookRepository, times(1)).findById(999L);
        verify(favoriteRepository, never()).deleteByUserAndBook(any(), any());
    }

    @Test
    @DisplayName("Should return user favorites")
    void testGetUserFavorites() {
        Favorite favorite2 = new Favorite();
        favorite2.setId(2L);
        favorite2.setUser(testUser);
        favorite2.setBook(testBook2);
        favorite2.setAddedAt(LocalDateTime.now());

        when(favoriteRepository.findByUser(testUser)).thenReturn(Arrays.asList(testFavorite, favorite2));

        List<FavoriteDto> result = favoriteService.getUserFavorites(testUser);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(1L, result.get(0).getBookId());
        assertEquals("Test Book", result.get(0).getTitel());
        assertEquals("Test Author", result.get(0).getAuteur());
        assertEquals("test-cover.jpg", result.get(0).getCover());

        verify(favoriteRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("Should return empty list when no favorites")
    void testGetUserFavoritesEmpty() {
        when(favoriteRepository.findByUser(testUser)).thenReturn(Collections.emptyList());

        List<FavoriteDto> result = favoriteService.getUserFavorites(testUser);

        assertTrue(result.isEmpty());
        verify(favoriteRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("Should return true when book is favorited")
    void testIsFavoritedTrue() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.of(testFavorite));

        boolean result = favoriteService.isFavorited(1L, testUser);

        assertTrue(result);
        verify(bookRepository, times(1)).findById(1L);
        verify(favoriteRepository, times(1)).findByUserAndBook(testUser, testBook);
    }

    @Test
    @DisplayName("Should return false when book is not favorited")
    void testIsFavoritedFalse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.empty());

        boolean result = favoriteService.isFavorited(1L, testUser);

        assertFalse(result);
        verify(bookRepository, times(1)).findById(1L);
        verify(favoriteRepository, times(1)).findByUserAndBook(testUser, testBook);
    }

    @Test
    @DisplayName("Should throw exception when book not found for isFavorited")
    void testIsFavoritedBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> favoriteService.isFavorited(999L, testUser));

        assertEquals("Boek niet gevonden", exception.getMessage());
        verify(bookRepository, times(1)).findById(999L);
        verify(favoriteRepository, never()).findByUserAndBook(any(), any());
    }
}