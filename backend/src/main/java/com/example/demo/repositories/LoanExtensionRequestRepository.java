package com.example.demo.repositories;

import com.example.demo.entities.LoanExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanExtensionRequestRepository extends JpaRepository<LoanExtensionRequest, Long> {
    List<LoanExtensionRequest> findByLoan_Copy_Book_School_IdAndStatus(Long schoolId,
            LoanExtensionRequest.RequestStatus status);

    long countByLoan_Copy_Book_School_IdAndStatus(Long schoolId, LoanExtensionRequest.RequestStatus status);

    boolean existsByLoan_IdAndStatus(Long loanId, LoanExtensionRequest.RequestStatus status);
}