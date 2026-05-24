package com.example.demo;

import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.services.AuthService;
import com.example.demo.services.SmartschoolMessageService;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.BookAvailabilityNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class BookAvailabilityNotificationServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private AuthService authService;

    @Mock
    private SmartschoolMessageService messageService;

    @Mock
    private SmartschoolProperties smartschoolProperties;

    @InjectMocks
    private BookAvailabilityNotificationService notificationService;

    @Test
    void notifyWishlistersThatBookIsAvailable_ShouldDisableNotificationAfterSuccessfulSend() {
        Book book = new Book();
        book.setId(1L);
        book.setTitel("Kruistocht in spijkerbroek");

        AppUser user = new AppUser();
        user.setId(4L);
        user.setSub("sub-4");

        Wishlist wishlist = new Wishlist();
        wishlist.setId(8L);
        wishlist.setUser(user);
        wishlist.setBook(book);
        wishlist.setNotificationEnabled(true);
        wishlist.setLastNotifiedAt(null);

        SmartschoolUserInfo userInfo = new SmartschoolUserInfo();
        userInfo.setAccessToken("access-token");

        when(wishlistRepository.findByBook_IdAndNotificationEnabledTrue(1L)).thenReturn(List.of(wishlist));
        when(authService.getUserInfoBySub("sub-4")).thenReturn(Mono.just(userInfo));
        when(smartschoolProperties.getApiBaseUrl()).thenReturn("https://school.example");
        when(messageService.sendMessage(eq("access-token"), any(SmartschoolMessageRequest.class)))
                .thenReturn(Mono.just("ok"));
        when(wishlistRepository.save(any(Wishlist.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyWishlistersThatBookIsAvailable(book);

        verify(messageService, times(1)).sendMessage(eq("access-token"), any(SmartschoolMessageRequest.class));

        ArgumentCaptor<Wishlist> savedWishlist = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository, times(1)).save(savedWishlist.capture());

        assertFalse(savedWishlist.getValue().isNotificationEnabled());
        assertNotNull(savedWishlist.getValue().getLastNotifiedAt());
    }

    @Test
    void notifyWishlistersThatBookIsAvailable_ShouldSkipIfAlreadyNotifiedInLast24h() {
        Book book = new Book();
        book.setId(2L);
        book.setTitel("Het smelt");

        AppUser user = new AppUser();
        user.setId(5L);
        user.setSub("sub-5");

        Wishlist wishlist = new Wishlist();
        wishlist.setId(9L);
        wishlist.setUser(user);
        wishlist.setBook(book);
        wishlist.setNotificationEnabled(true);
        wishlist.setLastNotifiedAt(LocalDateTime.now().minusHours(2));

        when(wishlistRepository.findByBook_IdAndNotificationEnabledTrue(2L)).thenReturn(List.of(wishlist));

        notificationService.notifyWishlistersThatBookIsAvailable(book);

        verifyNoInteractions(authService, messageService);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void notifyWishlistersThatBookIsAvailable_ShouldDoNothingWhenNoWishlistersEnabled() {
        Book book = new Book();
        book.setId(3L);
        book.setTitel("Elckerlijc");

        when(wishlistRepository.findByBook_IdAndNotificationEnabledTrue(3L)).thenReturn(Collections.emptyList());

        notificationService.notifyWishlistersThatBookIsAvailable(book);

        verifyNoInteractions(authService, messageService);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }
}
