package com.example.demo.controllers;

import com.example.demo.dto.FavoriteAddRequest;
import com.example.demo.dto.FavoriteDto;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.services.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorieten")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AppUserRepository appUserRepository;
    private final FavoriteRepository favoriteRepository;

    public FavoriteController(FavoriteService favoriteService, AppUserRepository appUserRepository,
            FavoriteRepository favoriteRepository) {
        this.favoriteService = favoriteService;
        this.appUserRepository = appUserRepository;
        this.favoriteRepository = favoriteRepository;
    }

    private AppUser getUserFromHeader(String userSub) {
        return appUserRepository.findBySub(userSub)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden"));
    }

    @PostMapping
    public ResponseEntity<Void> addToFavorites(
            @Valid @RequestBody FavoriteAddRequest request,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            favoriteService.addToFavorites(request.getBookId(), user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            favoriteService.removeFromFavorites(bookId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FavoriteDto>> getUserFavorites(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            List<FavoriteDto> favorites = favoriteService.getUserFavorites(user);
            return ResponseEntity.ok(favorites);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{bookId}/check")
    public ResponseEntity<Boolean> isFavorited(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = getUserFromHeader(userSub);
            boolean isFavorited = favoriteService.isFavorited(bookId, user);
            return ResponseEntity.ok(isFavorited);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}