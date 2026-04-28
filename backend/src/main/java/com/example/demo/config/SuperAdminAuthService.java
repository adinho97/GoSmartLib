package com.example.demo.config;

import com.example.demo.entities.SuperAdmin;
import com.example.demo.entities.SuperAdminSetupToken;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.repositories.SuperAdminSetupTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SuperAdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminAuthService.class);
    private final SuperAdminRepository superAdminRepository;
    private final SuperAdminSetupTokenRepository setupTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public SuperAdminAuthService(SuperAdminRepository superAdminRepository,
                               SuperAdminSetupTokenRepository setupTokenRepository,
                               JwtTokenProvider jwtTokenProvider) {
        this.superAdminRepository = superAdminRepository;
        this.setupTokenRepository = setupTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticate super admin with username and password
     */
    public SuperAdminLoginResponse login(SuperAdminLoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        logger.info("Super admin login attempt for identifier: {}", username);

        // Find super admin by identifier (email/username)
        Optional<SuperAdmin> adminOpt = superAdminRepository.findByEmail(username);

        if (adminOpt.isEmpty()) {
            logger.warn("Super admin login failed: user not found for identifier: {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        SuperAdmin admin = adminOpt.get();

        // Verify password
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            logger.warn("Super admin login failed: invalid password for identifier: {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(admin.getEmail(), admin.getId(), "super_admin");
        logger.info("Super admin login successful for identifier: {}, userId: {}", admin.getEmail(), admin.getId());

        return new SuperAdminLoginResponse(token, admin.getEmail(), admin.getId(), "super_admin");
    }

    /**
     * Create a new super admin (typically only called during setup/bootstrap)
     */
    public SuperAdmin createSuperAdmin(String username, String password) {
        logger.info("Creating new super admin with identifier: {}", username);

        // Check if user already exists
        if (superAdminRepository.findByEmail(username).isPresent()) {
            logger.warn("Super admin creation failed: identifier already exists: {}", username);
            throw new RuntimeException("Username already exists");
        }

        SuperAdmin admin = new SuperAdmin();
        admin.setEmail(username);
        admin.setPasswordHash(passwordEncoder.encode(password));

        SuperAdmin savedAdmin = superAdminRepository.save(admin);
        logger.info("Super admin created successfully with id: {}, identifier: {}", savedAdmin.getId(), username);
        return savedAdmin;
    }

    /**
     * Create the first super admin using a one-time setup token
     */
    public SuperAdmin createSuperAdminFromSetupToken(String token, String email, String password) {
        if (superAdminRepository.count() > 0) {
            throw new RuntimeException("Super admin already exists");
        }

        if (!isPasswordStrong(password)) {
            throw new RuntimeException("Password must be at least 8 characters and include uppercase, lowercase, and a number");
        }

        SuperAdminSetupToken setupToken = setupTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid setup token"));

        if (setupToken.getUsedAt() != null) {
            throw new RuntimeException("Setup token already used");
        }

        if (setupToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Setup token expired");
        }

        SuperAdmin admin = createSuperAdmin(email, password);
        setupToken.setUsedAt(LocalDateTime.now());
        setupTokenRepository.save(setupToken);
        return admin;
    }

    public boolean isSetupAllowed() {
        return superAdminRepository.count() == 0;
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Change super admin password
     */
    public void changeSuperAdminPassword(Long userId, String oldPassword, String newPassword) {
        logger.info("Password change attempt for userId: {}", userId);

        Optional<SuperAdmin> adminOpt = superAdminRepository.findById(userId);

        if (adminOpt.isEmpty()) {
            logger.warn("Password change failed: user not found for userId: {}", userId);
            throw new RuntimeException("User not found");
        }

        SuperAdmin admin = adminOpt.get();

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, admin.getPasswordHash())) {
            logger.warn("Password change failed: invalid old password for userId: {}", userId);
            throw new RuntimeException("Invalid current password");
        }

        if (!isPasswordStrong(newPassword)) {
            throw new RuntimeException("Password must be at least 8 characters and include uppercase, lowercase, and a number");
        }

        // Update password
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        superAdminRepository.save(admin);
        logger.info("Password changed successfully for userId: {}", userId);
    }

    /**
     * Validate JWT token
     */
    public boolean validateJwtToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * Get user ID from JWT token
     */
    public Long getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * Get role from JWT token
     */
    public String getRoleFromToken(String token) {
        return jwtTokenProvider.getRoleFromToken(token);
    }
}

