package com.example.demo.services;

import com.example.demo.dto.admin.genre.GenreRequest;
import com.example.demo.dto.admin.genre.GenreResponse;
import com.example.demo.dto.admin.genre.GenreSubgenreResponse;
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

    // ── Lees ────────────────────────────────────────────────────────────────

    /** Alle top-level genres met hun subgenres — voor publieke dropdown */
    public List<GenreResponse> getAll() {
        return genreRepository.findAllTopLevel().stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Top-level genre CRUD ─────────────────────────────────────────────────

    public GenreResponse createTopLevel(GenreRequest request) {
        if (genreRepository.existsByNaamIgnoreCaseAndParentIsNull(request.getNaam())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Genre bestaat al");
        }
        Genre genre = new Genre();
        genre.setNaam(request.getNaam().trim());
        return toResponse(genreRepository.save(genre));
    }

    public GenreResponse updateTopLevel(Long id, GenreRequest request) {
        Genre genre = findById(id);
        if (genre.getParent() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gebruik de subgenre-update endpoint");
        }
        genreRepository.findByNaamIgnoreCaseAndParentIsNull(request.getNaam())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(g -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Genre bestaat al"); });
        genre.setNaam(request.getNaam().trim());
        return toResponse(genreRepository.save(genre));
    }

    public void delete(Long id) {
        Genre genre = findById(id);
        genreRepository.delete(genre);
    }

    // ── Subgenre CRUD ────────────────────────────────────────────────────────

    public GenreResponse createSubgenre(Long parentId, GenreRequest request) {
        Genre parent = findById(parentId);
        if (parent.getParent() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subgenres van subgenres zijn niet toegestaan");
        }
        if (genreRepository.existsByNaamIgnoreCaseAndParentId(request.getNaam(), parentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subgenre bestaat al voor dit genre");
        }
        Genre sub = new Genre();
        sub.setNaam(request.getNaam().trim());
        sub.setParent(parent);
        genreRepository.save(sub);
        // herlaad parent met verse subgenres
        return toResponse(findById(parentId));
    }

    public GenreResponse updateSubgenre(Long parentId, Long subId, GenreRequest request) {
        Genre sub = findById(subId);
        if (sub.getParent() == null || !sub.getParent().getId().equals(parentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subgenre hoort niet bij dit genre");
        }
        genreRepository.findByNaamIgnoreCaseAndParentId(request.getNaam(), parentId)
                .filter(existing -> !existing.getId().equals(subId))
                .ifPresent(g -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Subgenre bestaat al"); });
        sub.setNaam(request.getNaam().trim());
        genreRepository.save(sub);
        return toResponse(findById(parentId));
    }

    // ── Intern ──────────────────────────────────────────────────────────────

    private Genre findById(Long id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre niet gevonden"));
    }

    private GenreResponse toResponse(Genre genre) {
        List<GenreSubgenreResponse> subs = genre.getSubgenres().stream()
                .map(s -> new GenreSubgenreResponse(s.getId(), s.getNaam()))
                .toList();
        return new GenreResponse(genre.getId(), genre.getNaam(), subs);
    }
}