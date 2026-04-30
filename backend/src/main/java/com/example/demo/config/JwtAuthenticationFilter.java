package com.example.demo.config;

import com.example.demo.entities.SuperAdmin;
import com.example.demo.repositories.SuperAdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;
    private final SuperAdminRepository superAdminRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, SuperAdminRepository superAdminRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.superAdminRepository = superAdminRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                Long tokenVersion = jwtTokenProvider.getTokenVersionFromToken(token);

                SuperAdmin admin = superAdminRepository.findById(userId).orElse(null);
                Long currentTokenVersion = admin == null || admin.getTokenVersion() == null
                        ? 0L
                        : admin.getTokenVersion();
                if (admin == null || !currentTokenVersion.equals(tokenVersion)) {
                    logger.debug("JWT rejected due to stale token version for userId: {}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    String authority = "ROLE_" + role.toUpperCase();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(authority))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                // Store in request attributes for later retrieval
                request.setAttribute("jwt-username", username);
                request.setAttribute("jwt-userId", userId);
                request.setAttribute("jwt-role", role);

                logger.debug("JWT token validated for user: {} (id: {})", username, userId);
            }
        } catch (Exception ex) {
            logger.debug("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
