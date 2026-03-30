package com.example.demo.controllers;

import com.example.demo.dto.WishlistAddRequest;
import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.services.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/verlanglijst")
public class WishlistController {

    private final WishlistService wishlistService;
    private final AppUserRepository appUserRepository;

    public WishlistController(WishlistService wishlistService, AppUserRepository appUserRepository) {
        this.wishlistService = wishlistService;
        this.appUserRepository = appUserRepository;
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
}
