package app.dashboard;

import com.example.demo.entities.ClassReadingListItem;
import com.example.demo.repositories.ClassReadingListItemRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
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
        return repository.findBySchoolId(schoolId).stream()
                .map(ClassReadingListItem::getBookId)
                .collect(Collectors.toList());
    }

    @PostMapping("/{bookId}/toggle")
    @Transactional
    public boolean toggleHighlight(@PathVariable Long bookId, @RequestParam Long schoolId) {
        var existing = repository.findByBookIdAndSchoolId(bookId, schoolId);
        if (existing.isPresent()) {
            repository.delete(existing.get());
            return false;
        } else {
            repository.save(new ClassReadingListItem(bookId, schoolId));
            return true;
        }
    }

    @GetMapping("/{bookId}/status")
    public boolean isHighlighted(@PathVariable Long bookId, @RequestParam Long schoolId) {
        return repository.existsByBookIdAndSchoolId(bookId, schoolId);
    }
}