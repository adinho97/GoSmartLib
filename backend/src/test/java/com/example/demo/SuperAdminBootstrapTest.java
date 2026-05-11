package com.example.demo;

import com.example.demo.config.SuperAdminBootstrap;
import com.example.demo.entities.SuperAdminSetupToken;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.repositories.SuperAdminSetupTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapTest {

    @Mock
    private SuperAdminRepository superAdminRepository;

    @Mock
    private SuperAdminSetupTokenRepository setupTokenRepository;

    @Test
    void run_shouldGenerateTokenWhenNoAdminAndNoActiveToken() throws Exception {
        when(superAdminRepository.count()).thenReturn(0L);
        when(setupTokenRepository.countByUsedAtIsNullAndExpiresAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(setupTokenRepository.save(any(SuperAdminSetupToken.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(superAdminRepository, setupTokenRepository);
        bootstrap.run();

        ArgumentCaptor<SuperAdminSetupToken> captor = ArgumentCaptor.forClass(SuperAdminSetupToken.class);
        verify(setupTokenRepository).save(captor.capture());
        SuperAdminSetupToken saved = captor.getValue();

        assertNotNull(saved.getTokenHash());
        assertTrue(saved.getTokenHash().length() >= 32);
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void run_shouldNotGenerateTokenWhenAdminExists() throws Exception {
        when(superAdminRepository.count()).thenReturn(1L);

        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(superAdminRepository, setupTokenRepository);
        bootstrap.run();

        verify(setupTokenRepository, never()).save(any(SuperAdminSetupToken.class));
    }
}
