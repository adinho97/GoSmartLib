package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boeken")
@CrossOrigin(origins = "*")
public class BoekController {
    private final BoekRepository repo;

    public BoekController(BoekRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Boek> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public Boek create(@Valid @RequestBody Boek boek) {
        return repo.save(boek);
    }
}
