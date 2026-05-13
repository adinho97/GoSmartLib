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
                    request.setBody(buildAvailabilityHtml(
                            userInfo.getName() != null ? userInfo.getName() : "Lezer",
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

    // ── HTML builders ──────────────────────────────────────────────────────────

    private String buildAvailabilityHtml(String name, String title) {
        return String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                  <div style="background-color: #1a3a5c; padding: 24px 32px;">
                    <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: normal; letter-spacing: 0.5px;">
                       Bibliotheek — Boek beschikbaar
                    </h1>
                  </div>
                  <div style="padding: 28px 32px;">
                    <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong></p>
                    <p style="margin: 0 0 24px; font-size: 15px; color: #333;">
                      Goed nieuws! Het boek dat u op uw verlanglijst had staan, is nu beschikbaar in de bibliotheek.
                    </p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Arial, sans-serif;">
                      <thead>
                        <tr style="background-color: #1a3a5c; color: #fff;">
                          <th style="padding: 10px 12px; text-align: left; font-size: 13px;">Titel</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr style="background-color: #f9f9f9;">
                          <td style="padding: 8px 12px; font-size: 14px; color: #222;">%s</td>
                        </tr>
                      </tbody>
                    </table>
                    <div style="background-color: #e8f5e9; border-left: 4px solid #2e7d32; padding: 14px 18px; border-radius: 3px; margin-bottom: 24px;">
                      <p style="margin: 0; font-size: 14px; color: #1b5e20;"><strong>Status:</strong> Nu beschikbaar!</p>
                      <p style="margin: 6px 0 0; font-size: 13px; color: #388e3c;">Wees er snel bij om het boek op te halen.</p>
                    </div>
                    <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten<br><strong>De bibliotheek</strong></p>
                  </div>
                  <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                    <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                  </div>
                </div>
                """, escapeHtml(name), escapeHtml(title));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
