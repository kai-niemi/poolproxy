package io.cockroachdb.poolproxy.repository;

import java.util.List;
import java.util.Optional;

import io.cockroachdb.poolproxy.model.PoolMetrics;

public interface PoolMetricsRepository {
    void createOrUpdate(PoolMetrics poolMetrics);

    void deleteByName(String name);

    Optional<PoolMetrics> findByName(String name);

    List<PoolMetrics> findAll(int limit);

    Integer countAll();
}
