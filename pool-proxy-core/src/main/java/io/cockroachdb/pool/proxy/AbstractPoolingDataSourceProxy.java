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
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.Assert;

import io.cockroachdb.pool.proxy.model.ClusterInfo;
import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.model.PoolMetrics;
import io.cockroachdb.pool.proxy.model.PoolStrategy;
import io.cockroachdb.pool.proxy.repository.BaselineRepository;
import io.cockroachdb.pool.proxy.repository.ClusterRepository;
import io.cockroachdb.pool.proxy.repository.MetricsRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcBaselineRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcClusterRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcMetricsRepository;

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
    private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClusterRepository clusterRepository;

    private BaselineRepository baselineRepository;

    private MetricsRepository metricsRepository;

    private ScheduledExecutorService scheduledExecutorService;

    private String poolName;

    private String baseline;

    private long updateIntervalMillis = UPDATE_INTERVAL_MILLIS;

    public AbstractPoolingDataSourceProxy() {
    }

    public AbstractPoolingDataSourceProxy(DataSource targetDataSource) {
        super(targetDataSource);
    }

    public void setUpdateIntervalMillis(long updateIntervalMillis) {
        Assert.isNull(scheduledExecutorService, "Liveness updater already started");
        this.updateIntervalMillis = updateIntervalMillis;
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

    public void setPoolBaselineRepository(BaselineRepository baselineRepository) {
        this.baselineRepository = baselineRepository;
    }

    public void setPoolMetricsRepository(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return super.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return super.getConnection(username, password);
    }

    @Override
    public void close() {
        try {
            logger.debug("Closing (on close) datasource: {}", getTargetDataSource());
            metricsRepository.deleteByName(poolName);
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

        if (Objects.isNull(baselineRepository)) {
            baselineRepository = new JdbcBaselineRepository(obtainTargetDataSource());
        }

        if (Objects.isNull(metricsRepository)) {
            metricsRepository = new JdbcMetricsRepository(obtainTargetDataSource());
        }

        Assert.notNull(baseline, "baseline not set");
        Assert.notNull(poolName, "poolName not set");

        startLivenessUpdater();
    }

    private void startLivenessUpdater() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                PoolAdapter poolAdapter = poolAdapter();
                applyDataSourceProperties(poolAdapter);
            } catch (SQLException e) {
                logger.error("SQL exception updating pool instance liveness record", e);
            } catch (NonTransientDataAccessException e) {
                logger.error("Non-transient exception updating pool instance liveness record", e);
            } catch (TransientDataAccessException e) {
                logger.warn("Transient SQL exception updating pool instance liveness record", e);
            }
        }, TimeUnit.SECONDS.toMillis(5), updateIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private void applyDataSourceProperties(PoolAdapter poolAdapter) throws SQLException {
        int poolCountBefore = Math.max(1, metricsRepository.countAll());

        PoolBaseline poolBaseline = baselineRepository.findByName(baseline)
                .orElse(PoolBaseline.withDefaults());
        PoolStrategy poolStrategy = poolBaseline.getPoolingStrategy();

        PoolMetrics poolMetricsBefore = poolMetrics(poolAdapter, poolBaseline);
        metricsRepository.createOrUpdate(poolMetricsBefore);

        int poolCountAfter = Math.max(1, metricsRepository.countAll());

        ClusterInfo clusterInfo = clusterRepository.findClusterInfo();
        int minIdle = poolStrategy.minIdle(clusterInfo, poolBaseline, poolCountAfter);
        int maxSize = poolStrategy.maxSize(clusterInfo, poolBaseline, poolCountAfter);
        Duration maxLifeTime = poolStrategy.maxLifetime(clusterInfo, poolBaseline, poolCountAfter);

        poolAdapter.applyPoolConfiguration(minIdle, maxSize, maxLifeTime);

        if (logger.isDebugEnabled()) {
            PoolMetrics poolMetricsAfter = poolMetrics(poolAdapter, poolBaseline);
            logger.debug(("""
                    Updated connection pool:
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

    private PoolMetrics poolMetrics(PoolAdapter poolAdapter,
                                    PoolBaseline poolBaseline) throws SQLException {
        final PoolMetrics poolMetrics = poolAdapter.collectPoolMetrics();
        poolMetrics.setPoolName(poolName);
        poolMetrics.setExpiredAt(LocalDateTime.now()
                .plusSeconds(poolBaseline.getLivenessInterval()));
        return poolMetrics;
    }

    protected abstract T unwrapTargetDataSource() throws SQLException;

    protected abstract PoolAdapter poolAdapter() throws SQLException;
}

