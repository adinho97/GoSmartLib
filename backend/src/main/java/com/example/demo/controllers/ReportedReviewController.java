package com.example.demo.controllers;

import com.example.demo.dto.ReportedReviewDto;
import com.example.demo.entities.ReportedReviewStatus;
import com.example.demo.dto.ResolveReportRequest;
import com.example.demo.services.ReportedReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review-reports")
@PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
public class ReportedReviewController {

    private final ReportedReviewService reportedReviewService;

    public ReportedReviewController(ReportedReviewService reportedReviewService) {
        this.reportedReviewService = reportedReviewService;
    }

    @GetMapping
    public List<ReportedReviewDto> getAll() {
        return reportedReviewService.getAll();
    }

    @GetMapping("/count")
    public Map<String, Long> getCount() {
        return Map.of("count", reportedReviewService.countPending());
    }

    @PatchMapping("/{reportId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long reportId, @Valid @RequestBody ResolveReportRequest request) {
        reportedReviewService.resolveReport(reportId, request.getAction());
        return ResponseEntity.ok().build();
    }
}