package com.example.demo.controllers;

import com.example.demo.entities.Book;
import com.example.demo.entities.SchoolSpotlight;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.SchoolSpotlightRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/spotlight")
public class SchoolSpotlightController {

    private static final Set<String> VALID_TYPES = Set.of("MAAND", "THEMA");

    private final SchoolSpotlightRepository spotlightRepo;
    private final BookRepository bookRepo;

    public SchoolSpotlightController(SchoolSpotlightRepository spotlightRepo, BookRepository bookRepo) {
        this.spotlightRepo = spotlightRepo;
        this.bookRepo = bookRepo;
    }

    record SpotlightBookDto(Long bookId, String titel, String auteur, String cover) {}

    /** Returns { "maand": SpotlightBookDto|null, "thema": SpotlightBookDto|null } */
    @GetMapping("/{schoolId}")
    public Map<String, SpotlightBookDto> getSpotlights(@PathVariable Long schoolId) {
        Map<String, SpotlightBookDto> result = new HashMap<>();
        result.put("maand", null);
        result.put("thema", null);

        for (SchoolSpotlight s : spotlightRepo.findBySchoolId(schoolId)) {
            Long bookId = s.getBookId();
            if (bookId != null) {
                bookRepo.findById(bookId).ifPresent(book ->
                    result.put(s.getType().toLowerCase(),
                        new SpotlightBookDto(book.getId(), book.getTitel(), book.getAuteur(), book.getCover()))
                );
            }
        }
        return result;
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{schoolId}/{type}")
    @Transactional
    public ResponseEntity<SpotlightBookDto> setSpotlight(
            @PathVariable Long schoolId,
            @PathVariable String type,
            @RequestBody Map<String, Long> body) {
        String upperType = type.toUpperCase();
        if (!VALID_TYPES.contains(upperType)) return ResponseEntity.badRequest().build();

        Long bookId = body.get("bookId");
        if (bookId == null) return ResponseEntity.badRequest().build();

        Optional<Book> bookOpt = bookRepo.findById(bookId);
        if (bookOpt.isEmpty()) return ResponseEntity.notFound().build();

        SchoolSpotlight spotlight = spotlightRepo.findBySchoolIdAndType(schoolId, upperType)
                .orElse(new SchoolSpotlight());
        spotlight.setSchoolId(schoolId);
        spotlight.setType(upperType);
        spotlight.setBookId(bookId);
        spotlight.setSetAt(LocalDateTime.now());
        spotlightRepo.save(spotlight);

        Book book = bookOpt.get();
        return ResponseEntity.ok(new SpotlightBookDto(book.getId(), book.getTitel(), book.getAuteur(), book.getCover()));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{schoolId}/{type}")
    @Transactional
    public ResponseEntity<Void> clearSpotlight(@PathVariable Long schoolId, @PathVariable String type) {
        String upperType = type.toUpperCase();
        if (!VALID_TYPES.contains(upperType)) return ResponseEntity.badRequest().build();
        spotlightRepo.deleteBySchoolIdAndType(schoolId, upperType);
        return ResponseEntity.noContent().build();
    }
}
