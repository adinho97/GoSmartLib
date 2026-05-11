// src/main/java/com/example/demo/repositories/GenreRepository.java
package com.example.demo.repositories;

import com.example.demo.entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByNaamIgnoreCase(String naam);
    boolean existsByNaamIgnoreCase(String naam);
}