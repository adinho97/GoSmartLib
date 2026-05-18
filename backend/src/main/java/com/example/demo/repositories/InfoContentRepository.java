package com.example.demo.repositories;

import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InfoContentRepository extends JpaRepository<InfoContent, Long> {

    @Query("""
        SELECT ic FROM InfoContent ic
        WHERE ic.sectie = :sectie
          AND (
            ic.school.id = :schoolId
            OR (
              ic.school IS NULL
              AND ic.id NOT IN (
                SELECT h.infoContent.id FROM InfoContentHidden h WHERE h.school.id = :schoolId
              )
            )
          )
        ORDER BY ic.sortOrder ASC
        """)
    List<InfoContent> findVisibleForSchool(@Param("schoolId") Long schoolId,
                                           @Param("sectie") Sectie sectie);

    @Query("""
        SELECT ic FROM InfoContent ic
        WHERE ic.sectie = :sectie AND ic.school IS NULL
        ORDER BY ic.sortOrder ASC
        """)
    List<InfoContent> findGlobals(@Param("sectie") Sectie sectie);

    boolean existsBySchoolIsNullAndSectie(Sectie sectie);
}
