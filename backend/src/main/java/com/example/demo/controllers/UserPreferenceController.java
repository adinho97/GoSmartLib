package com.example.demo.controllers;

import com.example.demo.entities.UserPreference;
import com.example.demo.repositories.UserPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferenceController {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferenceController.class);

    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceController(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getUserPreferences(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        logger.debug("Fetching preferences for userSub: {}", userSub);

        try {
            List<UserPreference> preferences = userPreferenceRepository.findByUserSub(userSub);
            Map<String, Boolean> result = new HashMap<>();
            for (UserPreference pref : preferences) {
                result.put(pref.getPreferenceKey(), pref.getPreferenceValue());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping
    public ResponseEntity<Void> savePreference(
            Authentication authentication,
            @RequestBody PreferenceRequest request) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        logger.info("Saving preference for userSub: {}, key: {}, value: {}", userSub, request.getKey(), request.getValue());

        if (request.getKey() == null || request.getValue() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // Find existing or create new
            UserPreference pref = userPreferenceRepository
                    .findByUserSubAndPreferenceKey(userSub, request.getKey())
                    .orElse(new UserPreference(userSub, request.getKey(), request.getValue()));

            pref.setPreferenceValue(request.getValue());
            userPreferenceRepository.save(pref);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DTO for request body
    public static class PreferenceRequest {
        private String key;
        private Boolean value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Boolean getValue() {
            return value;
        }

        public void setValue(Boolean value) {
            this.value = value;
        }
    }
}
