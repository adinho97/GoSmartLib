package com.example.demo.controllers;

import com.example.demo.dto.FavoriteDTO;
import com.example.demo.entities.Favorite;
import com.example.demo.repositories.FavoriteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;

    public FavoriteController(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping
    public List<FavoriteDTO> getFavorites(@RequestHeader("X-User-Sub") String userSub) {
        return favoriteRepository.findByUserSub(userSub).stream()
                .map(f -> {
                    FavoriteDTO dto = new FavoriteDTO();
                    dto.setId(f.getId());
                    dto.setUserSub(f.getUserSub());
                    dto.setBookId(f.getBookId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<Void> addFavorite(@RequestHeader("X-User-Sub") String userSub, @PathVariable Long bookId) {
        if (favoriteRepository.findByUserSubAndBookId(userSub, bookId).isEmpty()) {
            favoriteRepository.save(new Favorite(userSub, bookId));
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFavorite(@RequestHeader("X-User-Sub") String userSub, @PathVariable Long bookId) {
        favoriteRepository.findByUserSubAndBookId(userSub, bookId).ifPresent(favoriteRepository::delete);
        return ResponseEntity.ok().build();
    }
}