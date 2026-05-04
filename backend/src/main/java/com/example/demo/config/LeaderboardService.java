package com.example.demo.config;

import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final LoanRepository loanRepository;
    private final AppUserRepository userRepository;

    public LeaderboardService(LoanRepository loanRepository, AppUserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
    }

    /**
     * Fetches leaderboard data for a specific user.
     * 
     * @param userSub        The sub (identifier) of the current authenticated user.
     * @param selectedKlasId Optional ID of a specific class to view.
     * @return LeaderboardResponseDTO containing class and school rankings.
     */
    @Transactional(readOnly = true)
    public LeaderboardResponseDTO getLeaderboardData(String userSub, Long selectedKlasId) {
        LeaderboardResponseDTO response = new LeaderboardResponseDTO();

        // 1. Get School ID for the user
        Long schoolId = userRepository.findBySub(userSub)
                .map(u -> u.getSchool() != null ? u.getSchool().getId() : null)
                .orElse(null);

        if (schoolId == null)
            return response;

        // 2. Fetch Top School Readers (Returning 'sub' in displayName field for
        // frontend resolution)
        response.setTopSchoolReaders(convertToDTO(loanRepository.findTopReadersBySchool(schoolId), userSub));

        // 3. Fetch Top Class Readers
        // In a real implementation, you'd use selectedKlasId or the user's own klasId
        if (selectedKlasId != null) {
            // logic for specific class...
        }

        // 4. Set placeholders for User Ranks (You would calculate actual rank via SQL
        // count query)
        response.setUserSchoolRank(new LeaderboardEntryDTO(0, userSub, 0, true));

        return response;
    }

    private List<LeaderboardEntryDTO> convertToDTO(List<Object[]> results, String currentUserSub) {
        List<LeaderboardEntryDTO> dtos = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            String sub = (String) row[0];
            Long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;

            dtos.add(new LeaderboardEntryDTO(
                    i + 1,
                    sub, // Pass sub as displayName for now; frontend will resolve it
                    count.intValue(),
                    sub.equals(currentUserSub)));
        }
        return dtos;
    }
}