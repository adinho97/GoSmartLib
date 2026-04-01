package com.example.demo.repositories;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser(AppUser user);

    Optional<Favorite> findByUserAndBook(AppUser user, Book book);

    void deleteByUserAndBook(AppUser user, Book book);
}