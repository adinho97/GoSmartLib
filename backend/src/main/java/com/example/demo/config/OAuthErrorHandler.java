package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Detects and categorizes OAuth-related errors, particularly revoked/invalid
 * tokens.
 * 
 * Handles:
 * - 401 UNAUTHORIZED (token revoked, expired, or invalid)
 * - 400 BAD REQUEST (refresh token expired or revoked)
 * - Network failures (transient vs permanent)
 */
@Component
public class OAuthErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuthErrorHandler.class);

    /**
     * Determines if an error is due to a revoked or invalid refresh token.
     * 
     * Revoked tokens typically return:
     * - 401 UNAUTHORIZED from Smartschool API
     * - 400 BAD REQUEST with "invalid_grant" or "revoked" error
     * 
     * @param statusCode HTTP status code from Smartschool
     * @param body       Response body (may contain error details)
     * @return true if token is permanently invalid/revoked
     */
    public boolean isTokenRevoked(HttpStatusCode statusCode, String body) {
        if (statusCode == null) {
            return false;
        }

        // 401 indicates auth failed (token invalid/expired/revoked)
        if (statusCode.value() == 401) {
            logger.warn("OAuth endpoint returned 401: token is revoked or invalid");
            return true;
        }

        // 400 with specific error messages
        if (statusCode.value() == 400 && body != null) {
            String lowerBody = body.toLowerCase();
            if (lowerBody.contains("invalid_grant") ||
                    lowerBody.contains("revoked") ||
                    lowerBody.contains("expired")) {
                logger.warn("OAuth endpoint returned 400 with error indicating revoked/invalid token: {}",
                        lowerBody.substring(0, Math.min(200, lowerBody.length())));
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if an error is transient (retry-worthy) or permanent.
     * 
     * Transient:
     * - 408 REQUEST TIMEOUT
     * - 429 TOO MANY REQUESTS
     * - 5xx SERVER ERRORS
     * - Network timeouts/connection refused
     * 
     * Permanent:
     * - 401 UNAUTHORIZED (revoked token)
     * - 400 BAD REQUEST with invalid_grant (expired refresh token)
     * - 403 FORBIDDEN
     */
    public boolean isTransientError(HttpStatusCode statusCode, Throwable error) {
        if (statusCode != null) {
            int code = statusCode.value();
            // Transient: timeouts, rate limit, server errors
            if (code == 408 || code == 429 || (code >= 500 && code < 600)) {
                logger.debug("Transient HTTP error (retryable): {}", code);
                return true;
            }
            // Permanent: auth errors, client errors (except timeout)
            if ((code >= 400 && code < 500) && code != 408 && code != 429) {
                logger.debug("Permanent HTTP error (not retryable): {}", code);
                return false;
            }
        }

        // Check for network-level transient errors
        if (error != null) {
            String errorType = error.getClass().getSimpleName();
            // Transient network errors
            if (errorType.contains("Timeout") ||
                    errorType.contains("ConnectException") ||
                    errorType.contains("ConnectionRefused") ||
                    errorType.contains("SocketException")) {
                logger.debug("Transient network error (retryable): {}", errorType);
                return true;
            }
        }

        return false;
    }

    /**
     * Suggests appropriate action for an OAuth failure.
     * 
     * @return Action recommendation
     */
    public OAuthErrorAction categorizeError(HttpStatusCode statusCode, String body, Throwable error) {
        if (statusCode == null) {
            return OAuthErrorAction.RETRY_WITH_BACKOFF; // Network error
        }

        if (isTokenRevoked(statusCode, body)) {
            return OAuthErrorAction.REVOKED_TOKEN; // Force logout
        }

        if (isTransientError(statusCode, error)) {
            return OAuthErrorAction.RETRY_WITH_BACKOFF;
        }

        return OAuthErrorAction.PERMANENT_ERROR; // Give up
    }

    public enum OAuthErrorAction {
        REVOKED_TOKEN, // Token invalid/revoked - force logout immediately
        RETRY_WITH_BACKOFF, // Transient error - retry with exponential backoff
        PERMANENT_ERROR, // Permanent failure - don't retry
        RATE_LIMITED, // Too many requests - wait longer
    }
}
