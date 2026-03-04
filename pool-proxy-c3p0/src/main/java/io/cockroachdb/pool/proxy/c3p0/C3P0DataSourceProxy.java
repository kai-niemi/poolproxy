package io.cockroachdb.pool.proxy.c3p0;

import java.sql.SQLException;
import java.time.Duration;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.cockroachdb.pool.proxy.AbstractPoolingDataSourceProxy;
import io.cockroachdb.pool.proxy.PoolAdapter;
import io.cockroachdb.pool.proxy.model.PoolMetrics;

/**
 * Proxy for a target C3P0 DataSource.
 *
 * @author Kai Niemi
 */
public class C3P0DataSourceProxy extends AbstractPoolingDataSourceProxy<ComboPooledDataSource> {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    public C3P0DataSourceProxy() {
    }

    public C3P0DataSourceProxy(ComboPooledDataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    protected PoolAdapter poolAdapter() {
        return new PoolAdapter() {
            @Override
            public void applyPoolConfiguration(int minIdle, int maxSize, Duration maxLifeTime) throws SQLException {
                ComboPooledDataSource comboPooledDataSource = unwrapTargetDataSource();

                comboPooledDataSource.setMinPoolSize(minIdle);
                comboPooledDataSource.setMinPoolSize(maxSize);
                comboPooledDataSource.setMaxConnectionAge((int) maxLifeTime.toSeconds());
                comboPooledDataSource.setMaxIdleTime((int) maxLifeTime.minusSeconds(5).toSeconds());
                comboPooledDataSource.setConnectionIsValidTimeout((int) CONNECTION_TIMEOUT.toSeconds());
            }

            @Override
            public PoolMetrics collectPoolMetrics() throws SQLException {
                ComboPooledDataSource comboPooledDataSource = unwrapTargetDataSource();

                PoolMetrics poolMetrics = new PoolMetrics();
                poolMetrics.setMaxSize(comboPooledDataSource.getMaxPoolSize());
                poolMetrics.setMinIdle(comboPooledDataSource.getMinPoolSize());
                poolMetrics.setActiveCount(comboPooledDataSource.getNumConnections());
                poolMetrics.setIdleCount(comboPooledDataSource.getNumIdleConnections());
                poolMetrics.setTotalConnections(comboPooledDataSource.getNumConnectionsAllUsers());
                poolMetrics.setThreadsAwaitingConnection(comboPooledDataSource.getThreadPoolNumTasksPending());

                return poolMetrics;
            }
        };
    }

    @Override
    protected ComboPooledDataSource unwrapTargetDataSource() throws SQLException {
        return unwrap(ComboPooledDataSource.class);
    }
}
