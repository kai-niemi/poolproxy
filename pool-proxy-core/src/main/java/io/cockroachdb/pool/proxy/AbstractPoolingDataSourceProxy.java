package io.cockroachdb.pool.proxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.Assert;

import io.cockroachdb.pool.proxy.model.ClusterInfo;
import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.model.PoolMetrics;
import io.cockroachdb.pool.proxy.repository.ClusterRepository;
import io.cockroachdb.pool.proxy.repository.PoolBaselineRepository;
import io.cockroachdb.pool.proxy.repository.PoolMetricsRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcClusterRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcPoolBaselineRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcPoolMetricsRepository;

/**
 * An abstract pooling datasource proxy, adding elastic pool size and timeout
 * configuration based on CockroachDB production guidelines and best practices.
 * <p>
 * Effectively, it adjusts the pool configuration based on target database
 * provisioning and number of pool instances. The aim is to have zero-config
 * at application level and instead rely on a baseline configuration that
 * can also be adjusted and coordinated across all proxied data-sources
 * through the database itself.
 *
 * @author Kai Niemi
 */
public abstract class AbstractPoolingDataSourceProxy<T extends DataSource> extends DelegatingDataSource
        implements AutoCloseable {
    private static final int UPDATE_INTERVAL = 30;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClusterRepository clusterRepository;

    private PoolBaselineRepository poolBaselineRepository;

    private PoolMetricsRepository poolMetricsRepository;

    private ScheduledExecutorService scheduledExecutorService;

    private String poolName;

    private String baseline;

    private boolean initialized;

    public AbstractPoolingDataSourceProxy() {
    }

    public AbstractPoolingDataSourceProxy(DataSource targetDataSource) {
        super(targetDataSource);
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public void setBaseline(String baseline) {
        this.baseline = baseline;
    }

    public void setClusterRepository(ClusterRepository clusterRepository) {
        this.clusterRepository = clusterRepository;
    }

    public void setPoolBaselineRepository(PoolBaselineRepository poolBaselineRepository) {
        this.poolBaselineRepository = poolBaselineRepository;
    }

    public void setPoolMetricsRepository(PoolMetricsRepository poolMetricsRepository) {
        this.poolMetricsRepository = poolMetricsRepository;
    }

    @Override
    public void close() {
        try {
            logger.trace("Closing (on close) datasource: {}", getTargetDataSource());
            poolMetricsRepository.deleteByName(poolName);

            scheduledExecutorService.shutdownNow();
        } catch (DataAccessException ex) {
            logger.warn("Unable to delete pool instance", ex);
        }
    }

    @Override
    public final void afterPropertiesSet() {
        super.afterPropertiesSet();

        if (Objects.isNull(poolName)) {
            poolName = UUID.randomUUID().toString();
        }

        if (Objects.isNull(baseline)) {
            baseline = "default";
        }

        if (Objects.isNull(clusterRepository)) {
            clusterRepository = new JdbcClusterRepository(obtainTargetDataSource());
        }
        clusterRepository.initSchema();

        if (Objects.isNull(poolBaselineRepository)) {
            poolBaselineRepository = new JdbcPoolBaselineRepository(obtainTargetDataSource());
        }

        if (Objects.isNull(poolMetricsRepository)) {
            poolMetricsRepository = new JdbcPoolMetricsRepository(obtainTargetDataSource());
        }

        // Schedule periodic liveness updates

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                applyDataSourceProperties();
            } catch (SQLException e) {
                logger.warn("Failed to update pool instance liveness record", e);
            }
        }, 5, UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!initialized) {
            applyDataSourceProperties();
            initialized = true;
        }
        return super.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (!initialized) {
            applyDataSourceProperties();
            initialized = true;
        }
        return super.getConnection(username, password);
    }

    private void applyDataSourceProperties() throws SQLException {
        Assert.notNull(baseline, "baseline not set");
        Assert.notNull(poolName, "poolName not set");

        final ClusterInfo clusterInfo = clusterRepository.findClusterInfo();

        final PoolBaseline poolBaseline = poolBaselineRepository.findByName(baseline)
                .orElse(PoolBaseline.withDefaults());

        PoolingStrategy poolingStrategy = poolBaseline.getPoolingStrategy();
        Objects.requireNonNull(poolingStrategy, "poolingStrategy not set");

        final int poolCountBefore = Math.max(1, poolMetricsRepository.countAll());

        final PoolMetrics poolMetricsBefore = poolAdapter().collectPoolMetrics();
        poolMetricsBefore.setPoolName(poolName);
        poolMetricsBefore.setExpiredAt(LocalDateTime.now().plusSeconds(poolBaseline.getLivenessInterval()));

        poolMetricsRepository.createOrUpdate(poolMetricsBefore);

        final int poolCountAfter = Math.max(1, poolMetricsRepository.countAll());

        int minIdle = poolingStrategy.minIdle(clusterInfo, poolBaseline, poolCountAfter);
        int maxSize = poolingStrategy.maxSize(clusterInfo, poolBaseline, poolCountAfter);
        Duration maxLifeTime = poolingStrategy.maxLifetime(clusterInfo, poolBaseline, poolCountAfter);

        poolAdapter().applyPoolConfiguration(minIdle, maxSize, maxLifeTime);

        if (logger.isDebugEnabled()) {
            final PoolMetrics poolMetricsAfter = poolAdapter().collectPoolMetrics();
            poolMetricsAfter.setPoolName(poolMetricsBefore.getPoolName());
            poolMetricsAfter.setExpiredAt(poolMetricsBefore.getExpiredAt());

            logger.debug(("""
                    Updating connection pool:
                                   Pool name: %s
                                Cluster info: %s
                               Pool baseline: %s
                                Pool metrics: %s
                        Pool metrics (after): %s
                                  Pool count: %d (%d)
                      Minimum idle pool size: %s
                           Maximum pool size: %s
                     Max connection lifetime: %s""")
                    .formatted(poolName,
                            clusterInfo,
                            poolBaseline,
                            poolMetricsBefore,
                            poolMetricsAfter,
                            poolCountBefore,
                            poolCountAfter,
                            minIdle,
                            maxSize,
                            maxLifeTime
                    ));
        }
    }

    protected abstract T unwrapTargetDataSource() throws SQLException;

    protected abstract PoolAdapter poolAdapter() throws SQLException;
}

