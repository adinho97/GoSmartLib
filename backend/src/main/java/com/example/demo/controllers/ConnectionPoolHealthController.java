package com.example.demo.controllers;

import com.example.demo.config.ConnectionPoolMonitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes database connection pool metrics for monitoring.
 * 
 * Endpoints:
 * - GET /api/health/db-pool - Human-readable pool status
 * - GET /api/metrics/db-pool - JSON metrics for monitoring systems
 */
@RestController
@RequestMapping("/api/health")
public class ConnectionPoolHealthController {

    private final ConnectionPoolMonitor monitor;

    public ConnectionPoolHealthController(ConnectionPoolMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Quick health check for DB pool.
     * Used by load balancers/orchestration systems to detect pool exhaustion.
     * 
     * Returns 200 OK if pool is healthy, 503 SERVICE_UNAVAILABLE if exhausted.
     */
    @GetMapping("/db-pool")
    public ResponseEntity<?> getPoolHealth() {
        ConnectionPoolMonitor.PoolMetrics metrics = monitor.getMetrics();

        // Alert if utilization > 80%
        if (metrics.utilizationPercent > 80) {
            return ResponseEntity.status(503).body(metrics);
        }

        // Alert if pending requests > 0 (thread is waiting for connection)
        if (metrics.pendingThreads > 0) {
            return ResponseEntity.status(503).body(metrics);
        }

        return ResponseEntity.ok(metrics);
    }
}
