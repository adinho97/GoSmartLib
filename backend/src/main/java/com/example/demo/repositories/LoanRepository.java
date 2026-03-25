package com.example.demo.repositories;

import com.example.demo.entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUsernameAndReturnedAtIsNull(String username);
    List<Loan> findByUsernameAndReturnedAtIsNotNull(String username);
    List<Loan> findByCopy_Book_IdAndReturnedAtIsNull(Long bookId);
    Optional<Loan> findByCopy_IdAndReturnedAtIsNull(Long copyId);
    void deleteByCopy_Id(Long copyId);
}