package com.example.demo.controllers;

import com.example.demo.dto.WishlistAddRequest;
import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.services.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/verlanglijst")
@SuppressWarnings("null")
public class WishlistController {

    private final WishlistService wishlistService;
    private final AppUserRepository appUserRepository;

    public WishlistController(WishlistService wishlistService, AppUserRepository appUserRepository) {
        this.wishlistService = wishlistService;
        this.appUserRepository = appUserRepository;
    }

    private AppUser getUserFromSub(String userSub) {
        return appUserRepository.findBySub(userSub)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden"));
    }

    @PostMapping
    public ResponseEntity<Void> addToWishlist(
            @Valid @RequestBody WishlistAddRequest request,
            Authentication authentication) {
        Long bookId = Objects.requireNonNull(request.getBookId(), "bookId is required");
        AppUser user = getUserFromSub(authentication.getName());
        wishlistService.addToWishlist(bookId, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long bookId,
            Authentication authentication) {
        AppUser user = getUserFromSub(authentication.getName());
        wishlistService.removeFromWishlist(bookId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistDto>> getUserWishlist(Authentication authentication) {
        AppUser user = getUserFromSub(authentication.getName());
        List<WishlistDto> wishlist = wishlistService.getUserWishlist(user);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/{bookId}/check")
    public ResponseEntity<Boolean> isWishlisted(
            @PathVariable Long bookId,
            Authentication authentication) {
        AppUser user = getUserFromSub(authentication.getName());
        boolean isWishlisted = wishlistService.isWishlisted(bookId, user);
        return ResponseEntity.ok(isWishlisted);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WishlistDto> updateWishlist(
            @PathVariable Long id,
            @RequestBody WishlistDto dto) {
        WishlistDto responseDto = wishlistService.updateNotificationEnabled(id, dto.isNotificationEnabled());
        return ResponseEntity.ok(responseDto);
    }
}
