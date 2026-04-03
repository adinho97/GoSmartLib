package com.example.demo;

import com.example.demo.controllers.WishlistController;
import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.WishlistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    @Mock
    private WishlistService wishlistService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @InjectMocks
    private WishlistController wishlistController;

    @Test
    void updateWishlist_ShouldResetLastNotifiedAt_WhenEnablingNotifications() {
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

        WishlistDto dto = new WishlistDto();
        dto.setNotificationEnabled(true);

        when(wishlistRepository.findById(5L)).thenReturn(Optional.of(wishlist));
        when(bookCopyRepository.countByBook_IdAndStatus(11L, BookCopy.CopyStatus.AVAILABLE)).thenReturn(0L);
        when(bookCopyRepository.countByBook_Id(11L)).thenReturn(3L);
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = wishlistController.updateWishlist(5L, dto, "test-sub");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(WishlistDto.class, response.getBody());

        WishlistDto responseDto = (WishlistDto) response.getBody();
        assertNotNull(responseDto);
        assertTrue(responseDto.isNotificationEnabled());
        assertNull(responseDto.getLastNotifiedAt());

        ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).save(captor.capture());
        assertTrue(captor.getValue().isNotificationEnabled());
        assertNull(captor.getValue().getLastNotifiedAt());
    }

    @Test
    void updateWishlist_ShouldReturnBadRequest_WhenEnablingAndBookIsAvailable() {
        Book book = new Book();
        book.setId(12L);

        Wishlist wishlist = new Wishlist();
        wishlist.setId(7L);
        wishlist.setBook(book);

        WishlistDto dto = new WishlistDto();
        dto.setNotificationEnabled(true);

        when(wishlistRepository.findById(7L)).thenReturn(Optional.of(wishlist));
        when(bookCopyRepository.countByBook_IdAndStatus(12L, BookCopy.CopyStatus.AVAILABLE)).thenReturn(2L);

        ResponseEntity<?> response = wishlistController.updateWishlist(7L, dto, "test-sub");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void updateWishlist_ShouldReturnUnauthorized_WhenUserSubMissing() {
        WishlistDto dto = new WishlistDto();
        dto.setNotificationEnabled(true);

        ResponseEntity<?> response = wishlistController.updateWishlist(1L, dto, "");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(wishlistRepository, bookCopyRepository);
    }
}
