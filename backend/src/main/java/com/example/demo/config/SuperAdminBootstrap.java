package com.example.demo.config;

import com.example.demo.repositories.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Bootstrap service to create default super admin on first startup
 * Default credentials: username="admin", password="admin" (change in production!)
 */
@Component
public class SuperAdminBootstrap implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminBootstrap.class);
    private final AppUserRepository appUserRepository;
    private final SuperAdminAuthService superAdminAuthService;

    public SuperAdminBootstrap(AppUserRepository appUserRepository,
                             SuperAdminAuthService superAdminAuthService) {
        this.appUserRepository = appUserRepository;
        this.superAdminAuthService = superAdminAuthService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Super Admin bootstrap check...");

        // Check if any super admin exists
        boolean superAdminExists = appUserRepository
                .findAll()
                .stream()
                .anyMatch(user -> user.getIsSuperAdmin() != null && user.getIsSuperAdmin());

        if (!superAdminExists) {
            logger.warn("No super admin found. Creating default super admin...");
            logger.warn("DEFAULT CREDENTIALS: username=admin, password=admin");
            logger.warn("CHANGE THESE CREDENTIALS IMMEDIATELY IN PRODUCTION!");

            try {
                superAdminAuthService.createSuperAdmin("admin", "admin");
                logger.info("Default super admin created successfully!");
                logger.info("Please change the password immediately after first login.");
            } catch (Exception e) {
                logger.error("Failed to create default super admin", e);
            }
        } else {
            logger.info("Super admin already exists. Skipping bootstrap.");
        }
    }
}
