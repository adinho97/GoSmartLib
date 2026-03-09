package com.example.demo.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very simple in-memory sliding window rate limiter.
 * Not clustered, good enough for a single-node school project.
 */
public class InMemoryRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Map<String, Deque<Instant>> requestsPerKey = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    /**
     * @return true if the call is allowed, false if the limit is exceeded.
     */
    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(window);

        Deque<Instant> deque = requestsPerKey.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            // Remove timestamps outside the window
            while (!deque.isEmpty() && deque.peekFirst().isBefore(windowStart)) {
                deque.removeFirst();
            }

            if (deque.size() >= maxRequests) {
                return false;
            }

            deque.addLast(now);
            return true;
        }
    }
}