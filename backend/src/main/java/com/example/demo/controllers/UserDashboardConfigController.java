package com.example.demo.controllers;

import com.example.demo.entities.UserDashboardConfig;
import com.example.demo.repositories.UserDashboardConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/dashboard-config")
public class UserDashboardConfigController {

    private static final int MAX_CONFIG_BYTES = 8 * 1024;

    private final UserDashboardConfigRepository repo;

    public UserDashboardConfigController(UserDashboardConfigRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<DashboardConfigResponse> getConfig(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        String json = repo.findByUserSub(userSub)
                .map(UserDashboardConfig::getConfigJson)
                .orElse(null);
        return ResponseEntity.ok(new DashboardConfigResponse(json));
    }

    @PutMapping
    public ResponseEntity<Void> saveConfig(
            Authentication authentication,
            @RequestBody DashboardConfigRequest request) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request == null || request.getConfigJson() == null) {
            return ResponseEntity.badRequest().build();
        }
        String json = request.getConfigJson();
        if (json.length() > MAX_CONFIG_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String userSub = authentication.getName();
        UserDashboardConfig entity = repo.findByUserSub(userSub)
                .orElse(new UserDashboardConfig(userSub, json));
        entity.setConfigJson(json);
        repo.save(entity);
        return ResponseEntity.noContent().build();
    }

    public static class DashboardConfigRequest {
        private String configJson;

        public String getConfigJson() {
            return configJson;
        }

        public void setConfigJson(String configJson) {
            this.configJson = configJson;
        }
    }

    public static class DashboardConfigResponse {
        private final String configJson;

        public DashboardConfigResponse(String configJson) {
            this.configJson = configJson;
        }

        public String getConfigJson() {
            return configJson;
        }
    }
}
