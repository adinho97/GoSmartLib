package com.example.demo.security.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Circuit breaker pattern for OAuth refresh token endpoint.
 * 
 * Prevents cascading failures when Smartschool OAuth service is down:
 * - Tracks failure count and timing
 * - Opens circuit after N failures within time window
 * - Returns immediate errors when circuit is open (fail-fast)
 * - Half-open state allows single retry to probe recovery
 * 
 * Per-user circuit: if user's refresh token fails, only that user's requests
 * are blocked.
 */
@Component
public class OAuthCircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(OAuthCircuitBreaker.class);

    private static final int FAILURE_THRESHOLD = 5; // Open circuit after 5 failures
    private static final long TIME_WINDOW_MS = 60_000; // Per 60 seconds
    private static final long OPEN_DURATION_MS = 30_000; // Keep circuit open for 30 seconds
    private static final long HALF_OPEN_TIMEOUT_MS = 10_000; // Timeout for probe request

    private final Map<String, CircuitState> states = new ConcurrentHashMap<>();

    public boolean isAvailable(String userSub) {
        CircuitState state = states.getOrDefault(userSub, new CircuitState());
        return state.isAvailable();
    }

    /**
     * Record a successful OAuth refresh.
     * Resets the circuit to CLOSED state.
     */
    public void recordSuccess(String userSub) {
        states.put(userSub, new CircuitState());
        logger.debug("OAuth circuit breaker reset for user: {}", userSub);
    }

    /**
     * Record a failure in OAuth refresh.
     * May transition circuit from CLOSED -> OPEN, or HALF_OPEN -> OPEN.
     */
    public void recordFailure(String userSub) {
        CircuitState state = states.compute(userSub, (key, existing) -> {
            if (existing == null) {
                existing = new CircuitState();
            }
            existing.recordFailure();
            return existing;
        });

        if (state.getStatus() == Status.OPEN) {
            logger.warn("OAuth circuit breaker opened for user: {} (failures: {}, window: {}ms)",
                    userSub, state.failureCount, TIME_WINDOW_MS);
        }
    }

    /**
     * Record a revoked token.
     * Immediately opens the circuit and disables further retries for this user.
     */
    public void recordRevokedToken(String userSub) {
        CircuitState state = states.compute(userSub, (key, existing) -> {
            if (existing == null) {
                existing = new CircuitState();
            }
            existing.recordRevoked();
            return existing;
        });
        logger.warn("OAuth circuit breaker REVOKED for user: {} - immediate failure mode active", userSub);
    }

    /**
     * Get detailed status for monitoring/logging.
     */
    public String getStatus(String userSub) {
        CircuitState state = states.get(userSub);
        if (state == null) {
            return "CLOSED (no history)";
        }
        return state.toString();
    }

    // =========================== Circuit State Machine ===========================

    private static class CircuitState {
        private Status status = Status.CLOSED;
        private long failureCount = 0;
        private long firstFailureTime = 0;
        private long openedAt = 0;
        private long lastProbeTime = 0;

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        boolean isAvailable() {
            lock.readLock().lock();
            try {
                switch (status) {
                    case CLOSED:
                        return true;
                    case OPEN:
                        // Check if circuit should transition to HALF_OPEN
                        if (System.currentTimeMillis() - openedAt > OPEN_DURATION_MS) {
                            return true; // Try half-open
                        }
                        return false;
                    case HALF_OPEN:
                        return true; // Allow one request to probe recovery
                    case REVOKED:
                        return false; // Permanently unavailable
                    default:
                        return true;
                }
            } finally {
                lock.readLock().unlock();
            }
        }

        void recordFailure() {
            lock.writeLock().lock();
            try {
                long now = System.currentTimeMillis();

                if (status == Status.CLOSED) {
                    if (firstFailureTime == 0) {
                        firstFailureTime = now;
                        failureCount = 1;
                    } else if (now - firstFailureTime < TIME_WINDOW_MS) {
                        failureCount++;
                        if (failureCount >= FAILURE_THRESHOLD) {
                            status = Status.OPEN;
                            openedAt = now;
                        }
                    } else {
                        // Window expired, reset
                        firstFailureTime = now;
                        failureCount = 1;
                    }
                } else if (status == Status.HALF_OPEN) {
                    status = Status.OPEN;
                    openedAt = now;
                    failureCount++;
                }
                // If REVOKED, stay REVOKED
            } finally {
                lock.writeLock().unlock();
            }
        }

        void recordRevoked() {
            lock.writeLock().lock();
            try {
                status = Status.REVOKED;
                failureCount = Long.MAX_VALUE; // Permanent
            } finally {
                lock.writeLock().unlock();
            }
        }

        Status getStatus() {
            lock.readLock().lock();
            try {
                return status;
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public String toString() {
            lock.readLock().lock();
            try {
                return String.format("CircuitBreaker(status=%s, failures=%d, openedAt=%d)",
                        status, failureCount, openedAt);
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    private enum Status {
        CLOSED, // Normal operation
        OPEN, // Failing, reject requests
        HALF_OPEN, // Recovery probe in progress
        REVOKED, // Permanent failure (token revoked)
    }
}
