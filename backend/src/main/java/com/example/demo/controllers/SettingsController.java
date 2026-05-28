package com.example.demo.controllers;

import com.example.demo.entities.SchoolSettings;
import com.example.demo.repositories.SchoolSettingsRepository;
import com.example.demo.repositories.SchoolRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingsController {

    private final SchoolSettingsRepository settingsRepository;
    private final SchoolRepository schoolRepository;

    public SettingsController(SchoolSettingsRepository settingsRepository, SchoolRepository schoolRepository) {
        this.settingsRepository = settingsRepository;
        this.schoolRepository = schoolRepository;
    }

    @GetMapping("/settings/school/{schoolId}")
    public ResponseEntity<SchoolSettings> getSettings(@PathVariable Long schoolId) {
        return settingsRepository.findById(schoolId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> schoolRepository.findById(schoolId).map(school -> {
                    // Return default object instead of 404 to prevent frontend crashes
                    SchoolSettings defaults = new SchoolSettings();
                    defaults.setSchoolId(schoolId);
                    defaults.setMessages(new ArrayList<>());
                    defaults.setHours(new HashMap<>());
                    defaults.setLevels(new ArrayList<>());
                    return ResponseEntity.ok(defaults);
                }).orElse(ResponseEntity.notFound().build()));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/settings/school/{schoolId}")
    public ResponseEntity<SchoolSettings> saveSettings(@PathVariable Long schoolId,
            @RequestBody SchoolSettings settings) {
        return schoolRepository.findById(schoolId).map(school -> {
            // Fetch existing to perform a merge instead of an overwrite
            SchoolSettings existing = settingsRepository.findById(schoolId)
                    .orElse(new SchoolSettings());

            existing.setSchool(school);
            existing.setMessages(settings.getMessages());
            existing.setHours(settings.getHours());
            existing.setLevels(settings.getLevels());

            return ResponseEntity.ok(settingsRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/scholen/{schoolId}/name")
    public ResponseEntity<Map<String, String>> getSchoolName(@PathVariable Long schoolId) {
        return schoolRepository.findById(schoolId)
                .map(s -> ResponseEntity.ok(Map.of("name", s.getNaam())))
                .orElse(ResponseEntity.notFound().build());
    }
}