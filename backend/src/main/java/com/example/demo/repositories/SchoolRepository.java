package com.example.demo.repositories;

import com.example.demo.entities.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    @Query("SELECT s FROM School s WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(s.naam) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.adres) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<School> searchPaged(@Param("query") String query, Pageable pageable);
}