package com.example.demo.repositories;

import com.example.demo.entities.Klas;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KlasRepository extends JpaRepository<Klas, Long> {
    Optional<Klas> findBySchool_IdAndGroupId(Long schoolId, String groupId);

    long countBySchool_Id(Long schoolId);
}
