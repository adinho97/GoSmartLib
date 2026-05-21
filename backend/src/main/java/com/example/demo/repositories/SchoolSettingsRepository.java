package com.example.demo.repositories;

import com.example.demo.entities.SchoolSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolSettingsRepository extends JpaRepository<SchoolSettings, Long> {
}