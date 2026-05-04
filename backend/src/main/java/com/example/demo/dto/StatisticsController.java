package com.example.demo.dto;

import com.example.demo.dto.StatisticsDTO;
import com.example.demo.dto.StatisticsService;
import org.springframework.http.ResponseEntity;
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