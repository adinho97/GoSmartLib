package com.example.demo.repositories;

import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InfoContentRepository extends JpaRepository<InfoContent, Long> {
    List<InfoContent> findBySchoolIdAndSectieOrderBySortOrderAsc(Long schoolId, Sectie sectie);
    boolean existsBySchoolIdAndSectie(Long schoolId, Sectie sectie);
}