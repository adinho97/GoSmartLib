package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Synchronizes OAuth refresh token requests to prevent thundering herd.
 * 
 * Problem: When one request tries to refresh a token and fails, subsequent
 * requests immediately also try to refresh, causing N concurrent refresh
 * attempts on the same token.
 * 
 * Solution: Lock per user. Only ONE request can refresh a user's token at a
 * time.
 * Other requests wait for the result, then use the refreshed token.
 * 
 * This prevents:
 * - Multiple simultaneous refresh attempts
 * - Wasted API calls to Smartschool
 * - Connection pool exhaustion from failed refresh storms
 */
@Component
public class OAuthTokenRefreshLock {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenRefreshLock.class);

    private final Map<String, RefreshLock> locks = new ConcurrentHashMap<>();

    /**
     * Acquire exclusive lock for refreshing user's token.
     * Only call acquire() once per refresh operation, then call release().
     * 
     * @param userSub User's Smartschool ID
     * @return Lock handle - MUST call release(handle) when done
     */
    public RefreshLockHandle acquireLock(String userSub) {
        RefreshLock lock = locks.computeIfAbsent(userSub, k -> new RefreshLock());
        lock.acquireWriteLock();
        logger.debug("Acquired refresh lock for user: {}", userSub);
        return new RefreshLockHandle(userSub, lock);
    }

    /**
     * Release lock after refresh attempt (success or failure).
     * MUST be called to prevent deadlocks.
     */
    public void releaseLock(RefreshLockHandle handle) {
        if (handle != null) {
            handle.lock.releaseWriteLock();
            logger.debug("Released refresh lock for user: {}", handle.userSub);
        }
    }

    /**
     * Read-only wait: blocks until any ongoing refresh completes.
     * Used by other requests to wait for a concurrent refresh.
     * 
     * @param userSub   User's Smartschool ID
     * @param timeoutMs Max milliseconds to wait
     * @return true if refresh completed, false if timed out
     */
    public boolean waitForRefresh(String userSub, long timeoutMs) {
        RefreshLock lock = locks.get(userSub);
        if (lock == null) {
            return true; // No ongoing refresh
        }
        return lock.waitForRefresh(timeoutMs);
    }

    // =========================== Inner Classes ===========================

    public static class RefreshLockHandle {
        final String userSub;
        final RefreshLock lock;

        RefreshLockHandle(String userSub, RefreshLock lock) {
            this.userSub = userSub;
            this.lock = lock;
        }
    }

    private static class RefreshLock {
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private volatile long lastRefreshCompleted = System.currentTimeMillis();

        void acquireWriteLock() {
            rwLock.writeLock().lock();
        }

        void releaseWriteLock() {
            lastRefreshCompleted = System.currentTimeMillis();
            rwLock.writeLock().unlock();
        }

        boolean waitForRefresh(long timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (rwLock.readLock().tryLock()) {
                    try {
                        return true; // Refresh is complete
                    } finally {
                        rwLock.readLock().unlock();
                    }
                }
                try {
                    Thread.sleep(10); // Poll every 10ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false; // Timed out
        }
    }
}
