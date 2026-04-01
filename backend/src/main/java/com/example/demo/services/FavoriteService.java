package com.example.demo.services;

import com.example.demo.dto.FavoriteDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Favorite;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepo;
    private final BookRepository bookRepo;
    private final AppUserRepository appUserRepo;

    public FavoriteService(FavoriteRepository favoriteRepo, BookRepository bookRepo, AppUserRepository appUserRepo) {
        this.favoriteRepo = favoriteRepo;
        this.bookRepo = bookRepo;
        this.appUserRepo = appUserRepo;
    }

    @Transactional
    public void addToFavorites(Long bookId, AppUser user) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        boolean alreadyFavorited = favoriteRepo.findByUserAndBook(user, book).isPresent();
        if (alreadyFavorited) {
            throw new IllegalStateException("Boek staat al in favorieten");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);
        favoriteRepo.save(favorite);
    }

    @Transactional
    public void removeFromFavorites(Long bookId, AppUser user) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        favoriteRepo.deleteByUserAndBook(user, book);
    }

    @Transactional(readOnly = true)
    public List<FavoriteDto> getUserFavorites(AppUser user) {
        return favoriteRepo.findByUser(user)
                .stream()
                .map(this::toFavoriteDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long bookId, AppUser user) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        return favoriteRepo.findByUserAndBook(user, book).isPresent();
    }

    private FavoriteDto toFavoriteDto(Favorite favorite) {
        Book book = favorite.getBook();
        return new FavoriteDto(
                favorite.getId(),
                book.getId(),
                book.getTitel(),
                book.getAuteur(),
                book.getCover(),
                favorite.getAddedAt());
    }
}