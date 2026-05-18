package com.example.demo.services;

import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final BookRepository bookRepo;
    private final BookCopyRepository bookCopyRepo;

    public WishlistService(WishlistRepository wishlistRepo, BookRepository bookRepo, BookCopyRepository bookCopyRepo) {
        this.wishlistRepo = wishlistRepo;
        this.bookRepo = bookRepo;
        this.bookCopyRepo = bookCopyRepo;
    }

    @Transactional
    public void addToWishlist(Long bookId, AppUser user) {
        Book book = bookRepo.findById(Objects.requireNonNull(bookId, "bookId"))
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
        Book book = bookRepo.findById(Objects.requireNonNull(bookId, "bookId"))
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
        Book book = bookRepo.findById(Objects.requireNonNull(bookId, "bookId"))
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        return wishlistRepo.findByUserAndBook(user, book).isPresent();
    }

    private WishlistDto toWishlistDto(Wishlist wishlist) {
        Book book = wishlist.getBook();
        
        // Count available and total copies
        long totalCopies = bookCopyRepo.countByBook_Id(book.getId());
        long availableCopies = bookCopyRepo.findByBook_Id(book.getId()).stream()
 .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
 .count();
        
        return new WishlistDto(
                wishlist.getId(),
                book.getId(),
                book.getTitel(),
                book.getAuteur(),
                book.getCover(),
                wishlist.getAddedAt(),
                wishlist.isNotificationEnabled(),
                wishlist.getLastNotifiedAt(),
                (int) availableCopies,
                (int) totalCopies);
    }
}
