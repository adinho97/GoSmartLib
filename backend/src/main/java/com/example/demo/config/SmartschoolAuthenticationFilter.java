package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Authenticates requests using Smartschool access tokens.
 * 
 * With graceful degradation:
 * - If database is unavailable, does NOT throw exception
 * - Just continues without setting authentication (lets other filters/error
 * handlers deal with it)
 * - Prevents security filter from cascading failures when DB is down
 */
@Component
public class SmartschoolAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SmartschoolAuthenticationFilter.class);

    private final AppUserRepository appUserRepository;
    private final ConnectionPoolMonitor poolMonitor;

    public SmartschoolAuthenticationFilter(AppUserRepository appUserRepository,
            ConnectionPoolMonitor poolMonitor) {
        this.appUserRepository = appUserRepository;
        this.poolMonitor = poolMonitor;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // If already authenticated, skip
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token != null) {
            try {
                // Check pool health before attempting DB access
                ConnectionPoolMonitor.PoolMetrics metrics = poolMonitor.getMetrics();
                if (metrics.pendingThreads > 5 || metrics.utilizationPercent > 95) {
                    logger.warn("Skipping token auth due to pool stress: utilization={}%, pending={}",
                            String.format("%.0f", metrics.utilizationPercent),
                            metrics.pendingThreads);
                    // Don't authenticate, let framework return 401/403
                } else {
                    Optional<AppUser> userOpt = appUserRepository.findByAccessToken(token);
                    if (userOpt.isPresent()) {
                        AppUser user = userOpt.get();
                        if (user.isActive()) {
                            String authority = "ROLE_" + user.getRole().toUpperCase();
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    user.getSub(),
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(authority)));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.debug("Smartschool token authenticated for user: {}", user.getSub());
                        } else {
                            logger.debug("Smartschool token matched inactive user: {}", user.getSub());
                        }
                    }
                }
            } catch (Exception ex) {
                // DB might be unavailable. Log but don't rethrow.
                // Graceful degradation: fail the request with proper error handling,
                // not with an uncaught exception.
                String cause = ex.getClass().getSimpleName();
                if (cause.contains("Transient") || cause.contains("Connection")) {
                    logger.warn("Could not authenticate Smartschool token due to database issue: {}", cause);
                } else {
                    logger.debug("Could not authenticate Smartschool token", ex);
                }
                // Don't set authentication - let the request fail at endpoint with proper error
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
