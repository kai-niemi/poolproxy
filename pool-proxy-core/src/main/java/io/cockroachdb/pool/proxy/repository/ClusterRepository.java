package io.cockroachdb.pool.proxy.repository;

import io.cockroachdb.pool.proxy.model.ClusterInfo;

/**
 * Repository for cluster and pooling configuration and metadata.
 *
 * @author Kai Niemi
 */
public interface ClusterRepository {
    void initSchema();

    void dropSchema();

    /**
     * Retrieves cluster information used for pool sizing.
     *
     * @return cluster information
     */
    ClusterInfo findClusterInfo();
}
