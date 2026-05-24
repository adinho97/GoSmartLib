package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.services.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Integration adapter for OAuth refresh handling in AuthService.
 * 
 * This component provides the bridge between AuthService and
 * OAuthRefreshTokenHandler,
 * ensuring that getUserInfoBySub() uses the production-grade resilience
 * patterns.
 * 
 * Usage in AuthService.java:
 * ```java
 * 
 * @Autowired
 *            private OAuthRefreshIntegration oAuthRefreshIntegration;
 * 
 *            public Mono<SmartschoolUserInfo> getUserInfoBySub(String sub) {
 *            return oAuthRefreshIntegration.getUserInfoWithRefresh(sub, this);
 *            }
 *            ```
 */
@Component
public class OAuthRefreshIntegration {

    private static final Logger logger = LoggerFactory.getLogger(OAuthRefreshIntegration.class);

    private final AppUserRepository appUserRepository;
    private final OAuthRefreshTokenHandler refreshTokenHandler;

    public OAuthRefreshIntegration(
            AppUserRepository appUserRepository,
            OAuthRefreshTokenHandler refreshTokenHandler) {
        this.appUserRepository = appUserRepository;
        this.refreshTokenHandler = refreshTokenHandler;
    }

    /**
     * Safe wrapper for getting user info with token refresh.
     * 
     * Replaces direct call to AuthService.refreshAccessToken() in
     * getUserInfoBySub().
     * 
     * @param sub         User's Smartschool ID
     * @param authService Reference to AuthService for getting user info
     * @return User info Mono, or error if token is revoked
     */
    public Mono<SmartschoolUserInfo> getUserInfoWithRefresh(String sub, AuthService authService) {
        logger.debug("Getting user info for sub: {} with resilient refresh", sub);

        return Mono.justOrEmpty(appUserRepository.findBySub(sub))
                .switchIfEmpty(Mono.error(new ApiException(
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND")))
                .flatMap(user -> {
                    if (user.getSmartschoolRefreshToken() == null) {
                        logger.error("No refresh token available for user: {}", sub);
                        return Mono.error(new ApiException(
                                "No refresh token available",
                                HttpStatus.UNAUTHORIZED,
                                "NO_REFRESH_TOKEN"));
                    }

                    // Use resilient refresh handler
                    return refreshTokenHandler.refreshAccessTokenSafely(sub, user.getSmartschoolRefreshToken())
                            .<SmartschoolUserInfo>flatMap(newAccessToken -> {
                                // Update user's access token
                                user.setAccessToken(newAccessToken);
                                appUserRepository.save(user);
                                logger.debug("Updated access token for user: {}", sub);

                                // Get user info using new token
                                com.example.demo.config.SmartschoolTokenResponse tokenResponse = new com.example.demo.config.SmartschoolTokenResponse();
                                tokenResponse.setAccessToken(newAccessToken);
                                return authService.getUserInfo(tokenResponse, user.getPlatform());
                            })
                            .onErrorResume(error -> {
                                // If token revoked, error is already logged by handler
                                return Mono.error(error);
                            });
                });
    }

    /**
     * Helper class for testing integration without AuthService circular dependency.
     * Used internally during token refresh.
     */
    static class SmartschoolTokenResponse {
        private String accessToken;
        private String refreshToken;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
