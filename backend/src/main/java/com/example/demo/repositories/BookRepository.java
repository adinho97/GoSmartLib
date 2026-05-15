package com.example.demo.repositories;

import com.example.demo.entities.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Override
        @NonNull
        List<Book> findAll();

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Override
        @NonNull
        Optional<Book> findById(@NonNull Long id);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Override
        @NonNull
        Page<Book> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByIsbn(String isbn);

    boolean existsByGoNumber(String goNumber);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByGoNumber(String goNumber);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByGoNumberAndSchool_Id(String goNumber, Long schoolId);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByIsbnAndSchool_Id(String isbn, Long schoolId);

    long countBySchool_Id(Long schoolId);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Optional<Book> findByIdAndSchool_Id(Long id, Long schoolId);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    List<Book> findAllBySchool_Id(Long schoolId);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    Page<Book> findAllBySchool_Id(Long schoolId, Pageable pageable);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Query("""
            SELECT b
            FROM Book b
            WHERE (:schoolId IS NULL OR (b.school IS NOT NULL AND b.school.id = :schoolId))
                AND (:query IS NULL OR LOWER(b.titel) LIKE LOWER(CONCAT('%', :query, '%'))
                         OR LOWER(b.auteur) LIKE LOWER(CONCAT('%', :query, '%')))
                AND (:excludeDidactic = false OR b.genre IS NULL OR LOWER(b.genre) NOT LIKE 'didactiek%')
            """)
    Page<Book> searchPaged(@Param("schoolId") Long schoolId,
            @Param("query") String query,
            @Param("excludeDidactic") boolean excludeDidactic,
            Pageable pageable);

    @EntityGraph(attributePaths = { "copies", "reviews" })
    @Query("SELECT b FROM Book b WHERE b.school.id = :schoolId AND (b.genre IS NULL OR LOWER(b.genre) NOT LIKE 'didactiek%')")
    List<Book> findNonDidacticBySchool_Id(@Param("schoolId") Long schoolId);

    boolean existsByIdAndSchool_Id(Long id, Long schoolId);
}