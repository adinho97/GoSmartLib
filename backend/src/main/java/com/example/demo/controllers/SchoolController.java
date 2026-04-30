package com.example.demo.controllers;

import com.example.demo.entities.School;
import com.example.demo.repositories.SchoolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scholen")
public class SchoolController {

    private final SchoolRepository schoolRepository;

    public SchoolController(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
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
}