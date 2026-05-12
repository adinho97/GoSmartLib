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

@Component
public class SmartschoolAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SmartschoolAuthenticationFilter.class);

    private final AppUserRepository appUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public SmartschoolAuthenticationFilter(AppUserRepository appUserRepository, JwtTokenProvider jwtTokenProvider) {
        this.appUserRepository = appUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token != null) {
            try {
                // First, try to find user by access token (Smartschool OAuth token)
                Optional<AppUser> userOpt = appUserRepository.findByAccessToken(token);
                if (userOpt.isPresent()) {
                    AppUser user = userOpt.get();
                    if (user.isActive()) {
                        setAuthentication(user);
                        logger.debug("Smartschool access token authenticated for user: {}", user.getSub());
                    } else {
                        logger.debug("Smartschool token matched inactive user: {}", user.getSub());
                    }
                } else {
                    // If not found as access token, try to validate as JWT (for Smartschool users with JWT tokens)
                    if (jwtTokenProvider.validateToken(token)) {
                        String sub = jwtTokenProvider.getUsernameFromToken(token);
                        Optional<AppUser> jwtUserOpt = appUserRepository.findBySub(sub);
                        if (jwtUserOpt.isPresent()) {
                            AppUser user = jwtUserOpt.get();
                            if (user.isActive()) {
                                setAuthentication(user);
                                logger.debug("JWT token authenticated for Smartschool user: {}", sub);
                            } else {
                                logger.debug("JWT token matched inactive user: {}", sub);
                            }
                        } else {
                            logger.debug("JWT token sub {} not found in app_users", sub);
                        }
                    } else {
                        logger.debug("Token validation failed - not a valid Smartschool access token or JWT");
                    }
                }
            } catch (Exception ex) {
                logger.debug("Could not authenticate token: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(AppUser user) {
        String authority = "ROLE_" + user.getRole().toUpperCase();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getSub(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
