package io.cockroachdb.poolproxy;

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

import io.cockroachdb.poolproxy.model.ClusterInfo;
import io.cockroachdb.poolproxy.model.PoolBaseline;
import io.cockroachdb.poolproxy.model.PoolMetrics;
import io.cockroachdb.poolproxy.repository.ClusterRepository;
import io.cockroachdb.poolproxy.repository.PoolBaselineRepository;
import io.cockroachdb.poolproxy.repository.PoolMetricsRepository;
import io.cockroachdb.poolproxy.repository.jdbc.JdbcClusterRepository;
import io.cockroachdb.poolproxy.repository.jdbc.JdbcPoolBaselineRepository;
import io.cockroachdb.poolproxy.repository.jdbc.JdbcPoolMetricsRepository;

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
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClusterRepository clusterRepository;

    private PoolBaselineRepository poolBaselineRepository;

    private PoolMetricsRepository poolMetricsRepository;

    private PoolingStrategy poolingStrategy = PoolingStrategy.DYNAMIC_SIZE;

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
        Objects.requireNonNull(poolName);
        this.poolName = poolName;
    }

    public void setBaseline(String baseline) {
        Objects.requireNonNull(poolName);
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

    public void setPoolingStrategy(PoolingStrategy poolingStrategy) {
        this.poolingStrategy = poolingStrategy;
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

        Objects.requireNonNull(poolingStrategy, "configStrategy is null");

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
        }, 30, 30, TimeUnit.SECONDS);
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
        Assert.notNull(baseline, "baseline is null");
        Assert.notNull(poolName, "poolName is null");

        final ClusterInfo clusterInfo = clusterRepository.findClusterInfo();

        final PoolBaseline poolBaseline = poolBaselineRepository.findByName(baseline)
                .orElse(PoolBaseline.withDefaults());

        final int instanceCount = Math.max(1, poolMetricsRepository.countAll());

        final PoolMetrics poolMetrics = poolAdapter().collectPoolMetrics();
        {
            poolMetrics.setPoolName(poolName);
            poolMetrics.setExpiredAt(LocalDateTime.now().plusSeconds(poolBaseline.getLivenessInterval()));
            poolMetricsRepository.createOrUpdate(poolMetrics);
        }

        int minIdle = poolingStrategy.minIdle(clusterInfo, poolBaseline, instanceCount);
        int maxSize = poolingStrategy.maxSize(clusterInfo, poolBaseline, instanceCount);
        Duration maxLifeTime = poolingStrategy.maxLifetime(clusterInfo, poolBaseline, instanceCount);

        poolAdapter().applyPoolConfiguration(minIdle, maxSize, maxLifeTime);

        if (logger.isDebugEnabled()) {
            logger.debug("""
                       Update pool: %s
                      Cluster info: %s
                     Pool baseline: %s
                        Pool count: %d
                    Pool instances: %s
                          Min idle: %s
                          Max size: %s
                      Max lifetime: %s
                    """.formatted(poolName,
                    clusterInfo,
                    poolBaseline,
                    instanceCount,
                    poolMetrics,
                    minIdle,
                    maxSize,
                    maxLifeTime
            ));
        }
    }

    protected abstract T unwrapTargetDataSource() throws SQLException;

    protected abstract PoolAdapter poolAdapter() throws SQLException;
}

