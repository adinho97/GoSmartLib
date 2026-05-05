package com.example.demo;

import com.example.demo.config.JwtAuthenticationFilter;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.entities.SuperAdmin;
import com.example.demo.repositories.SuperAdminRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private SuperAdminRepository superAdminRepository;

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider"), "jwtSecret",
                "test-secret-key-for-hs512-must-be-at-least-64-bytes-long-1234567890");
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider"),
            "jwtExpirationInMs", 86_400_000);
        filter = new JwtAuthenticationFilter(jwtTokenProvider, superAdminRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAuthenticateWhenTokenVersionMatches() throws Exception {
        SuperAdmin admin = new SuperAdmin();
        admin.setId(10L);
        admin.setTokenVersion(2L);
        when(superAdminRepository.findById(10L)).thenReturn(Optional.of(admin));

        String token = jwtTokenProvider.generateToken("admin@example.com", 10L, "super_admin", 2L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldRejectStaleTokenAfterPasswordRotation() throws Exception {
        SuperAdmin admin = new SuperAdmin();
        admin.setId(10L);
        admin.setTokenVersion(3L);
        when(superAdminRepository.findById(10L)).thenReturn(Optional.of(admin));

        String staleToken = jwtTokenProvider.generateToken("admin@example.com", 10L, "super_admin", 2L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + staleToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
