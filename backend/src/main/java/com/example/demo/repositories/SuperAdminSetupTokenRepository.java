package com.example.demo.repositories;

import com.example.demo.entities.SuperAdminSetupToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SuperAdminSetupTokenRepository extends JpaRepository<SuperAdminSetupToken, Long> {
    Optional<SuperAdminSetupToken> findByTokenHash(String tokenHash);

    long countByUsedAtIsNullAndExpiresAtAfter(LocalDateTime now);
}
