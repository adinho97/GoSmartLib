package com.example.demo.controllers;

import com.example.demo.entities.School;
import com.example.demo.repositories.SchoolRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}