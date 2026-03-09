package com.example.demo;

import com.example.demo.entities.Boek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoekRepository extends JpaRepository<Boek, Long> {

    Optional<Boek> findByIsbn(String isbn);
}
