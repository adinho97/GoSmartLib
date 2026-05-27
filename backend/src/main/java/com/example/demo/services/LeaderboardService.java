package com.example.demo.services;

import com.example.demo.dto.LeaderboardEntryDTO;
import com.example.demo.dto.LeaderboardResponseDTO;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.KlasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final LoanRepository loanRepository;
    private final AppUserRepository userRepository;
    private final KlasRepository klasRepository;
    private final DisplayNameResolver displayNameResolver;

    public LeaderboardService(LoanRepository loanRepository, AppUserRepository userRepository,
            KlasRepository klasRepository, DisplayNameResolver displayNameResolver) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.klasRepository = klasRepository;
        this.displayNameResolver = displayNameResolver;
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

        // 1. Get User, School ID, and Klas ID for the user
        Optional<AppUser> currentUserOpt = userRepository.findBySub(userSub);
        AppUser currentUser = currentUserOpt.orElse(null);

        Long schoolId = currentUser != null && currentUser.getSchool() != null ? currentUser.getSchool().getId() : null;
        Long userKlasId = currentUser != null && currentUser.getKlas() != null ? currentUser.getKlas().getId() : null;

        if (schoolId == null)
            return response; // If no schoolId, return an empty response.

        // Determine the effective klasId for class leaderboard
        Long effectiveKlasId = selectedKlasId != null ? selectedKlasId : userKlasId;

        // 2. Fetch Top School Readers
        response.setTopSchoolReaders(convertToDTO(loanRepository.findTopReadersBySchool(schoolId), userSub));

        // 3. Fetch Top Class Readers
        if (effectiveKlasId != null) {
            response.setTopClassReaders(convertToDTO(loanRepository.findTopReadersByClass(effectiveKlasId), userSub));
        }

        // 4. Set User Ranks
        // User's rank in school
        List<Object[]> userSchoolRankData = loanRepository.findUserRankAndCountInSchool(userSub, schoolId);
        if (!userSchoolRankData.isEmpty()) {
            Object[] data = userSchoolRankData.get(0);
            Long rank = ((Number) data[0]).longValue();
            Long count = ((Number) data[1]).longValue();
            response.setUserSchoolRank(new LeaderboardEntryDTO(rank.intValue(), userSub, count.intValue(), true));
        } else {
            // If user has no loans in school, set a default rank/count
            response.setUserSchoolRank(new LeaderboardEntryDTO(0, userSub, 0, true));
        }

        // User's rank in class
        if (effectiveKlasId != null) {
            List<Object[]> userClassRankData = loanRepository.findUserRankAndCountInClass(userSub, effectiveKlasId);
            if (!userClassRankData.isEmpty()) {
                Object[] data = userClassRankData.get(0);
                Long rank = ((Number) data[0]).longValue();
                Long count = ((Number) data[1]).longValue();
                response.setUserClassRank(new LeaderboardEntryDTO(rank.intValue(), userSub, count.intValue(), true));
            } else {
                // If user has no loans in class, set a default rank/count
                response.setUserClassRank(new LeaderboardEntryDTO(0, userSub, 0, true));
            }
        }

        // 5. Populate available classes for teachers/librarians
        List<LeaderboardResponseDTO.LeaderboardKlasDTO> availableClasses = klasRepository.findBySchool_Id(schoolId)
                .stream()
                .map(klas -> new LeaderboardResponseDTO.LeaderboardKlasDTO(klas.getId(), klas.getNaam()))
                .collect(Collectors.toList());
        response.setAvailableClasses(availableClasses);

        // 6. Resolve sub placeholders to display names (OneRoster batch + Smartschool
        // fallback, all in-memory cached — no names ever written to the DB).
        resolveDisplayNames(response, schoolId);

        return response;
    }

    private void resolveDisplayNames(LeaderboardResponseDTO response, Long schoolId) {
        List<LeaderboardEntryDTO> allEntries = new ArrayList<>();
        if (response.getTopClassReaders() != null)
            allEntries.addAll(response.getTopClassReaders());
        if (response.getTopSchoolReaders() != null)
            allEntries.addAll(response.getTopSchoolReaders());
        if (response.getUserClassRank() != null)
            allEntries.add(response.getUserClassRank());
        if (response.getUserSchoolRank() != null)
            allEntries.add(response.getUserSchoolRank());

        List<String> subs = allEntries.stream()
                .map(LeaderboardEntryDTO::getDisplayName)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> names = displayNameResolver.resolveAll(schoolId, subs);
        allEntries.forEach(entry -> {
            String sub = entry.getDisplayName();
            if (sub != null) {
                entry.setDisplayName(names.getOrDefault(sub, sub));
            }
        });
    }

    private List<LeaderboardEntryDTO> convertToDTO(List<Object[]> results, String currentUserSub) {
        List<LeaderboardEntryDTO> dtos = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) { // Rank is 1-based index from the ordered list
            Object[] row = results.get(i);
            String subFromQuery = (String) row[0];
            Long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;

            // Sub is used as the placeholder displayName; resolveDisplayNames overwrites
            // it with the real name (OneRoster batch + Smartschool fallback).
            dtos.add(new LeaderboardEntryDTO(
                    i + 1,
                    subFromQuery,
                    count.intValue(),
                    subFromQuery != null && subFromQuery.equals(currentUserSub)));
        }
        return dtos;
    }
}