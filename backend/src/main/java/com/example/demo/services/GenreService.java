// src/main/java/com/example/demo/services/GenreService.java
package com.example.demo.services;

import com.example.demo.dto.admin.genre.GenreRequest;
import com.example.demo.dto.admin.genre.GenreResponse;
import com.example.demo.entities.Genre;
import com.example.demo.repositories.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<GenreResponse> getAll() {
        return genreRepository.findAll().stream()
                .map(g -> new GenreResponse(g.getId(), g.getNaam()))
                .toList();
    }

    public GenreResponse create(GenreRequest request) {
        if (genreRepository.existsByNaamIgnoreCase(request.getNaam())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Genre bestaat al");
        }
        Genre genre = new Genre();
        genre.setNaam(request.getNaam().trim());
        Genre saved = genreRepository.save(genre);
        return new GenreResponse(saved.getId(), saved.getNaam());
    }

    public GenreResponse update(Long id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre niet gevonden"));

        genreRepository.findByNaamIgnoreCase(request.getNaam())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(g -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Genre bestaat al"); });

        genre.setNaam(request.getNaam().trim());
        Genre saved = genreRepository.save(genre);
        return new GenreResponse(saved.getId(), saved.getNaam());
    }

    public void delete(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre niet gevonden");
        }
        genreRepository.deleteById(id);
    }
}