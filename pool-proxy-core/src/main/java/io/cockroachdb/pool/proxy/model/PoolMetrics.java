package io.cockroachdb.pool.proxy.model;

import java.time.LocalDateTime;

/**
 * Value object describing metrics for a connection pool instance.
 *
 * @author Kai Niemi
 */
public class PoolMetrics {
    public static PoolMetrics withDefaults(String name) {
        PoolMetrics poolMetrics = new PoolMetrics();
        poolMetrics.setPoolName(name);
        return poolMetrics;
    }

    private String poolName;

    private LocalDateTime expiredAt;

    private String appName;

    private String appVersion;

    private int minIdle;

    private int maxSize;

    private int activeCount;

    private int idleCount;

    private int totalConnections;

    private int threadsAwaitingConnection;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getIdleCount() {
        return idleCount;
    }

    public void setIdleCount(int idleCount) {
        this.idleCount = idleCount;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
    }

    public int getThreadsAwaitingConnection() {
        return threadsAwaitingConnection;
    }

    public void setThreadsAwaitingConnection(int threadsAwaitingConnection) {
        this.threadsAwaitingConnection = threadsAwaitingConnection;
    }

    @Override
    public String toString() {
        return "PoolMetrics{" +
               "poolName='" + poolName + '\'' +
               ", expiredAt=" + expiredAt +
               ", appName='" + appName + '\'' +
               ", appVersion='" + appVersion + '\'' +
               ", minIdle=" + minIdle +
               ", maxSize=" + maxSize +
               ", activeCount=" + activeCount +
               ", idleCount=" + idleCount +
               ", totalConnections=" + totalConnections +
               ", threadsAwaitingConnection=" + threadsAwaitingConnection +
               '}';
    }
}
