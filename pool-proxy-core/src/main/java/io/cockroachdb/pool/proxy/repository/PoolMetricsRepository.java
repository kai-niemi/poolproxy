package io.cockroachdb.pool.proxy.repository;

import java.util.List;
import java.util.Optional;

import io.cockroachdb.pool.proxy.model.PoolMetrics;

public interface PoolMetricsRepository {
    void createOrUpdate(PoolMetrics poolMetrics);

    void deleteByName(String name);

    Optional<PoolMetrics> findByName(String name);

    List<PoolMetrics> findAll(int limit);

    Integer countAll();
}
