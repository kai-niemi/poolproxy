package io.cockroachdb.pool.proxy.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.pool.proxy.model.PoolStrategy;
import io.cockroachdb.pool.proxy.model.ClusterInfo;
import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.model.PoolMetrics;
import io.cockroachdb.pool.proxy.repository.ClusterRepository;
import io.cockroachdb.pool.proxy.repository.BaselineRepository;
import io.cockroachdb.pool.proxy.repository.MetricsRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kai Niemi
 */
public class HikariDataSourceProxyTest extends AbstractIntegrationTest {
    private static ClusterRepository clusterRepositoryMock;

    private static MetricsRepository metricsRepositoryMock;

    private static BaselineRepository baselineRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    public static void setupMocks() {
        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setClusterId(UUID.randomUUID().toString());
        clusterInfo.setNumVCPUs(3 * 8);
        clusterInfo.setNumNodes(3);

        clusterRepositoryMock = mock(ClusterRepository.class);
        when(clusterRepositoryMock.findClusterInfo()).thenReturn(clusterInfo);

        metricsRepositoryMock = mock(MetricsRepository.class);
        doNothing().when(metricsRepositoryMock).createOrUpdate(any());
        doNothing().when(metricsRepositoryMock).deleteByName(any());
        when(metricsRepositoryMock.findByName(any())).thenReturn(
                Optional.of(PoolMetrics.withDefaults("test")));
        when(metricsRepositoryMock.countAll()).thenReturn(1);

        PoolBaseline dynamicBaseline = PoolBaseline.withDefaults();
        dynamicBaseline.setPoolingStrategy(PoolStrategy.DYNAMIC_SIZE);

        PoolBaseline fixedBaseline = PoolBaseline.withDefaults();
        fixedBaseline.setPoolingStrategy(PoolStrategy.FIXED_SIZE);

        baselineRepository = mock(BaselineRepository.class);
        when(baselineRepository.findByName(Mockito.eq("dynamic"))).thenReturn(Optional.of(dynamicBaseline));
        when(baselineRepository.findByName(Mockito.eq("fixed"))).thenReturn(Optional.of(fixedBaseline));
    }

    @Test
    public void openConnectionWhenDynamicSizeStrategyThenSucceed() {
        try (Connection c = dataSource.getConnection()) {
            assertThat(c.isClosed()).isFalse();
        } catch (SQLException e) {
            Assertions.fail(e);
        }

        HikariDataSourceProxy dataSourceProxy = new HikariDataSourceProxy();
        dataSourceProxy.setTargetDataSource(dataSource);
        dataSourceProxy.setClusterRepository(clusterRepositoryMock);
        dataSourceProxy.setPoolMetricsRepository(metricsRepositoryMock);
        dataSourceProxy.setPoolBaselineRepository(baselineRepository);
        dataSourceProxy.setBaseline("dynamic");
        dataSourceProxy.afterPropertiesSet();

        try (Connection connection = dataSourceProxy.getConnection()) {
            assertThat(connection.isClosed()).isFalse();

            HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
            assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(96);
            assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(24);
            assertThat(hikariDataSource.getIdleTimeout()).isEqualTo(HikariDataSourceProxy.IDLE_TIMEOUT.toMillis());
        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void openConnectionWhenFixedSizeStrategyThenSucceed() {
        try (Connection c = dataSource.getConnection()) {
            assertThat(c.isClosed()).isFalse();
        } catch (SQLException e) {
            Assertions.fail(e);
        }

        HikariDataSourceProxy dataSourceProxy = new HikariDataSourceProxy();
        dataSourceProxy.setTargetDataSource(dataSource);
        dataSourceProxy.setClusterRepository(clusterRepositoryMock);
        dataSourceProxy.setPoolMetricsRepository(metricsRepositoryMock);
        dataSourceProxy.setPoolBaselineRepository(baselineRepository);
        dataSourceProxy.setBaseline("fixed");
        dataSourceProxy.afterPropertiesSet();

        try (Connection connection = dataSourceProxy.getConnection()) {
            assertThat(connection.isClosed()).isFalse();

            HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
            assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(96);
            assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(96);
            assertThat(hikariDataSource.getIdleTimeout()).isEqualTo(HikariDataSourceProxy.IDLE_TIMEOUT.toMillis());
        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }
}
