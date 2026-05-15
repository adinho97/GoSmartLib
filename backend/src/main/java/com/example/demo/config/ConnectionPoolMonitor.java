package com.example.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

/**
 * Monitors HikariCP connection pool metrics for production visibility.
 * 
 * Tracks:
 * - Active connections (in-use)
 * - Idle connections (available)
 * - Pending threads (waiting for connection)
 * - Pool saturation percentage
 * - Total pool size
 */
@Component
public class ConnectionPoolMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolMonitor.class);

    private final DataSource dataSource;

    public ConnectionPoolMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Get current pool metrics.
     * Safe to call frequently (reads only, no locking).
     */
    public PoolMetrics getMetrics() {
        if (!(dataSource instanceof HikariDataSource)) {
            return PoolMetrics.unavailable();
        }

        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        HikariPoolMXBean poolBean = hikariDs.getHikariPoolMXBean();

        if (poolBean == null) {
            return PoolMetrics.unavailable();
        }

        int activeConnections = poolBean.getActiveConnections();
        int idleConnections = poolBean.getIdleConnections();
        int totalConnections = activeConnections + idleConnections;
        int maximumPoolSize = hikariDs.getMaximumPoolSize();
        int pendingThreads = poolBean.getThreadsAwaitingConnection();

        double utilizationPercent = maximumPoolSize > 0
                ? (activeConnections / (double) maximumPoolSize) * 100
                : 0;

        PoolMetrics metrics = new PoolMetrics(
                activeConnections,
                idleConnections,
                pendingThreads,
                totalConnections,
                maximumPoolSize,
                utilizationPercent);

        // Log warning if saturation is high
        if (utilizationPercent > 80) {
            logger.warn("Database pool saturation HIGH: {}% ({}/{} connections in use, {} threads waiting)",
                    String.format("%.1f", utilizationPercent),
                    activeConnections,
                    maximumPoolSize,
                    pendingThreads);
        }

        // Log critical if pool exhausted
        if (pendingThreads > 0 && activeConnections >= maximumPoolSize) {
            logger.error("Database pool EXHAUSTED: {} threads waiting, all {} connections in use!",
                    pendingThreads,
                    maximumPoolSize);
        }

        return metrics;
    }

    /**
     * Force log pool metrics (useful for debugging).
     */
    public void logMetrics() {
        PoolMetrics metrics = getMetrics();
        logger.info("Pool Metrics: active={}, idle={}, pending={}, total={}/{}, utilization={:.1f}%",
                metrics.activeConnections,
                metrics.idleConnections,
                metrics.pendingThreads,
                metrics.totalConnections,
                metrics.maximumPoolSize,
                metrics.utilizationPercent);
    }

    // =========================== Data Classes ===========================

    public static class PoolMetrics {
        public final int activeConnections;
        public final int idleConnections;
        public final int pendingThreads;
        public final int totalConnections;
        public final int maximumPoolSize;
        public final double utilizationPercent;
        public final boolean available;

        public PoolMetrics(int active, int idle, int pending, int total, int maximum, double utilization) {
            this.activeConnections = active;
            this.idleConnections = idle;
            this.pendingThreads = pending;
            this.totalConnections = total;
            this.maximumPoolSize = maximum;
            this.utilizationPercent = utilization;
            this.available = true;
        }

        private PoolMetrics() {
            this.activeConnections = -1;
            this.idleConnections = -1;
            this.pendingThreads = -1;
            this.totalConnections = -1;
            this.maximumPoolSize = -1;
            this.utilizationPercent = -1;
            this.available = false;
        }

        static PoolMetrics unavailable() {
            return new PoolMetrics();
        }

        @Override
        public String toString() {
            if (!available) {
                return "PoolMetrics{unavailable}";
            }
            return String.format("PoolMetrics{active=%d, idle=%d, pending=%d, utilization=%.1f%%}",
                    activeConnections, idleConnections, pendingThreads, utilizationPercent);
        }
    }
}
