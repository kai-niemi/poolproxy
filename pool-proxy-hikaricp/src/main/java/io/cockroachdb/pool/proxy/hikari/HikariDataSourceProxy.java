package io.cockroachdb.pool.proxy.hikari;

import java.sql.SQLException;
import java.time.Duration;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import io.cockroachdb.pool.proxy.AbstractPoolingDataSourceProxy;
import io.cockroachdb.pool.proxy.PoolAdapter;
import io.cockroachdb.pool.proxy.model.PoolMetrics;

/**
 * Proxy for a target HikariCP DataSource.
 *
 * @author Kai Niemi
 */
public class HikariDataSourceProxy extends AbstractPoolingDataSourceProxy<HikariDataSource> {
    public static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    public static final Duration VALIDATION_TIMEOUT = Duration.ofSeconds(5);

    public static final Duration IDLE_TIMEOUT = Duration.ofSeconds(10);

    public HikariDataSourceProxy() {
    }

    public HikariDataSourceProxy(HikariDataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    protected PoolAdapter poolAdapter() {
        return new PoolAdapter() {
            @Override
            public void applyPoolConfiguration(int minIdle, int maxSize, Duration maxLifeTime) throws SQLException {
                HikariDataSource hikariDataSource = unwrapTargetDataSource();
                HikariConfigMXBean mxBean = hikariDataSource.getHikariConfigMXBean();

                final Duration idleTimeout;
                if (mxBean.getMinimumIdle() < mxBean.getMaximumPoolSize() && mxBean.getMinimumIdle() > 0) {
                    idleTimeout = maxLifeTime.minusSeconds(5);
                } else {
                    idleTimeout = IDLE_TIMEOUT;
                }

                hikariDataSource.setKeepaliveTime(maxLifeTime.minusSeconds(5).toMillis());

                mxBean.setMinimumIdle(minIdle);
                mxBean.setMaximumPoolSize(maxSize);
                mxBean.setMaxLifetime(maxLifeTime.toMillis());
                mxBean.setIdleTimeout(idleTimeout.toMillis());
                mxBean.setConnectionTimeout(CONNECTION_TIMEOUT.toMillis());
                mxBean.setValidationTimeout(VALIDATION_TIMEOUT.toMillis());
            }

            @Override
            public PoolMetrics collectPoolMetrics() throws SQLException {
                HikariDataSource hikariDataSource = unwrapTargetDataSource();
                HikariPoolMXBean mxBean = hikariDataSource.getHikariPoolMXBean();

                PoolMetrics poolMetrics = new PoolMetrics();
                poolMetrics.setMinIdle(hikariDataSource.getMinimumIdle());
                poolMetrics.setMaxSize(hikariDataSource.getMaximumPoolSize());
                poolMetrics.setActiveCount(mxBean.getActiveConnections());
                poolMetrics.setIdleCount(mxBean.getIdleConnections());
                poolMetrics.setTotalConnections(mxBean.getTotalConnections());
                poolMetrics.setThreadsAwaitingConnection(mxBean.getThreadsAwaitingConnection());

                return poolMetrics;
            }
        };
    }

    @Override
    protected HikariDataSource unwrapTargetDataSource() throws SQLException {
        return unwrap(HikariDataSource.class);
    }
}
