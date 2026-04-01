package com.example.demo.controllers;

import com.example.demo.dto.WishlistAddRequest;
import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/verlanglijst")
public class WishlistController {

    private final WishlistService wishlistService;
    private final AppUserRepository appUserRepository;
    private final WishlistRepository wishlistRepository;
    private final BookCopyRepository bookCopyRepository;

    public WishlistController(WishlistService wishlistService, AppUserRepository appUserRepository,
            WishlistRepository wishlistRepository, BookCopyRepository bookCopyRepository) {
        this.wishlistService = wishlistService;
        this.appUserRepository = appUserRepository;
        this.wishlistRepository = wishlistRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    private AppUser getUserFromHeader(String userSub) {
        return appUserRepository.findBySub(userSub)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden"));
    }

    @PostMapping
    public ResponseEntity<Void> addToWishlist(
            @Valid @RequestBody WishlistAddRequest request,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            wishlistService.addToWishlist(request.getBookId(), user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            wishlistService.removeFromWishlist(bookId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<WishlistDto>> getUserWishlist(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            List<WishlistDto> wishlist = wishlistService.getUserWishlist(user);
            return ResponseEntity.ok(wishlist);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{bookId}/check")
    public ResponseEntity<Boolean> isWishlisted(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            boolean isWishlisted = wishlistService.isWishlisted(bookId, user);
            return ResponseEntity.ok(isWishlisted);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateWishlist(
            @PathVariable Long id,
            @RequestBody WishlistDto dto,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Wishlist wishlist = wishlistRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Wishlist not found"));

            // Validation: Can only enable notifications if book is unavailable
            if (dto.isNotificationEnabled()) {
                long availableCopies = bookCopyRepository.countByBook_IdAndStatus(
                        wishlist.getBook().getId(),
                        BookCopy.CopyStatus.AVAILABLE);

                if (availableCopies > 0) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Cannot enable notifications for available book"));
                }
            }

            wishlist.setNotificationEnabled(dto.isNotificationEnabled());
            Wishlist updated = wishlistRepository.save(wishlist);
            long totalCopies = bookCopyRepository.countByBook_Id(updated.getBook().getId());
            long availableCopies = bookCopyRepository.countByBook_IdAndStatus(
                    updated.getBook().getId(),
                    BookCopy.CopyStatus.AVAILABLE);

            WishlistDto responseDto = new WishlistDto(
                    updated.getId(),
                    updated.getBook().getId(),
                    updated.getBook().getTitel(),
                    updated.getBook().getAuteur(),
                    updated.getBook().getCover(),
                    updated.getAddedAt(),
                    updated.isNotificationEnabled(),
                    updated.getLastNotifiedAt(),
                    (int) availableCopies,
                    (int) totalCopies);

            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
