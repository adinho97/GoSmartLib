package com.example.demo.repositories;

import com.example.demo.entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Query("SELECT l FROM Loan l JOIN FETCH l.copy c JOIN FETCH c.book WHERE l.dueDate = :dueDate AND l.returnedAt IS NULL")
    List<Loan> findByDueDateAndReturnedAtIsNull(@Param("dueDate") LocalDate dueDate);

    List<Loan> findByUserSubAndReturnedAtIsNull(String userSub);

    List<Loan> findByUserSubAndReturnedAtIsNotNull(String userSub);

    List<Loan> findByCopy_Book_IdAndReturnedAtIsNull(Long bookId);

    Optional<Loan> findByCopy_IdAndReturnedAtIsNull(Long copyId);

    void deleteByCopy_Id(Long copyId);

    @Query("SELECT new map(l.copy.book.id as bookId, COUNT(l) as loanCount) FROM Loan l GROUP BY l.copy.book.id")
    List<Map<String, Object>> getLoanCountsByBook();
}