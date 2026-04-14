package com.example.demo.controllers;

import com.example.demo.entities.UserExperience;
import com.example.demo.repositories.UserExperienceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/experience")
public class UserExperienceController {

    private final UserExperienceRepository userExperienceRepository;

    public UserExperienceController(UserExperienceRepository userExperienceRepository) {
        this.userExperienceRepository = userExperienceRepository;
    }

    @GetMapping
    public ResponseEntity<UserExperienceResponse> getUserExperience(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserExperience userExperience = userExperienceRepository.findByUserSub(userSub)
                    .orElseGet(() -> new UserExperience(userSub, 0, "[]"));

            UserExperienceResponse response = new UserExperienceResponse();
            response.setTotalExperience(
                    userExperience.getTotalExperience() != null ? userExperience.getTotalExperience() : 0);
            response.setClaimedBadgeRewardsJson(
                    userExperience.getClaimedBadgeRewardsJson() != null
                            ? userExperience.getClaimedBadgeRewardsJson()
                            : "[]");

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping
    public ResponseEntity<Void> saveUserExperience(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestBody UserExperienceRequest request) {
        if (userSub == null || userSub.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request == null || request.getTotalExperience() == null || request.getTotalExperience() < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            UserExperience userExperience = userExperienceRepository.findByUserSub(userSub)
                    .orElseGet(() -> new UserExperience(userSub, 0, "[]"));

            userExperience.setTotalExperience(request.getTotalExperience());
            userExperience.setClaimedBadgeRewardsJson(
                    request.getClaimedBadgeRewardsJson() != null
                            ? request.getClaimedBadgeRewardsJson()
                            : "[]");

            userExperienceRepository.save(userExperience);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
