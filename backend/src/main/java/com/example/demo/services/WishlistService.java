package com.example.demo.services;

import com.example.demo.dto.WishlistDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Wishlist;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.WishlistRepository;
import org.springframework.http.HttpStatus;
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

        if (user.getSchool() != null) {
            if (book.getSchool() == null
                    || !Objects.requireNonNull(user.getSchool().getId(), "schoolId is required")
                            .equals(Objects.requireNonNull(book.getSchool().getId(), "schoolId is required"))) {
                throw new ApiException("Boek behoort niet tot jouw school.", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
            }
        }

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

    @Transactional
    public WishlistDto updateNotificationEnabled(Long id, boolean notificationEnabled) {
        Wishlist wishlist = wishlistRepo.findById(Objects.requireNonNull(id, "id is required"))
                .orElseThrow(() -> new IllegalArgumentException("Wishlist not found"));
        Long bookId = Objects.requireNonNull(wishlist.getBook().getId(), "bookId is required");

        if (notificationEnabled) {
            long availableCopies = bookCopyRepo.findByBook_Id(bookId).stream()
                    .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                            || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
                    .count();

            if (availableCopies > 0) {
                throw new IllegalStateException("Cannot enable notifications for available book");
            }
        }

        wishlist.setNotificationEnabled(notificationEnabled);
        if (notificationEnabled) {
            wishlist.setLastNotifiedAt(null);
        }
        Wishlist updated = wishlistRepo.save(wishlist);
        return toWishlistDtoWithCounts(updated, bookId);
    }

    private WishlistDto toWishlistDtoWithCounts(Wishlist wishlist, Long bookId) {
        long totalCopies = bookCopyRepo.countByBook_Id(bookId);
        long availableCopies = bookCopyRepo.countByBook_IdAndStatus(bookId, BookCopy.CopyStatus.AVAILABLE);

        return new WishlistDto(
                wishlist.getId(),
                bookId,
                wishlist.getBook().getTitel(),
                wishlist.getBook().getAuteur(),
                wishlist.getBook().getCover(),
                wishlist.getAddedAt(),
                wishlist.isNotificationEnabled(),
                wishlist.getLastNotifiedAt(),
                (int) availableCopies,
                (int) totalCopies);
    }

    private WishlistDto toWishlistDto(Wishlist wishlist) {
        Book book = wishlist.getBook();

        long totalCopies = bookCopyRepo.countByBook_Id(book.getId());
        long availableCopies = bookCopyRepo.findByBook_Id(book.getId()).stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                        || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
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
