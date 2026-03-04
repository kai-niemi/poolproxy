package io.cockroachdb.pool.proxy.c3p0;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.cockroachdb.pool.proxy.model.ClusterInfo;
import io.cockroachdb.pool.proxy.model.PoolMetrics;
import io.cockroachdb.pool.proxy.repository.ClusterRepository;
import io.cockroachdb.pool.proxy.repository.PoolMetricsRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kai Niemi
 */
public class C3P0DataSourceProxyTest extends AbstractIntegrationTest {
    private static ClusterRepository clusterRepositoryMock;

    private static PoolMetricsRepository poolMetricsRepositoryMock;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    public static void setupMocks() {
        clusterRepositoryMock = mock(ClusterRepository.class);
        when(clusterRepositoryMock.findClusterInfo()).thenReturn(
                ClusterInfo.withDefaults());
//        when(clusterRepositoryMock.findByName(anyString())).thenReturn(
//                Optional.of(PoolBaseline.withDefaults()));

        poolMetricsRepositoryMock = mock(PoolMetricsRepository.class);
        doNothing().when(poolMetricsRepositoryMock).createOrUpdate(any());
        doNothing().when(poolMetricsRepositoryMock).deleteByName(any());
        when(poolMetricsRepositoryMock.findByName(any())).thenReturn(
                Optional.of(PoolMetrics.withDefaults("test")));
        when(poolMetricsRepositoryMock.countAll()).thenReturn(1);
    }

    @Test
    public void openConnectionWhenDynamicSizeStrategyThenSucceed() {
        try (Connection c = dataSource.getConnection()) {
            assertThat(c.isClosed()).isFalse();
        } catch (SQLException e) {
            Assertions.fail(e);
        }

        C3P0DataSourceProxy dataSourceProxy = new C3P0DataSourceProxy();
        dataSourceProxy.setTargetDataSource(dataSource);
        dataSourceProxy.setClusterRepository(clusterRepositoryMock);
        dataSourceProxy.setPoolMetricsRepository(poolMetricsRepositoryMock);
        dataSourceProxy.afterPropertiesSet();

        try (Connection connection = dataSourceProxy.getConnection()) {
            assertThat(connection.isClosed()).isFalse();
//            assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(96);
//            assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(24);
//            assertThat(hikariDataSource.getIdleTimeout()).isEqualTo(Duration.ofSeconds(10).toMillis());
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

        C3P0DataSourceProxy dataSourceProxy = new C3P0DataSourceProxy();
        dataSourceProxy.setTargetDataSource(dataSource);
        dataSourceProxy.setClusterRepository(clusterRepositoryMock);
        dataSourceProxy.setPoolMetricsRepository(poolMetricsRepositoryMock);
        dataSourceProxy.afterPropertiesSet();

        try (Connection connection = dataSourceProxy.getConnection()) {
            assertThat(connection.isClosed()).isFalse();
//            assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(96);
//            assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(96);
//            assertThat(hikariDataSource.getIdleTimeout()).isEqualTo(Duration.ofSeconds(10).toMillis());
        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }
}
