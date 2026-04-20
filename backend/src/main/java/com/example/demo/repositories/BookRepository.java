package com.example.demo.repositories;

import com.example.demo.entities.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Override
    List<Book> findAll();

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Override
    Optional<Book> findById(Long id);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByIsbn(String isbn);

    boolean existsByGoNumber(String goNumber);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByIsbnAndSchool_Id(String isbn, Long schoolId);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByIdAndSchool_Id(Long id, Long schoolId);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    List<Book> findAllBySchool_Id(Long schoolId);

    boolean existsByIdAndSchool_Id(Long id, Long schoolId);
}