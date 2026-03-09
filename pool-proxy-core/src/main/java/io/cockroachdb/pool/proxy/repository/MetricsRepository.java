package io.cockroachdb.pool.proxy.repository;

import java.util.Optional;

import io.cockroachdb.pool.proxy.model.PoolMetrics;

public interface MetricsRepository {
    void createOrUpdate(PoolMetrics poolMetrics);

    void deleteByName(String name);

    Optional<PoolMetrics> findByName(String name);

    Integer countAll();
}
