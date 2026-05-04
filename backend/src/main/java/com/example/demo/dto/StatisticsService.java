package com.example.demo.dto;

import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for calculating and aggregating statistics.
 */
@Service
public class StatisticsService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final AppUserRepository userRepository;
    private final SchoolRepository schoolRepository;

    public StatisticsService(BookRepository bookRepository,
            LoanRepository loanRepository,
            AppUserRepository userRepository,
            SchoolRepository schoolRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
    }

    /**
     * Returns aggregate statistics for the entire platform.
     */
    @Transactional(readOnly = true)
    public StatisticsDTO getGlobalStatistics() {
        StatisticsDTO dto = new StatisticsDTO();
        dto.setTotalBooks(bookRepository.count());
        dto.setTotalLoans(loanRepository.count());
        dto.setActiveLoans(loanRepository.countByReturnedAtIsNull());
        dto.setTotalUsers(userRepository.count());

        dto.setPopularBooks(formatPopularBooks(loanRepository.findPopularBooksBySchool(null)));
        dto.setBooksPerGenre(formatGenreCounts(bookRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getGenre() != null && !b.getGenre().isBlank() ? b.getGenre() : "Onbekend",
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Object[] { e.getKey(), e.getValue() })
                .collect(Collectors.toList())));

        return dto;
    }

    /**
     * Returns statistics filtered by a specific school.
     */
    @Transactional(readOnly = true)
    public StatisticsDTO getSchoolStatistics(Long schoolId) {
        StatisticsDTO dto = new StatisticsDTO();
        dto.setTotalBooks(bookRepository.countBySchool_Id(schoolId));
        dto.setTotalLoans(loanRepository.countByCopy_Book_School_Id(schoolId));
        dto.setActiveLoans(loanRepository.countByCopy_Book_School_IdAndReturnedAtIsNull(schoolId));
        dto.setTotalUsers(userRepository.countBySchool_Id(schoolId));

        // Fetch school name
        schoolIdOptional(schoolId).ifPresent(s -> dto.setSchool(s.getNaam()));

        // Map the first popular book to mostReadBook
        List<Map<String, Object>> popular = formatPopularBooks(loanRepository.findPopularBooksBySchool(schoolId));
        dto.setPopularBooks(popular);
        if (!popular.isEmpty()) {
            dto.setMostReadBook(popular.get(0));
        }

        // Populate Top Reader and Top Class
        dto.setTopReader(formatTopItem(loanRepository.findTopReadersBySchool(schoolId), "sub", "displayName"));
        dto.setTopClass(formatTopItem(loanRepository.findTopClassesBySchool(schoolId), "name"));

        dto.setPopularBooks(formatPopularBooks(loanRepository.findPopularBooksBySchool(schoolId)));
        dto.setBooksPerGenre(formatGenreCounts(bookRepository.findAllBySchool_Id(schoolId).stream()
                .collect(Collectors.groupingBy(
                        b -> b.getGenre() != null && !b.getGenre().isBlank() ? b.getGenre() : "Onbekend",
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Object[] { e.getKey(), e.getValue() })
                .collect(Collectors.toList())));

        return dto;
    }

    private Optional<School> schoolIdOptional(Long schoolId) {
        return schoolRepository.findById(schoolId);
    }

    private List<Map<String, Object>> formatPopularBooks(List<Object[]> results) {
        return results.stream()
                .limit(5)
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", row[0]);
                    map.put("titel", row[1] != null ? row[1].toString() : "Onbekend");
                    map.put("auteur", row[2] != null ? row[2].toString() : "Onbekend");
                    map.put("count", row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L);
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> formatTopItem(List<Object[]> results, String... keys) {
        if (results.isEmpty())
            return null;
        Object[] row = results.get(0);
        Map<String, Object> map = new HashMap<>();
        for (String key : keys) {
            map.put(key, row[0]); // Uses the identifier for both key/display if multiple keys provided
        }
        map.put("count", row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L);
        return map;
    }

    private Map<String, Long> formatGenreCounts(List<Object[]> results) {
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "Onbekend",
                        row -> row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L,
                        (existing, replacement) -> existing));
    }
}