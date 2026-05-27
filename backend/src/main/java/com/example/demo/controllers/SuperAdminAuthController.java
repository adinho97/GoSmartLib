package com.example.demo.controllers;

import com.example.demo.services.SuperAdminAuthService;
import com.example.demo.dto.SuperAdminLoginRequest;
import com.example.demo.dto.SuperAdminLoginResponse;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class SuperAdminAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminAuthController.class);

    private final SuperAdminAuthService superAdminAuthService;

    public SuperAdminAuthController(SuperAdminAuthService superAdminAuthService) {
        this.superAdminAuthService = superAdminAuthService;
    }

    /**
     * Super admin login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SuperAdminLoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }

            SuperAdminLoginResponse response = superAdminAuthService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.warn("Super admin login failed for username='{}': {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    /**
     * One-time setup endpoint for first super admin
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(@RequestBody SuperAdminSetupRequest request) {
        try {
            if (request.getToken() == null || request.getToken().isEmpty() ||
                request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Token, email, and password are required");
            }

            superAdminAuthService.createSuperAdminFromSetupToken(
                    request.getToken(),
                    request.getEmail(),
                    request.getPassword());

            return ResponseEntity.ok("Super admin created successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Super admin setup rejected: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Super admin setup failed unexpectedly ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(500).body("Setup failed: " + e.getMessage());
        }
    }

    /**
     * Check if setup is allowed (only when no super admin exists)
     */
    @GetMapping("/setup-status")
    public ResponseEntity<?> setupStatus() {
        boolean allowed = superAdminAuthService.isSetupAllowed();
        if (!allowed) {
            return ResponseEntity.status(404).body("Not found");
        }
        return ResponseEntity.ok(new SetupStatusResponse(true));
    }

    /**
     * Validate JWT token (protected endpoint - caller must include token in header)
     */
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            boolean isValid = superAdminAuthService.validateJwtToken(token);

            if (!isValid) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }

            return ResponseEntity.ok("Token is valid");
        } catch (JwtException e) {
            logger.debug("JWT validation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body("Token validation failed: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected error during token validation", e);
            return ResponseEntity.status(401).body("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Get current super admin info from token
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            
            if (!superAdminAuthService.validateJwtToken(token)) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }

            String username = superAdminAuthService.getUsernameFromToken(token);
            Long userId = superAdminAuthService.getUserIdFromToken(token);
            String role = superAdminAuthService.getRoleFromToken(token);

            AdminInfoResponse info = new AdminInfoResponse(userId, username, role);
            return ResponseEntity.ok(info);
        } catch (JwtException e) {
            logger.debug("JWT validation failed while fetching admin info: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body("Failed to get admin info: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected error fetching admin info", e);
            return ResponseEntity.status(401).body("Failed to get admin info: " + e.getMessage());
        }
    }

    /**
     * Change super admin password (requires valid JWT token)
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswordRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            
            if (!superAdminAuthService.validateJwtToken(token)) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }

            Long userId = superAdminAuthService.getUserIdFromToken(token);
            superAdminAuthService.changeSuperAdminPassword(userId, request.getOldPassword(), request.getNewPassword());

            return ResponseEntity.ok("Password changed successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Password change rejected: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected error while changing password ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to change password: " + e.getMessage());
        }
    }

    /**
     * Inner class for admin info response
     */
    public static class AdminInfoResponse {
        private Long userId;
        private String username;
        private String role;

        public AdminInfoResponse(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public static class SetupStatusResponse {
        private boolean allowed;

        public SetupStatusResponse(boolean allowed) {
            this.allowed = allowed;
        }

        public boolean isAllowed() { return allowed; }
    }

    /**
     * DTO for one-time setup request
     */
    public static class SuperAdminSetupRequest {
        private String token;
        private String email;
        private String password;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}

/**
 * DTO for change password request
 */
class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
