package com.example.demo;

import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.School;
import com.example.demo.entities.Wishlist;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.WishlistService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
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

    @Mock
    private BookCopyRepository bookCopyRepository;

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

    @Test
    @DisplayName("Should throw FORBIDDEN when book belongs to a different school than the user")
    void testAddToWishlistRejectsDifferentSchool() {
        School userSchool = new School();
        userSchool.setId(100L);
        testUser.setSchool(userSchool);

        School otherSchool = new School();
        otherSchool.setId(200L);
        testBook.setSchool(otherSchool);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        ApiException ex = assertThrows(ApiException.class,
                () -> wishlistService.addToWishlist(1L, testUser));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when book has no school but user has one")
    void testAddToWishlistRejectsBookWithoutSchool() {
        School userSchool = new School();
        userSchool.setId(100L);
        testUser.setSchool(userSchool);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        ApiException ex = assertThrows(ApiException.class,
                () -> wishlistService.addToWishlist(1L, testUser));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should allow add when book and user share the same school")
    void testAddToWishlistAllowsSameSchool() {
        School school = new School();
        school.setId(100L);
        testUser.setSchool(school);
        testBook.setSchool(school);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(wishlistRepository.findByUserAndBook(testUser, testBook)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        assertDoesNotThrow(() -> wishlistService.addToWishlist(1L, testUser));
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should reset lastNotifiedAt when enabling notifications")
    void testUpdateNotificationEnabledResetsLastNotifiedAt() {
        Book book = new Book();
        book.setId(11L);
        book.setTitel("Dune");
        book.setAuteur("Frank Herbert");
        book.setCover("cover.jpg");

        Wishlist wishlist = new Wishlist();
        wishlist.setId(5L);
        wishlist.setBook(book);
        wishlist.setAddedAt(LocalDateTime.now().minusDays(5));
        wishlist.setNotificationEnabled(false);
        wishlist.setLastNotifiedAt(LocalDateTime.now().minusHours(2));

        when(wishlistRepository.findById(5L)).thenReturn(Optional.of(wishlist));
        when(bookCopyRepository.findByBook_Id(11L)).thenReturn(Collections.emptyList());
        when(bookCopyRepository.countByBook_Id(11L)).thenReturn(3L);
        when(bookCopyRepository.countByBook_IdAndStatus(11L, BookCopy.CopyStatus.AVAILABLE)).thenReturn(0L);
        when(wishlistRepository.save(wishlist)).thenReturn(wishlist);

        WishlistDto responseDto = wishlistService.updateNotificationEnabled(5L, true);

        assertTrue(responseDto.isNotificationEnabled());
        assertNull(responseDto.getLastNotifiedAt());

        ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).save(captor.capture());
        assertTrue(captor.getValue().isNotificationEnabled());
        assertNull(captor.getValue().getLastNotifiedAt());
    }

    @Test
    @DisplayName("Should throw IllegalState when enabling notifications and book has available copies")
    void testUpdateNotificationEnabledRejectsWhenAvailable() {
        Book book = new Book();
        book.setId(12L);

        Wishlist wishlist = new Wishlist();
        wishlist.setId(7L);
        wishlist.setBook(book);

        BookCopy availableCopy = new BookCopy();
        availableCopy.setStatus(BookCopy.CopyStatus.AVAILABLE);

        when(wishlistRepository.findById(7L)).thenReturn(Optional.of(wishlist));
        when(bookCopyRepository.findByBook_Id(12L)).thenReturn(List.of(availableCopy, availableCopy));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> wishlistService.updateNotificationEnabled(7L, true));

        assertEquals("Cannot enable notifications for available book", ex.getMessage());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }
}
