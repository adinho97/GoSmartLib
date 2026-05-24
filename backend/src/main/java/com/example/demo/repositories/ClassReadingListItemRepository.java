package com.example.demo.repositories;

import com.example.demo.entities.ClassReadingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClassReadingListItemRepository extends JpaRepository<ClassReadingListItem, Long> {
    List<ClassReadingListItem> findBySchoolId(Long schoolId);

    Optional<ClassReadingListItem> findByBookIdAndSchoolId(Long bookId, Long schoolId);

    void deleteByBookIdAndSchoolId(Long bookId, Long schoolId);

    boolean existsByBookIdAndSchoolId(Long bookId, Long schoolId);

    long countBySchoolId(Long schoolId);
}