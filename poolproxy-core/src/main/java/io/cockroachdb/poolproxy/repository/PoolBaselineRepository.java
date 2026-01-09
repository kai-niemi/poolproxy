package io.cockroachdb.poolproxy.repository;

import java.util.Optional;

import io.cockroachdb.poolproxy.model.PoolBaseline;

/**
 * @author Kai Niemi
 */
public interface PoolBaselineRepository {
    /**
     * Retrieves pool configuration baseline by name.
     *
     * @return the baseline with the given name or {@literal Optional#empty()} if none found.
     */
    Optional<PoolBaseline> findByName(String name);

    void update(PoolBaseline baseline);
}
