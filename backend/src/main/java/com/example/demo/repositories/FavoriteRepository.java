package com.example.demo.repositories;

import com.example.demo.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserSub(String userSub);

    Optional<Favorite> findByUserSubAndBookId(String userSub, Long bookId);

    void deleteByUserSubAndBookId(String userSub, Long bookId);
}