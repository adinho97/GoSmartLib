package com.example.demo.config;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HighlightedBookRepository extends JpaRepository<HighlightedBook, Long> {
    List<HighlightedBook> findBySchoolIdOrderByIdDesc(Long schoolId);

    Optional<HighlightedBook> findByBookIdAndSchoolId(Long bookId, Long schoolId);

    void deleteByBookIdAndSchoolId(Long bookId, Long schoolId);

    boolean existsByBookIdAndSchoolId(Long bookId, Long schoolId);
}