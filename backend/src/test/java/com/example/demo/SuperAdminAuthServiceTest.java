package com.example.demo;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.services.SuperAdminAuthService;
import com.example.demo.config.SuperAdminLoginRequest;
import com.example.demo.config.SuperAdminLoginResponse;
import com.example.demo.entities.SuperAdmin;
import com.example.demo.entities.SuperAdminSetupToken;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.repositories.SuperAdminSetupTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class SuperAdminAuthServiceTest {

    @Mock
    private SuperAdminRepository superAdminRepository;

    @Mock
    private SuperAdminSetupTokenRepository setupTokenRepository;

    private JwtTokenProvider jwtTokenProvider;
    private SuperAdminAuthService service;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "test-secret-key-for-hs512-must-be-at-least-64-bytes-long-1234567890");
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider"),
            "jwtExpirationInMs", 86_400_000);
        service = new SuperAdminAuthService(superAdminRepository, setupTokenRepository, jwtTokenProvider);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void createSuperAdminFromSetupToken_shouldCreateAdminAndMarkTokenUsed() throws Exception {
        String rawToken = "setup-token-123";
        String email = "admin@example.com";
        String password = "StrongPass1";

        SuperAdminSetupToken setupToken = new SuperAdminSetupToken();
        setupToken.setTokenHash(sha256(rawToken));
        setupToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        setupToken.setUsedAt(null);

        when(superAdminRepository.count()).thenReturn(0L);
        when(setupTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(setupToken));
        when(superAdminRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(superAdminRepository.save(any(SuperAdmin.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(setupTokenRepository.save(any(SuperAdminSetupToken.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SuperAdmin created = service.createSuperAdminFromSetupToken(rawToken, email, password);

        assertEquals(email, created.getEmail());
        assertTrue(passwordEncoder.matches(password, created.getPasswordHash()));
        assertNotNull(setupToken.getUsedAt());
    }

    @Test
    void login_shouldIssueJwtWithTokenVersionClaim() {
        SuperAdmin admin = new SuperAdmin();
        admin.setId(42L);
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("StrongPass1"));
        admin.setTokenVersion(3L);

        when(superAdminRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        SuperAdminLoginResponse response = service.login(
                new SuperAdminLoginRequest("admin@example.com", "StrongPass1"));

        assertNotNull(response.getToken());
        assertTrue(service.validateJwtToken(response.getToken()));
        assertEquals(42L, service.getUserIdFromToken(response.getToken()));
        assertEquals("admin@example.com", service.getUsernameFromToken(response.getToken()));
        assertEquals("super_admin", service.getRoleFromToken(response.getToken()));
        assertEquals(3L, jwtTokenProvider.getTokenVersionFromToken(response.getToken()));
    }

    @Test
    void changeSuperAdminPassword_shouldRotateTokenVersionAndUpdatePasswordHash() {
        SuperAdmin admin = new SuperAdmin();
        admin.setId(5L);
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("OldPass1"));
        admin.setTokenVersion(0L);

        when(superAdminRepository.findById(5L)).thenReturn(Optional.of(admin));
        when(superAdminRepository.save(any(SuperAdmin.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.changeSuperAdminPassword(5L, "OldPass1", "NewPass1A");

        ArgumentCaptor<SuperAdmin> captor = ArgumentCaptor.forClass(SuperAdmin.class);
        verify(superAdminRepository).save(captor.capture());
        SuperAdmin saved = captor.getValue();

        assertEquals(1L, saved.getTokenVersion());
        assertTrue(passwordEncoder.matches("NewPass1A", saved.getPasswordHash()));
        assertNotNull(saved.getUpdatedAt());
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
