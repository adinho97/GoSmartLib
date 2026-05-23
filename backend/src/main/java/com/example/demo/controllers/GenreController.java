package com.example.demo.controllers;

import com.example.demo.dto.admin.genre.GenreRequest;
import com.example.demo.dto.admin.genre.GenreResponse;
import com.example.demo.services.GenreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public List<GenreResponse> getAll() {
        return genreService.getAll();
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<GenreResponse> create(@Valid @RequestBody GenreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.createTopLevel(request));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public GenreResponse update(@PathVariable Long id, @Valid @RequestBody GenreRequest request) {
        return genreService.updateTopLevel(id, request);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        genreService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/{parentId}/subgenres")
    public ResponseEntity<GenreResponse> createSubgenre(
            @PathVariable Long parentId,
            @Valid @RequestBody GenreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(genreService.createSubgenre(parentId, request));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{parentId}/subgenres/{subId}")
    public GenreResponse updateSubgenre(
            @PathVariable Long parentId,
            @PathVariable Long subId,
            @Valid @RequestBody GenreRequest request) {
        return genreService.updateSubgenre(parentId, subId, request);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{parentId}/subgenres/{subId}")
    public ResponseEntity<Void> deleteSubgenre(
            @PathVariable Long parentId,
            @PathVariable Long subId) {
        genreService.delete(subId);
        return ResponseEntity.noContent().build();
    }
}