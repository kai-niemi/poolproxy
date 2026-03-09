package io.cockroachdb.pool.proxy.model;

/**
 * Value object defining a baseline configuration for connection pools.
 *
 * @author Kai Niemi
 */
public class PoolBaseline {
    public static PoolBaseline withDefaults() {
        PoolBaseline baseline = new PoolBaseline();
        baseline.setLivenessInterval(5 * 60);
        baseline.setConnectionTimeout(5 * 60);
        baseline.setMinIdle(4);
        baseline.setMaxSize(512);
        baseline.setPoolingStrategy(PoolStrategy.DYNAMIC_SIZE);
        return baseline;
    }

    private String name;

    private int livenessInterval;

    private int connectionTimeout;

    private int minIdle;

    private int maxSize;

    private PoolStrategy poolStrategy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLivenessInterval() {
        return livenessInterval;
    }

    public void setLivenessInterval(int livenessInterval) {
        this.livenessInterval = livenessInterval;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
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

    public PoolStrategy getPoolingStrategy() {
        return poolStrategy;
    }

    public void setPoolingStrategy(PoolStrategy poolStrategy) {
        this.poolStrategy = poolStrategy;
    }

    @Override
    public String toString() {
        return "PoolBaseline{" +
               "name='" + name + '\'' +
               ", livenessInterval=" + livenessInterval +
               ", connectionTimeout=" + connectionTimeout +
               ", minIdle=" + minIdle +
               ", maxSize=" + maxSize +
               ", poolingStrategy=" + poolStrategy +
               '}';
    }
}
