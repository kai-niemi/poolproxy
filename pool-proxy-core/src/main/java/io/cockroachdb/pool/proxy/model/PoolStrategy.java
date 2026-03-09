package io.cockroachdb.pool.proxy.model;

import java.time.Duration;

/**
 * Enumeration of different pool size/timeout configuration strategies.
 *
 * @author Kai Niemi
 */
public enum PoolStrategy {
    DYNAMIC_SIZE {
        @Override
        public int minIdle(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            final int vCPUs = clusterInfo.getNumVCPUs() * 4 / Math.min(baseline.getMaxSize(), numPools);
            return (int) Math.ceil(vCPUs * 0.25);
        }

        @Override
        public int maxSize(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            final int vCPUs = clusterInfo.getNumVCPUs() * 4 / Math.min(baseline.getMaxSize(), numPools);
            return Math.min(baseline.getMaxSize(), vCPUs);
        }

        @Override
        public Duration maxLifetime(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            return Duration.ofSeconds(baseline.getConnectionTimeout())
                    .minusSeconds(5);
        }
    },
    FIXED_SIZE {
        @Override
        public int minIdle(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            return maxSize(clusterInfo, baseline, numPools);
        }

        @Override
        public int maxSize(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            final int vCPUs = clusterInfo.getNumVCPUs() * 4 / Math.min(baseline.getMaxSize(), numPools);
            return Math.min(baseline.getMaxSize(), vCPUs);
        }

        @Override
        public Duration maxLifetime(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            return Duration.ofSeconds(baseline.getConnectionTimeout())
                    .minusSeconds(5);
        }
    };

    public abstract int minIdle(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools);

    public abstract int maxSize(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools);

    public abstract Duration maxLifetime(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools);
}
