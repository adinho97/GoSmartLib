package com.example.demo.services;

import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final BookRepository bookRepo;
    private final AppUserRepository appUserRepo;

    public WishlistService(WishlistRepository wishlistRepo, BookRepository bookRepo, AppUserRepository appUserRepo) {
        this.wishlistRepo = wishlistRepo;
        this.bookRepo = bookRepo;
        this.appUserRepo = appUserRepo;
    }

    @Transactional
    public void addToWishlist(Long bookId, AppUser user) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        boolean alreadyWishlisted = wishlistRepo.findByUserAndBook(user, book).isPresent();
        if (alreadyWishlisted) {
            throw new IllegalStateException("Boek staat al op verlanglijst");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setBook(book);
        wishlistRepo.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long bookId, AppUser user) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        wishlistRepo.deleteByUserAndBook(user, book);
    }

    @Transactional(readOnly = true)
    public List<WishlistDto> getUserWishlist(AppUser user) {
        return wishlistRepo.findByUser(user)
                .stream()
                .map(this::toWishlistDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(Long bookId, AppUser user) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        return wishlistRepo.findByUserAndBook(user, book).isPresent();
    }

    private WishlistDto toWishlistDto(Wishlist wishlist) {
        return new WishlistDto(
                wishlist.getId(),
                wishlist.getBook().getId(),
                wishlist.getBook().getTitel(),
                wishlist.getBook().getAuteur(),
                wishlist.getBook().getCover(),
                wishlist.getAddedAt());
    }
}
