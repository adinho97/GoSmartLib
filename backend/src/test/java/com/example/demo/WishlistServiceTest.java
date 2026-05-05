package com.example.demo;

import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.WishlistService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService Tests")
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private AppUser testUser;
    private Book testBook;
    private Book testBook2;
    private Wishlist testWishlist;

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

        testBook2 = new Book();
        testBook2.setId(2L);
        testBook2.setTitel("Another Book");
        testBook2.setAuteur("Another Author");

        testWishlist = new Wishlist();
        testWishlist.setId(1L);
        testWishlist.setUser(testUser);
        testWishlist.setBook(testBook);
    }

    @Test
    @DisplayName("Should successfully add book to wishlist")
    void testAddToWishlistSuccess() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(wishlistRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        assertDoesNotThrow(() -> wishlistService.addToWishlist(1L, testUser));

        verify(bookRepository, times(1)).findById(1L);
        verify(wishlistRepository, times(1)).findByUserAndBook(testUser, testBook);
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should throw exception when book not found for add")
    void testAddToWishlistBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wishlistService.addToWishlist(999L, testUser));

        assertEquals("Boek niet gevonden", exception.getMessage());
        verify(bookRepository, times(1)).findById(999L);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should throw exception when book already on wishlist")
    void testAddToWishlistDuplicate() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(wishlistRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.of(testWishlist));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> wishlistService.addToWishlist(1L, testUser));

        assertEquals("Boek staat al op verlanglijst", exception.getMessage());
        verify(bookRepository, times(1)).findById(1L);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should successfully remove book from wishlist")
    void testRemoveFromWishlistSuccess() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(1L, testUser));

        verify(bookRepository, times(1)).findById(1L);
        verify(wishlistRepository, times(1)).deleteByUserAndBook(testUser, testBook);
    }

    @Test
    @DisplayName("Should throw exception when book not found for remove")
    void testRemoveFromWishlistBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wishlistService.removeFromWishlist(999L, testUser));

        assertEquals("Boek niet gevonden", exception.getMessage());
        verify(wishlistRepository, never()).deleteByUserAndBook(any(), any());
    }

    @Test
    @DisplayName("Should return user's wishlist with multiple books")
    void testGetUserWishlistWithBooks() {
        Wishlist wishlist2 = new Wishlist();
        wishlist2.setId(2L);
        wishlist2.setUser(testUser);
        wishlist2.setBook(testBook2);

        when(wishlistRepository.findByUser(testUser))
                .thenReturn(Arrays.asList(testWishlist, wishlist2));

        List<WishlistDto> result = wishlistService.getUserWishlist(testUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(wishlistRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("Should return empty wishlist when user has no books")
    void testGetUserWishlistEmpty() {
        when(wishlistRepository.findByUser(testUser)).thenReturn(Collections.emptyList());

        List<WishlistDto> result = wishlistService.getUserWishlist(testUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(wishlistRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("Should return true when book is on wishlist")
    void testIsWishlistedTrue() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(wishlistRepository.findByUserAndBook(testUser, testBook))
                .thenReturn(Optional.of(testWishlist));

        boolean result = wishlistService.isWishlisted(1L, testUser);

        assertTrue(result);
        verify(bookRepository, times(1)).findById(1L);
        verify(wishlistRepository, times(1)).findByUserAndBook(testUser, testBook);
    }

    @Test
    @DisplayName("Should return false when book is not on wishlist")
    void testIsWishlistedFalse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(wishlistRepository.findByUserAndBook(testUser, testBook))
                .thenReturn(Optional.empty());

        boolean result = wishlistService.isWishlisted(1L, testUser);

        assertFalse(result);
        verify(bookRepository, times(1)).findById(1L);
        verify(wishlistRepository, times(1)).findByUserAndBook(testUser, testBook);
    }

    @Test
    @DisplayName("Should throw exception when checking wishlist with invalid book")
    void testIsWishlistedBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wishlistService.isWishlisted(999L, testUser));

        assertEquals("Boek niet gevonden", exception.getMessage());
        verify(wishlistRepository, never()).findByUserAndBook(any(), any());
    }
}
