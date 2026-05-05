package com.example.demo.config;

import com.example.demo.entities.SuperAdminSetupToken;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.repositories.SuperAdminSetupTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Bootstrap service to generate a one-time setup token on first startup
 */
@Component
public class SuperAdminBootstrap implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminBootstrap.class);
    private final SuperAdminRepository superAdminRepository;
    private final SuperAdminSetupTokenRepository setupTokenRepository;

    public SuperAdminBootstrap(SuperAdminRepository superAdminRepository,
                             SuperAdminSetupTokenRepository setupTokenRepository) {
        this.superAdminRepository = superAdminRepository;
        this.setupTokenRepository = setupTokenRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Super Admin bootstrap check...");

        // Check if any super admin exists
        boolean superAdminExists = superAdminRepository.count() > 0;

        if (!superAdminExists) {
            long activeTokenCount = setupTokenRepository.countByUsedAtIsNullAndExpiresAtAfter(LocalDateTime.now());
            if (activeTokenCount == 0) {
                SuperAdminSetupToken setupToken = new SuperAdminSetupToken();
                String rawToken = generateToken();
                setupToken.setTokenHash(hashToken(rawToken));
                setupToken.setExpiresAt(LocalDateTime.now().plusHours(24));
                setupTokenRepository.save(setupToken);

                logger.warn("No super admin found. Setup token generated.");
                logger.warn("Use this token once to create the first super admin:");
                logger.warn("/api/admin/setup (token: {})", rawToken);
                logger.warn("Token expires in 24 hours.");
            } else {
                logger.warn("No super admin found, but a setup token already exists.");
            }
        } else {
            logger.info("Super admin already exists. Skipping bootstrap.");
        }
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash setup token", e);
        }
    }
}

