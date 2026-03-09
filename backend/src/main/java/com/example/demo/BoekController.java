package com.example.demo;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.entities.School;
import com.example.demo.mappers.BoekMapper;
import com.example.demo.services.BoekService;
import com.example.demo.services.SchoolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/boeken")
@CrossOrigin(origins = "*")
public class BoekController {
    private final BoekRepository repo;
    private final BoekService boekService;
    private final SchoolService schoolService;

    public BoekController(BoekRepository repo, BoekService boekService, SchoolService schoolService) {
        this.repo = repo;
        this.boekService = boekService;
        this.schoolService = schoolService;
    }

    @GetMapping
    public List<BoekDto> getAll(@RequestParam(required = false) Long schoolId) {
        List<Boek> boeken = schoolId == null ? repo.findAll() : repo.findAllBySchool_Id(schoolId);

        return boeken
                .stream()
                .map(BoekMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoekDto> getBook(@PathVariable Long id, @RequestParam(required = false) Long schoolId) {
        return (schoolId == null ? repo.findById(id) : repo.findByIdAndSchool_Id(id, schoolId))
                .map(BoekMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BoekDto> create(@Valid @RequestBody BoekDto boekDto) {
        School school;
        try {
            school = schoolService.getByIdOrDefault(boekDto.getSchoolId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (repo.findByIsbnAndSchool_Id(boekDto.getIsbn(), school.getId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Boek entity = BoekMapper.toEntity(boekDto);
        entity.setId(null); // id altijd door de database laten bepalen
        entity.setSchool(school);

        Boek saved = repo.save(entity);
        return ResponseEntity.ok(BoekMapper.toDto(saved));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BoekDto> getByIsbn(@PathVariable String isbn, @RequestParam(required = false) Long schoolId) {
        return boekService.findByIsbn(isbn, schoolId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/preview/{isbn}")
    public ResponseEntity<BoekDto> previewByIsbn(@PathVariable String isbn) {
        BoekDto dto = boekService.fetchPreviewByIsbn(isbn);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/isbn/{isbn}")
    public ResponseEntity<BoekDto> importByIsbn(@PathVariable String isbn, @RequestParam(required = false) Long schoolId) {
        BoekDto dto;
        try {
            dto = boekService.importByIsbn(isbn, schoolId);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(required = false) Long schoolId) {
        boolean exists = schoolId == null ? repo.existsById(id) : repo.existsByIdAndSchool_Id(id, schoolId);
        if (!exists) {
            return ResponseEntity.notFound().build();
        }

        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
