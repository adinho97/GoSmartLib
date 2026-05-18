package com.example.demo.controllers;

import com.example.demo.entities.School;
import com.example.demo.exception.ApiException;
import com.example.demo.oneroster.OneRosterProperties;
import com.example.demo.oneroster.OneRosterSyncService;
import com.example.demo.oneroster.dto.OneRosterSyncResult;
import com.example.demo.repositories.SchoolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/schools")
public class AdminOneRosterController {

    private final SchoolRepository schoolRepository;
    private final OneRosterProperties properties;
    private final OneRosterSyncService syncService;

    public AdminOneRosterController(SchoolRepository schoolRepository,
            OneRosterProperties properties,
            OneRosterSyncService syncService) {
        this.schoolRepository = schoolRepository;
        this.properties = properties;
        this.syncService = syncService;
    }

    @PostMapping("/{id}/oneroster/sync")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OneRosterSyncResult triggerSync(@PathVariable Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ApiException("School niet gevonden",
                        HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));

        String subdomain = school.getSubdomein();
        if (!properties.isConfigured(subdomain)) {
            OneRosterSyncResult result = new OneRosterSyncResult();
            result.setSubdomain(subdomain);
            result.setSkippedEntirely(true);
            result.setSkipReason("no config");
            return result;
        }

        return syncService.syncBySubdomain(subdomain);
    }
}
