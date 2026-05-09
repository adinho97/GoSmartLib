package com.example.demo.dto;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API Controller for accessing library statistics.
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * GET /api/statistics
     * Fetches library statistics. Can be filtered by a specific school.
     */
    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<StatisticsDTO> getStatistics(
            @RequestParam(required = false) Long schoolId) {
        if (schoolId != null) {
            StatisticsDTO schoolStats = statisticsService.getSchoolStatistics(schoolId);
            return ResponseEntity.ok(schoolStats);
        }
        return ResponseEntity.ok(statisticsService.getGlobalStatistics());
    }
}