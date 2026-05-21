package com.example.demo.repositories;

import com.example.demo.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findBySchool_Id(Long schoolId);
}