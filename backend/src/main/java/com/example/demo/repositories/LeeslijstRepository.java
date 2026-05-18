package com.example.demo.repositories;

import com.example.demo.entities.Leeslijst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeeslijstRepository extends JpaRepository<Leeslijst, Long> {
    /**
     * Find all reading lists for a specific school
     */
    List<Leeslijst> findBySchool_Id(Long schoolId);

    /**
     * Find all reading lists for a specific class
     */
    @Query("SELECT l FROM Leeslijst l JOIN l.klassen k WHERE k.id = :klasId")
    List<Leeslijst> findByKlas(@Param("klasId") Long klasId);

    /**
     * Find reading lists created by a specific user
     */
    List<Leeslijst> findByCreatedBy_Id(Long userId);

    /**
     * Find all global reading lists
     */
    List<Leeslijst> findByIsGlobalTrue();

    /**
     * Find reading lists assigned to a specific user
     */
    @Query("SELECT l FROM Leeslijst l JOIN l.assignedUsers u WHERE u.id = :userId")
    List<Leeslijst> findByAssignedUsers(@Param("userId") Long userId);
}
