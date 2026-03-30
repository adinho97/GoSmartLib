package com.example.demo.repositories;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser(AppUser user);
    Optional<Wishlist> findByUserAndBook(AppUser user, Book book);
    void deleteByUserAndBook(AppUser user, Book book);
}
