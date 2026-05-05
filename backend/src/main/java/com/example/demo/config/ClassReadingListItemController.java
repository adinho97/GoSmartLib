package com.example.demo.config;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/class-reading-list")
public class ClassReadingListItemController {

    private final ClassReadingListItemRepository repository;

    public ClassReadingListItemController(ClassReadingListItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/school/{schoolId}")
    public List<Long> getHighlightedBookIds(@PathVariable Long schoolId) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        return repository.findBySchoolId(resolvedSchoolId).stream()
                .map(ClassReadingListItem::getBookId)
                .collect(Collectors.toList());
    }

    @PostMapping("/{bookId}/toggle")
    @Transactional
    public boolean toggleHighlight(@PathVariable Long bookId, @RequestParam Long schoolId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        var existing = repository.findByBookIdAndSchoolId(resolvedBookId, resolvedSchoolId);
        if (existing.isPresent()) {
            repository.delete(existing.get());
            return false;
        } else {
            repository.save(new ClassReadingListItem(resolvedBookId, resolvedSchoolId));
            return true;
        }
    }

    @GetMapping("/{bookId}/status")
    public boolean isHighlighted(@PathVariable Long bookId, @RequestParam Long schoolId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        return repository.existsByBookIdAndSchoolId(resolvedBookId, resolvedSchoolId);
    }
}