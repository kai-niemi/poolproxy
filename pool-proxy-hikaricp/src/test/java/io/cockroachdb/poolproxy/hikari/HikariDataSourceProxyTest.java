package io.cockroachdb.poolproxy.hikari;

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

import io.cockroachdb.poolproxy.PoolingStrategy;
import io.cockroachdb.poolproxy.model.ClusterInfo;
import io.cockroachdb.poolproxy.model.PoolBaseline;
import io.cockroachdb.poolproxy.model.PoolMetrics;
import io.cockroachdb.poolproxy.repository.ClusterRepository;
import io.cockroachdb.poolproxy.repository.PoolBaselineRepository;
import io.cockroachdb.poolproxy.repository.PoolMetricsRepository;
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

    private static PoolMetricsRepository poolMetricsRepositoryMock;

    private static PoolBaselineRepository poolBaselineRepository;

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

        poolMetricsRepositoryMock = mock(PoolMetricsRepository.class);
        doNothing().when(poolMetricsRepositoryMock).createOrUpdate(any());
        doNothing().when(poolMetricsRepositoryMock).deleteByName(any());
        when(poolMetricsRepositoryMock.findByName(any())).thenReturn(
                Optional.of(PoolMetrics.withDefaults("test")));
        when(poolMetricsRepositoryMock.countAll()).thenReturn(1);

        PoolBaseline dynamicBaseline = PoolBaseline.withDefaults();
        dynamicBaseline.setPoolingStrategy(PoolingStrategy.DYNAMIC_SIZE);

        PoolBaseline fixedBaseline = PoolBaseline.withDefaults();
        fixedBaseline.setPoolingStrategy(PoolingStrategy.FIXED_SIZE);

        poolBaselineRepository = mock(PoolBaselineRepository.class);
        when(poolBaselineRepository.findByName(Mockito.eq("dynamic"))).thenReturn(Optional.of(dynamicBaseline));
        when(poolBaselineRepository.findByName(Mockito.eq("fixed"))).thenReturn(Optional.of(fixedBaseline));
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
        dataSourceProxy.setPoolMetricsRepository(poolMetricsRepositoryMock);
        dataSourceProxy.setPoolBaselineRepository(poolBaselineRepository);
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
        dataSourceProxy.setPoolMetricsRepository(poolMetricsRepositoryMock);
        dataSourceProxy.setPoolBaselineRepository(poolBaselineRepository);
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
