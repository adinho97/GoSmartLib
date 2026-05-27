package com.example.demo.controllers;

import com.example.demo.entities.UserExperience;
import com.example.demo.repositories.UserExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/experience")
public class UserExperienceController {

    private static final Logger logger = LoggerFactory.getLogger(UserExperienceController.class);

    private final UserExperienceRepository userExperienceRepository;

    public UserExperienceController(UserExperienceRepository userExperienceRepository) {
        this.userExperienceRepository = userExperienceRepository;
    }

    @GetMapping
    public ResponseEntity<UserExperienceResponse> getUserExperience(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        try {
            UserExperience userExperience = findOrCreate(userSub);
            return ResponseEntity.ok(toResponse(userExperience));
        } catch (DataAccessException ex) {
            logger.error("Database error fetching user experience for userSub: {}", userSub, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException ex) {
            logger.error("Unexpected error fetching user experience for userSub: {}", userSub, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping
    public ResponseEntity<Void> saveUserExperience(
            Authentication authentication,
            @RequestBody UserExperienceRequest request) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        if (request == null || request.getTotalExperience() == null || request.getTotalExperience() < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            UserExperience userExperience = findOrCreate(userSub);

            userExperience.setTotalExperience(request.getTotalExperience());
            userExperience.setClaimedBadgeRewardsJson(sanitizeClaimedBadgeRewardsJson(request.getClaimedBadgeRewardsJson()));

            userExperienceRepository.save(userExperience);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            logger.error("Database error saving user experience for userSub: {}", userSub, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException ex) {
            logger.error("Unexpected error saving user experience for userSub: {}", userSub, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private UserExperience findOrCreate(String userSub) {
        return userExperienceRepository.findByUserSub(userSub)
                .orElseGet(() -> new UserExperience(userSub, 0, "[]"));
    }

    private String sanitizeClaimedBadgeRewardsJson(String value) {
        return value != null ? value : "[]";
    }

    private UserExperienceResponse toResponse(UserExperience userExperience) {
        UserExperienceResponse response = new UserExperienceResponse();
        response.setTotalExperience(
                userExperience.getTotalExperience() != null ? userExperience.getTotalExperience() : 0);
        response.setClaimedBadgeRewardsJson(
                sanitizeClaimedBadgeRewardsJson(userExperience.getClaimedBadgeRewardsJson()));
        return response;
    }

    public static class UserExperienceRequest {
        private Integer totalExperience;
        private String claimedBadgeRewardsJson;

        public Integer getTotalExperience() {
            return totalExperience;
        }

        public void setTotalExperience(Integer totalExperience) {
            this.totalExperience = totalExperience;
        }

        public String getClaimedBadgeRewardsJson() {
            return claimedBadgeRewardsJson;
        }

        public void setClaimedBadgeRewardsJson(String claimedBadgeRewardsJson) {
            this.claimedBadgeRewardsJson = claimedBadgeRewardsJson;
        }
    }

    public static class UserExperienceResponse {
        private Integer totalExperience;
        private String claimedBadgeRewardsJson;

        public Integer getTotalExperience() {
            return totalExperience;
        }

        public void setTotalExperience(Integer totalExperience) {
            this.totalExperience = totalExperience;
        }

        public String getClaimedBadgeRewardsJson() {
            return claimedBadgeRewardsJson;
        }

        public void setClaimedBadgeRewardsJson(String claimedBadgeRewardsJson) {
            this.claimedBadgeRewardsJson = claimedBadgeRewardsJson;
        }
    }
}
