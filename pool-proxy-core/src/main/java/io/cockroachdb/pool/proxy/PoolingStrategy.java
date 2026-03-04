package io.cockroachdb.pool.proxy;

import java.time.Duration;

import io.cockroachdb.pool.proxy.model.ClusterInfo;
import io.cockroachdb.pool.proxy.model.PoolBaseline;

/**
 * Enumeration of different pool size/timeout configuration strategies.
 *
 * @author Kai Niemi
 */
public enum PoolingStrategy {
    DYNAMIC_SIZE {
        @Override
        int minIdle(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            final int vCPUs = clusterInfo.getNumVCPUs() * 4 / Math.min(baseline.getMaxSize(), numPools);
            return (int) Math.ceil(vCPUs * 0.25);
        }

        @Override
        int maxSize(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            final int vCPUs = clusterInfo.getNumVCPUs() * 4 / Math.min(baseline.getMaxSize(), numPools);
            return Math.min(baseline.getMaxSize(), vCPUs);
        }

        @Override
        Duration maxLifetime(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            return Duration.ofSeconds(baseline.getConnectionTimeout())
                    .minusSeconds(5);
        }
    },
    FIXED_SIZE {
        @Override
        int minIdle(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            return maxSize(clusterInfo, baseline, numPools);
        }

        @Override
        int maxSize(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            final int vCPUs = clusterInfo.getNumVCPUs() * 4 / Math.min(baseline.getMaxSize(), numPools);
            return Math.min(baseline.getMaxSize(), vCPUs);
        }

        @Override
        Duration maxLifetime(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools) {
            return Duration.ofSeconds(baseline.getConnectionTimeout())
                    .minusSeconds(5);
        }
    };

    abstract int minIdle(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools);

    abstract int maxSize(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools);

    abstract Duration maxLifetime(ClusterInfo clusterInfo, PoolBaseline baseline, int numPools);
}
