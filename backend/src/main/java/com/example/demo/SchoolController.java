package com.example.demo;

import com.example.demo.entities.School;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scholen")
@CrossOrigin(origins = "*")
public class SchoolController {
    private final SchoolRepository schoolRepository;

    public SchoolController(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @GetMapping
    public List<School> getAll() {
        return schoolRepository.findAll();
    }
}
