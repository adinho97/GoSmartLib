package com.example.demo.controllers;

import com.example.demo.dto.StatisticsDTO;
import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.entities.School;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.SchoolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.dto.StatisticsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scholen")
public class SchoolController {

    private final SchoolRepository schoolRepository;
    private final KlasRepository klasRepository;
    private final StatisticsService statisticsService;

    public SchoolController(SchoolRepository schoolRepository, KlasRepository klasRepository, StatisticsService statisticsService) {
        this.schoolRepository = schoolRepository;
        this.klasRepository = klasRepository;
        this.statisticsService = statisticsService;
    }

    /**
     * Returns a list of all schools with their names, addresses, and coordinates.
     */
    @GetMapping
    public List<School> getAllSchools() {
        return schoolRepository.findAll();
    }

    @GetMapping("/paged")
    public Map<String, Object> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {
        Page<School> schoolPage = schoolRepository.searchPaged(query, PageRequest.of(page, size));
        return Map.of("items", schoolPage.getContent(), "total", schoolPage.getTotalElements());
    }

    @GetMapping("/{schoolId}/statistieken")
    public StatisticsDTO getSchoolStatistics(@PathVariable Long schoolId) {
        return statisticsService.getSchoolStatistics(schoolId);
    }

    @GetMapping("/{schoolId}/klassen")
    public List<KlasListItem> getSchoolKlassen(@PathVariable Long schoolId) {
        return klasRepository.findBySchool_Id(schoolId)
                .stream()
                .map(klas -> new KlasListItem(klas.getId(), klas.getGroupId(), klas.getNaam()))
                .toList();
    }
}