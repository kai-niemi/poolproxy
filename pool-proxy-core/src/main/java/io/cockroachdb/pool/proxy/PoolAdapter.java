package io.cockroachdb.pool.proxy;

import java.sql.SQLException;
import java.time.Duration;

import io.cockroachdb.pool.proxy.model.PoolMetrics;

/**
 * Pool configurator to be implemented by different connection pool adapters.
 *
 * @author Kai Niemi
 */
public interface PoolAdapter {
    /**
     * Apply specified pool size dimensions.
     *
     * @param minIdle min idle pool size
     * @param maxSize max pool size
     * @param maxLifeTime max connection lifetime
     * @throws SQLException on any communication/configuration errors
     */
    void applyPoolConfiguration(int minIdle, int maxSize, Duration maxLifeTime) throws SQLException;

    PoolMetrics collectPoolMetrics() throws SQLException;
}
