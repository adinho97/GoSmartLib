package com.example.demo;

import com.example.demo.entities.Boek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoekRepository extends JpaRepository<Boek, Long> {

    Optional<Boek> findByIsbn(String isbn);

    Optional<Boek> findByIsbnAndSchool_Id(String isbn, Long schoolId);

    Optional<Boek> findByIdAndSchool_Id(Long id, Long schoolId);

    List<Boek> findAllBySchool_Id(Long schoolId);

    boolean existsByIdAndSchool_Id(Long id, Long schoolId);
}
