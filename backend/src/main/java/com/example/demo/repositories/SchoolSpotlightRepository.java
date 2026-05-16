package com.example.demo.repositories;

import com.example.demo.entities.SchoolSpotlight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchoolSpotlightRepository extends JpaRepository<SchoolSpotlight, Long> {
    List<SchoolSpotlight> findBySchoolId(Long schoolId);
    Optional<SchoolSpotlight> findBySchoolIdAndType(Long schoolId, String type);
    void deleteBySchoolIdAndType(Long schoolId, String type);
}
