package com.example.demo.repositories;

import com.example.demo.entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    /** Alle top-level genres (geen parent), gesorteerd op naam */
    @Query("SELECT g FROM Genre g WHERE g.parent IS NULL ORDER BY g.naam ASC")
    List<Genre> findAllTopLevel();

    Optional<Genre> findByNaamIgnoreCaseAndParentIsNull(String naam);
    Optional<Genre> findByNaamIgnoreCaseAndParentId(String naam, Long parentId);

    boolean existsByNaamIgnoreCaseAndParentIsNull(String naam);
    boolean existsByNaamIgnoreCaseAndParentId(String naam, Long parentId);

    // Added for generic genre lookup
    Optional<Genre> findByNaamIgnoreCase(String naam);
}