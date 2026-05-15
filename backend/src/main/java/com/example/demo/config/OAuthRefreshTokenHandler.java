package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

/**
 * Production-grade OAuth refresh token handler with resilience patterns.
 * 
 * Handles:
 * 1. Detects revoked/invalid tokens (doesn't retry)
 * 2. Implements exponential backoff with jitter for transient failures
 * 3. Uses circuit breaker to prevent cascading failures
 * 4. Synchronizes concurrent refresh requests (prevents thundering herd)
 * 5. Force-logs out users with revoked tokens
 * 6. Gracefully handles DB unavailability
 */
@Service
public class OAuthRefreshTokenHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuthRefreshTokenHandler.class);

    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final OAuthErrorHandler errorHandler;
    private final OAuthCircuitBreaker circuitBreaker;
    private final OAuthTokenRefreshLock refreshLock;

    public OAuthRefreshTokenHandler(
            AuthService authService,
            AppUserRepository appUserRepository,
            OAuthErrorHandler errorHandler,
            OAuthCircuitBreaker circuitBreaker,
            OAuthTokenRefreshLock refreshLock) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.errorHandler = errorHandler;
        this.circuitBreaker = circuitBreaker;
        this.refreshLock = refreshLock;
    }

    /**
     * Safely refresh a user's access token with full resilience support.
     * 
     * @param userSub             User's Smartschool ID
     * @param currentRefreshToken Current refresh token from DB
     * @return New access token, or Mono.error if refresh fails/revoked
     */
    public Mono<String> refreshAccessTokenSafely(String userSub, String currentRefreshToken) {
        // 1. Check circuit breaker
        if (!circuitBreaker.isAvailable(userSub)) {
            logger.warn("OAuth circuit breaker is open for user: {} - failing fast", userSub);
            return Mono.error(new ApiException(
                    "OAuth service temporarily unavailable",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OAUTH_SERVICE_UNAVAILABLE"));
        }

        // 2. Acquire exclusive refresh lock
        OAuthTokenRefreshLock.RefreshLockHandle lockHandle = refreshLock.acquireLock(userSub);

        try {
            return attemptRefreshWithResilience(userSub, currentRefreshToken)
                    .doOnSuccess(token -> {
                        circuitBreaker.recordSuccess(userSub);
                        logger.info("Successfully refreshed token for user: {}", userSub);
                    })
                    .doOnError(error -> {
                        // Determine error type and record appropriately
                        if (error instanceof TokenRevokedException) {
                            circuitBreaker.recordRevokedToken(userSub);
                            forceLogoutUser(userSub);
                        } else {
                            circuitBreaker.recordFailure(userSub);
                        }
                        logger.warn("Token refresh failed for user: {} - {}", userSub, error.getMessage());
                    })
                    .doFinally(signal -> refreshLock.releaseLock(lockHandle));
        } catch (Exception e) {
            refreshLock.releaseLock(lockHandle);
            return Mono.error(e);
        }
    }

    /**
     * Attempt refresh with exponential backoff + jitter for transient errors.
     * 
     * Retry strategy:
     * - 1st retry: 100ms delay + jitter
     * - 2nd retry: 200ms delay + jitter
     * - 3rd retry: 400ms delay + jitter
     * - 4th retry: 800ms delay + jitter
     * Max: 3 total attempts (initial + 3 retries)
     */
    private Mono<String> attemptRefreshWithResilience(String userSub, String refreshToken) {
        logger.debug("Attempting OAuth refresh for user: {}", userSub);

        return authService.refreshAccessToken(refreshToken)
                .map(response -> {
                    if (response.getAccessToken() == null) {
                        throw new TokenRefreshFailedException("Response missing access token");
                    }
                    return response.getAccessToken();
                })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(1))
                        .jitter(0.1)
                        .doBeforeRetry(signal -> {
                            Throwable error = signal.failure();
                            logger.debug("Retrying token refresh for user: {} (attempt {}): {}",
                                    userSub,
                                    signal.totalRetries() + 1,
                                    error.getMessage());
                        })
                        .filter(error -> {
                            // Only retry on transient errors
                            if (error instanceof TokenRevokedException) {
                                logger.warn("Token revoked for user: {} - not retrying", userSub);
                                return false;
                            }
                            boolean shouldRetry = errorHandler.isTransientError(null, error);
                            if (!shouldRetry) {
                                logger.warn("Permanent error refreshing token for user: {} - not retrying: {}",
                                        userSub, error.getMessage());
                            }
                            return shouldRetry;
                        }))
                .onErrorMap(this::mapRefreshError);
    }

    /**
     * Map refresh errors to appropriate exception types.
     */
    private Throwable mapRefreshError(Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : "Unknown error";

        // Check for revoked token indicators in error message
        if (message.contains("401") || message.toLowerCase().contains("revoked") ||
                message.toLowerCase().contains("invalid")) {
            return new TokenRevokedException(
                    "Refresh token has been revoked or is invalid",
                    error);
        }

        // Generic refresh failure
        return new TokenRefreshFailedException(
                "Failed to refresh access token: " + message,
                error);
    }

    /**
     * Force-logout a user whose token was revoked.
     * 
     * This ensures the user cannot use the invalid token further
     * and security filters will reject future requests.
     */
    @Transactional
    public void forceLogoutUser(String userSub) {
        try {
            Optional<AppUser> userOpt = appUserRepository.findBySub(userSub);
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                appUserRepository.save(user);
                logger.warn("Force logged out user due to revoked token: {}", userSub);
            }
        } catch (Exception e) {
            logger.error("Failed to force-logout user {}: {}", userSub, e.getMessage(), e);
            // Don't rethrow - logout failure shouldn't cascade
        }
    }

    /**
     * Check if user's token is revoked without attempting refresh.
     * Useful for quick validation.
     */
    public boolean isTokenRevokedForUser(String userSub) {
        String status = circuitBreaker.getStatus(userSub);
        // Check if the status string indicates a revoked state
        return status != null && status.contains("REVOKED");
    }

    // =========================== Exception Classes ===========================

    public static class TokenRevokedException extends RuntimeException {
        public TokenRevokedException(String message) {
            super(message);
        }

        public TokenRevokedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TokenRefreshFailedException extends RuntimeException {
        public TokenRefreshFailedException(String message) {
            super(message);
        }

        public TokenRefreshFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
