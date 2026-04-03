package com.example.demo.repositories;

import com.example.demo.entities.LoanHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanHistoryRepository extends JpaRepository<LoanHistory, Long> {
    List<LoanHistory> findByUserSubOrderByReturnedAtDesc(String userSub);
}
