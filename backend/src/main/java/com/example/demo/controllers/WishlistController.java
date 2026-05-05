package com.example.demo.controllers;

import com.example.demo.dto.WishlistAddRequest;
import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.exception.ApiException;
import com.example.demo.services.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/verlanglijst")
public class WishlistController {

    private final WishlistService wishlistService;
    private final AppUserRepository appUserRepository;
    private final WishlistRepository wishlistRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    public WishlistController(WishlistService wishlistService, AppUserRepository appUserRepository,
            WishlistRepository wishlistRepository, BookCopyRepository bookCopyRepository,
            BookRepository bookRepository) {
        this.wishlistService = wishlistService;
        this.appUserRepository = appUserRepository;
        this.wishlistRepository = wishlistRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
    }

    private AppUser getUserFromHeader(String userSub) {
        return appUserRepository.findBySub(userSub)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden"));
    }

    private String requireUserSub(String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            throw new ApiException("Niet ingelogd", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        return userSub;
    }

    @PostMapping
    public ResponseEntity<Void> addToWishlist(
            @Valid @RequestBody WishlistAddRequest request,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        Long bookId = Objects.requireNonNull(request.getBookId(), "bookId is required");
        AppUser user = getUserFromHeader(requireUserSub(userSub));
        if (user.getSchool() != null) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null || book.getSchool() == null
                    || !Objects.requireNonNull(book.getSchool().getId(), "schoolId is required")
                            .equals(Objects.requireNonNull(user.getSchool().getId(), "schoolId is required"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        wishlistService.addToWishlist(bookId, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        AppUser user = getUserFromHeader(requireUserSub(userSub));
        wishlistService.removeFromWishlist(bookId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistDto>> getUserWishlist(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        AppUser user = getUserFromHeader(requireUserSub(userSub));
        List<WishlistDto> wishlist = wishlistService.getUserWishlist(user);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/{bookId}/check")
    public ResponseEntity<Boolean> isWishlisted(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        AppUser user = getUserFromHeader(requireUserSub(userSub));
        boolean isWishlisted = wishlistService.isWishlisted(bookId, user);
        return ResponseEntity.ok(isWishlisted);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateWishlist(
            @PathVariable Long id,
            @RequestBody WishlistDto dto,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        requireUserSub(userSub);
        Wishlist wishlist = wishlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist not found"));
        Long bookId = Objects.requireNonNull(wishlist.getBook().getId(), "bookId is required");

        // Validation: Can only enable notifications if book is unavailable
        if (dto.isNotificationEnabled()) {
            long availableCopies = bookCopyRepository.countByBook_IdAndStatus(
                    bookId,
                    BookCopy.CopyStatus.AVAILABLE);

            if (availableCopies > 0) {
                throw new IllegalStateException("Cannot enable notifications for available book");
            }
        }

        wishlist.setNotificationEnabled(dto.isNotificationEnabled());
        if (dto.isNotificationEnabled()) {
            wishlist.setLastNotifiedAt(null);
        }
        Wishlist updated = wishlistRepository.save(wishlist);
        long totalCopies = bookCopyRepository.countByBook_Id(bookId);
        long availableCopies = bookCopyRepository.countByBook_IdAndStatus(
            bookId,
                BookCopy.CopyStatus.AVAILABLE);

        WishlistDto responseDto = new WishlistDto(
                updated.getId(),
            bookId,
                updated.getBook().getTitel(),
                updated.getBook().getAuteur(),
                updated.getBook().getCover(),
                updated.getAddedAt(),
                updated.isNotificationEnabled(),
                updated.getLastNotifiedAt(),
                (int) availableCopies,
                (int) totalCopies);

        return ResponseEntity.ok(responseDto);
    }

}
