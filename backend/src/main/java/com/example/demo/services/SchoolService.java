package com.example.demo.services;

import com.example.demo.SchoolRepository;
import com.example.demo.entities.School;
import org.springframework.stereotype.Service;

@Service
public class SchoolService {
    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public School getByIdOrDefault(Long schoolId) {
        if (schoolId != null) {
            return schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new IllegalArgumentException("School niet gevonden"));
        }
        return getDefaultSchool();
    }

    public School getDefaultSchool() {
        return schoolRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Geen scholen beschikbaar"));
    }
}
