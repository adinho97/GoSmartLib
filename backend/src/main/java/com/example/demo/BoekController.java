package com.example.demo;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.mappers.BoekMapper;
import com.example.demo.services.BoekService;
import jakarta.validation.Valid;
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

    public BoekController(BoekRepository repo, BoekService boekService) {
        this.repo = repo;
        this.boekService = boekService;
    }

    @GetMapping
    public List<BoekDto> getAll() {
        return repo.findAll()
                .stream()
                .map(BoekMapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public BoekDto create(@Valid @RequestBody BoekDto boekDto) {
        Boek entity = BoekMapper.toEntity(boekDto);
        entity.setId(null); // id altijd door de database laten bepalen

        Boek saved = repo.save(entity);
        return BoekMapper.toDto(saved);
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BoekDto> getByIsbn(@PathVariable String isbn) {
        return boekService.findByIsbn(isbn)
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
    public ResponseEntity<BoekDto> importByIsbn(@PathVariable String isbn) {
        BoekDto dto = boekService.importByIsbn(isbn);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}
