package com.example.demo.repositories;

import com.example.demo.entities.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
}
