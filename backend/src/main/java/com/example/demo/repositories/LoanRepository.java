package com.example.demo.repositories;

import com.example.demo.entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByReturnedAtIsNull();

    List<Loan> findByReturnedAtIsNotNull();

    List<Loan> findByUserSubAndReturnedAtIsNull(String userSub);

    List<Loan> findByUserSubAndReturnedAtIsNotNull(String userSub);

    List<Loan> findByCopy_Book_IdAndReturnedAtIsNull(Long bookId);

    List<Loan> findByDueDateAndReturnedAtIsNull(LocalDate dueDate);

    List<Loan> findByDueDateBeforeAndReturnedAtIsNull(LocalDate date);

    void deleteByCopy_Id(Long copyId);
    long countByReturnedAtIsNull();

    long countByCopy_Book_School_Id(Long schoolId);

    long countByCopy_Book_School_IdAndReturnedAtIsNull(Long schoolId);

    @Query("SELECT l.copy.book.titel, COUNT(l) FROM Loan l " +
            "WHERE (:schoolId IS NULL OR l.copy.book.school.id = :schoolId) " +
            "GROUP BY l.copy.book.titel " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> findPopularBooksBySchool(@Param("schoolId") Long schoolId);
    @Query("SELECT new map(l.copy.book.id as bookId, COUNT(l) as loanCount) FROM Loan l GROUP BY l.copy.book.id")
    List<Map<String, Object>> getLoanCountsByBook();
}