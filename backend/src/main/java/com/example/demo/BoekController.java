package com.example.demo;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.mappers.BoekMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/boeken")
@CrossOrigin(origins = "*")
public class BoekController {
    private final BoekRepository repo;

    public BoekController(BoekRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<BoekDto> getAll() {
        return repo.findAll()
                .stream()
                .map(BoekMapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public Boek create(@Valid @RequestBody Boek boek) {
        return repo.save(boek);
    }
}
