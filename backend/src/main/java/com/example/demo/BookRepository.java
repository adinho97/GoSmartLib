package com.example.demo;

import com.example.demo.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    Optional<Book> findByIsbnAndSchool_Id(String isbn, Long schoolId);

    Optional<Book> findByIdAndSchool_Id(Long id, Long schoolId);

    List<Book> findAllBySchool_Id(Long schoolId);

    boolean existsByIdAndSchool_Id(Long id, Long schoolId);
}
