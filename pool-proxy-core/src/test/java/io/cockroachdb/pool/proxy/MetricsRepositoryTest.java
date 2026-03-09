package io.cockroachdb.pool.proxy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.cockroachdb.pool.proxy.model.PoolMetrics;
import io.cockroachdb.pool.proxy.repository.MetricsRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcMetricsRepository;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kai Niemi
 */
public class MetricsRepositoryTest extends AbstractIntegrationTest {
    @Test
    @Order(1)
    public void givenDefault_whenCreateAndFindByName_thenReturnMetrics() {
        MetricsRepository repository = new JdbcMetricsRepository(getDataSource());

        PoolMetrics poolMetrics = PoolMetrics.withDefaults("x");
        poolMetrics.setExpiredAt(LocalDateTime.now());

        repository.createOrUpdate(poolMetrics);

        poolMetrics = repository.findByName(poolMetrics.getPoolName()).orElseThrow();

        assertThat(poolMetrics.getPoolName()).isEqualTo("x");
    }
}
