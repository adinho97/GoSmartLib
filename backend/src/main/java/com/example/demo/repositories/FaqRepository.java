package com.example.demo.repositories;

import com.example.demo.entities.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findBySchoolIdOrderBySortOrderAsc(Long schoolId);
}