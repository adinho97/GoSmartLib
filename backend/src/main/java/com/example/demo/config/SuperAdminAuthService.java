package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SuperAdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminAuthService.class);
    private final AppUserRepository appUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public SuperAdminAuthService(AppUserRepository appUserRepository,
                               JwtTokenProvider jwtTokenProvider) {
        this.appUserRepository = appUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticate super admin with username and password
     */
    public SuperAdminLoginResponse login(SuperAdminLoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        logger.info("Super admin login attempt for username: {}", username);

        // Find user by username
        Optional<AppUser> userOpt = appUserRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            logger.warn("Super admin login failed: user not found for username: {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        AppUser user = userOpt.get();

        // Check if user is super admin
        if (!user.getIsSuperAdmin()) {
            logger.warn("Login attempt by non-super-admin user: {}", username);
            throw new RuntimeException("User is not a super admin");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Super admin login failed: invalid password for username: {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(username, user.getId(), user.getRole());
        logger.info("Super admin login successful for username: {}, userId: {}", username, user.getId());

        return new SuperAdminLoginResponse(token, username, user.getId(), user.getRole());
    }

    /**
     * Create a new super admin (typically only called during setup/bootstrap)
     */
    public AppUser createSuperAdmin(String username, String password) {
        logger.info("Creating new super admin with username: {}", username);

        // Check if user already exists
        if (appUserRepository.findByUsername(username).isPresent()) {
            logger.warn("Super admin creation failed: username already exists: {}", username);
            throw new RuntimeException("Username already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAuthType("local");
        user.setRole("super_admin");
        user.setIsSuperAdmin(true);

        AppUser savedUser = appUserRepository.save(user);
        logger.info("Super admin created successfully with id: {}, username: {}", savedUser.getId(), username);
        return savedUser;
    }

    /**
     * Change super admin password
     */
    public void changeSuperAdminPassword(Long userId, String oldPassword, String newPassword) {
        logger.info("Password change attempt for userId: {}", userId);

        Optional<AppUser> userOpt = appUserRepository.findById(userId);

        if (userOpt.isEmpty()) {
            logger.warn("Password change failed: user not found for userId: {}", userId);
            throw new RuntimeException("User not found");
        }

        AppUser user = userOpt.get();

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            logger.warn("Password change failed: invalid old password for userId: {}", userId);
            throw new RuntimeException("Invalid current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
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
