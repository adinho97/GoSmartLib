package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.StatisticsController;
import com.example.demo.dto.StatisticsDTO;
import com.example.demo.dto.StatisticsService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @Test
    void getStatistics_WithoutSchoolId_CallsGlobalStats() throws Exception {
        // Arrange
        when(statisticsService.getGlobalStatistics()).thenReturn(new StatisticsDTO());

        // Act & Assert
        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk());

        verify(statisticsService).getGlobalStatistics();
    }

    @Test
    void getStatistics_WithSchoolId_CallsSchoolStats() throws Exception {
        // Arrange
        Long schoolId = 1L;
        when(statisticsService.getSchoolStatistics(schoolId)).thenReturn(new StatisticsDTO());

        // Act & Assert
        mockMvc.perform(get("/api/statistics")
                .param("schoolId", schoolId.toString()))
                .andExpect(status().isOk());

        verify(statisticsService).getSchoolStatistics(schoolId);
    }

    @Test
    void getStatistics_InvalidSchoolId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/statistics")
                .param("schoolId", "not-a-number"))
                .andExpect(status().isBadRequest());
    }
}