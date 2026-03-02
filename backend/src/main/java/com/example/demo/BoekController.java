package com.example.demo;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.mappers.BoekMapper;
import com.example.demo.services.BoekService;
import jakarta.validation.Valid;
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
    public BoekDto getOrImportByIsbn(@PathVariable String isbn) {
        return boekService.importByIsbn(isbn);
    }
}
