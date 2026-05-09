package com.example.demo.config;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/highlighted-books")
public class HighlightedBookController {

    private final HighlightedBookRepository repository;

    public HighlightedBookController(HighlightedBookRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/school/{schoolId}")
    public List<Long> getHighlightedBookIds(@PathVariable Long schoolId) {
        return repository.findBySchoolId(schoolId).stream()
                .map(HighlightedBook::getBookId)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/{bookId}/toggle")
    @Transactional
    public boolean toggleHighlight(@PathVariable Long bookId, @RequestParam Long schoolId) {
        var existing = repository.findByBookIdAndSchoolId(bookId, schoolId);
        if (existing.isPresent()) {
            repository.delete(Objects.requireNonNull(existing.get(), "highlightedBook"));
            return false;
        } else {
            repository.save(new HighlightedBook(bookId, schoolId));
            return true;
        }
    }

    @GetMapping("/{bookId}/status")
    public boolean isHighlighted(@PathVariable Long bookId, @RequestParam Long schoolId) {
        return repository.existsByBookIdAndSchoolId(bookId, schoolId);
    }
}