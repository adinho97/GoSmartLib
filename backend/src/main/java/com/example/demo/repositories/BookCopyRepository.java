package com.example.demo.repositories;

import com.example.demo.entities.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBook_Id(Long bookId);
    long countByBook_IdAndStatus(Long bookId, BookCopy.CopyStatus status);
    long countByBook_Id(Long bookId);
}