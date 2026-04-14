package com.example.demo.services;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolMessageService;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.entities.Book;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookAvailabilityNotificationService {

    private final WishlistRepository wishlistRepository;
    private final AuthService authService;
    private final SmartschoolMessageService messageService;
    private final SmartschoolProperties smartschoolProperties;
    private static final Logger logger = LoggerFactory.getLogger(BookAvailabilityNotificationService.class);

    public BookAvailabilityNotificationService(
            WishlistRepository wishlistRepository,
            AuthService authService,
            SmartschoolMessageService messageService,
            SmartschoolProperties smartschoolProperties) {
        this.wishlistRepository = wishlistRepository;
        this.authService = authService;
        this.messageService = messageService;
        this.smartschoolProperties = smartschoolProperties;
    }

    public void notifyWishlistersThatBookIsAvailable(Book book) {
        List<Wishlist> wishlists = wishlistRepository.findByBook_IdAndNotificationEnabledTrue(book.getId());

        if (wishlists.isEmpty()) {
            logger.info("No wishlists with notifications enabled for book: {}", book.getTitel());
            return;
        }

        Flux.fromIterable(wishlists)
                .flatMap(this::sendNotificationToWishlister)
                .subscribe(
                        result -> logger.info("Notification sent for book: {}", book.getTitel()),
                        error -> logger.error("Error sending notifications for book: {}", book.getTitel(), error));
    }

    private Mono<String> sendNotificationToWishlister(Wishlist wishlist) {
        // Prevent duplicate notifications within 24 hours
        if (wishlist.getLastNotifiedAt() != null &&
                wishlist.getLastNotifiedAt().isAfter(LocalDateTime.now().minusHours(24))) {
            logger.info("Skipping notification - already notified within 24h for wishlist id: {}", wishlist.getId());
            return Mono.empty();
        }

        return authService.getUserInfoBySub(wishlist.getUser().getSub())
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest request = new SmartschoolMessageRequest();
                    request.setPlatformUrl(userInfo.getPlatform() != null ? userInfo.getPlatform()
                            : smartschoolProperties.getApiBaseUrl());
                    request.setSubject("Boek beschikbaar: " + wishlist.getBook().getTitel());
                    request.setBody(String.format(
                            "Beste,\n\nJe gewenste boek '%s' is nu beschikbaar in de bibliotheek!\n\nWees er snel bij!",
                            wishlist.getBook().getTitel()));

                    return messageService.sendMessage(userInfo.getAccessToken(), request);
                })
                .doOnSuccess(response -> {
                    wishlist.setLastNotifiedAt(LocalDateTime.now());
                    wishlist.setNotificationEnabled(false);
                    wishlistRepository.save(wishlist);
                    logger.info("Notification sent successfully to user: {} for book: {}",
                            wishlist.getUser().getId(), wishlist.getBook().getTitel());
                })
                .doOnError(error -> logger.error("Failed to send notification to user: {} for book: {}",
                        wishlist.getUser().getId(), wishlist.getBook().getTitel(), error))
                .onErrorResume(e -> Mono.empty()); // Don't fail entire batch if one user fails
    }
}
