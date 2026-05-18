package com.example.demo.repositories;

import com.example.demo.entities.InfoContentHidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InfoContentHiddenRepository extends JpaRepository<InfoContentHidden, Long> {

    boolean existsBySchoolIdAndInfoContentId(Long schoolId, Long infoContentId);

    Optional<InfoContentHidden> findBySchoolIdAndInfoContentId(Long schoolId, Long infoContentId);

    @Query("select h.infoContent.id from InfoContentHidden h where h.school.id = :schoolId")
    List<Long> findHiddenInfoContentIdsBySchoolId(@Param("schoolId") Long schoolId);

    @Modifying
    void deleteBySchoolIdAndInfoContentId(Long schoolId, Long infoContentId);
}
