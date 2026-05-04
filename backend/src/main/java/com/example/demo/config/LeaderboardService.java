package com.example.demo.config;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    /**
     * Fetches leaderboard data for a specific user.
     * 
     * @param userId         The ID of the current authenticated user.
     * @param selectedKlasId Optional ID of a specific class to view.
     * @return LeaderboardResponseDTO containing class and school rankings.
     */
    public LeaderboardResponseDTO getLeaderboardData(Long userId, Long selectedKlasId) {
        LeaderboardResponseDTO response = new LeaderboardResponseDTO();

        // In a real app, if selectedKlasId is null, use the userId's own class
        response.setTopClassReaders(
                getMockTopReaders(userId, "Class " + (selectedKlasId != null ? selectedKlasId : "")));
        response.setTopSchoolReaders(getMockTopReaders(userId, "School"));
        response.setUserClassRank(getUserRank(userId, "Class"));
        response.setUserSchoolRank(getUserRank(userId, "School"));

        // Simulate providing available classes for a teacher's school
        List<LeaderboardResponseDTO.LeaderboardKlasDTO> classes = new ArrayList<>();
        classes.add(new LeaderboardResponseDTO.LeaderboardKlasDTO(1L, "1A"));
        classes.add(new LeaderboardResponseDTO.LeaderboardKlasDTO(2L, "1B"));
        classes.add(new LeaderboardResponseDTO.LeaderboardKlasDTO(3L, "2A"));
        response.setAvailableClasses(classes);

        return response;
    }

    private List<LeaderboardEntryDTO> getMockTopReaders(Long currentUserId, String scope) {
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            entries.add(new LeaderboardEntryDTO(
                    i,
                    "Reader " + i,
                    100 - (i * 5),
                    false));
        }
        return entries;
    }

    private LeaderboardEntryDTO getUserRank(Long userId, String scope) {
        return new LeaderboardEntryDTO(
                scope.equals("Class") ? 10 : 25,
                "Current User",
                5,
                true);
    }
}