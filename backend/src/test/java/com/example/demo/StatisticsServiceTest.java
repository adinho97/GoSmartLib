package com.example.demo;

import com.example.demo.dto.StatisticsDTO;
import com.example.demo.dto.StatisticsService;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private SchoolRepository schoolRepository;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(bookRepository, loanRepository, userRepository, schoolRepository);
    }

    @Test
    void getGlobalStatistics_ReturnsAggregatedData() {
        // Arrange
        when(bookRepository.count()).thenReturn(100L);
        when(loanRepository.count()).thenReturn(50L);
        when(loanRepository.countByReturnedAtIsNull()).thenReturn(10L);
        when(userRepository.count()).thenReturn(20L);
        when(loanRepository.findPopularBooksBySchool(null)).thenReturn(new ArrayList<>());
        when(bookRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        StatisticsDTO result = statisticsService.getGlobalStatistics();

        // Assert
        assertEquals(100L, result.getTotalBooks());
        assertEquals(50L, result.getTotalLoans());
        assertEquals(10L, result.getActiveLoans());
        assertEquals(20L, result.getTotalUsers());
        verify(loanRepository).findPopularBooksBySchool(null);
    }

    @Test
    void getSchoolStatistics_ReturnsFilteredData() {
        // Arrange
        Long schoolId = 1L;
        School school = new School();
        school.setId(schoolId);
        school.setNaam("Test School");

        when(bookRepository.countBySchool_Id(schoolId)).thenReturn(50L);
        when(loanRepository.countByCopy_Book_School_Id(schoolId)).thenReturn(30L);
        when(loanRepository.countByCopy_Book_School_IdAndReturnedAtIsNull(schoolId)).thenReturn(5L);
        when(userRepository.countBySchool_Id(schoolId)).thenReturn(15L);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        // Mock popular books query result
        List<Object[]> popularBooksRaw = new ArrayList<>();
        popularBooksRaw.add(new Object[] { 1L, "De Brief voor de Koning", "Tonke Dragt", 10L });
        when(loanRepository.findPopularBooksBySchool(schoolId)).thenReturn(popularBooksRaw);

        // Mock Top Reader and Class
        List<Object[]> topReaderRaw = new ArrayList<>();
        topReaderRaw.add(new Object[] { "user-sub-123", 5L });
        when(loanRepository.findTopReadersBySchool(schoolId)).thenReturn(topReaderRaw);

        List<Object[]> topClassRaw = new ArrayList<>();
        topClassRaw.add(new Object[] { "6A", 12L });
        when(loanRepository.findTopClassesBySchool(schoolId)).thenReturn(topClassRaw);

        when(bookRepository.findAllBySchool_Id(schoolId)).thenReturn(new ArrayList<>());

        // Act
        StatisticsDTO result = statisticsService.getSchoolStatistics(schoolId);

        // Assert
        assertEquals(50L, result.getTotalBooks());
        assertEquals("Test School", result.getSchool());
        assertNotNull(result.getMostReadBook());
        assertEquals("De Brief voor de Koning", result.getMostReadBook().get("titel"));
        assertEquals(10L, result.getMostReadBook().get("count"));

        assertNotNull(result.getTopReader());
        assertEquals("user-sub-123", result.getTopReader().get("sub"));

        assertNotNull(result.getTopClass());
        assertEquals("6A", result.getTopClass().get("name"));
    }

    @Test
    void getSchoolStatistics_SchoolNotFound_HandlesOptionalGracefully() {
        Long schoolId = 99L;
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());
        StatisticsDTO result = statisticsService.getSchoolStatistics(schoolId);
        assertNull(result.getSchool());
    }
}