package io.cockroachdb.poolproxy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.cockroachdb.poolproxy.model.PoolMetrics;
import io.cockroachdb.poolproxy.repository.PoolMetricsRepository;
import io.cockroachdb.poolproxy.repository.jdbc.JdbcPoolMetricsRepository;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kai Niemi
 */
public class PoolMetricsRepositoryTest extends AbstractIntegrationTest {
    @Test
    @Order(1)
    public void givenDefault_whenCreateAndFindByName_thenReturnMetrics() {
        PoolMetricsRepository repository = new JdbcPoolMetricsRepository(getDataSource());

        PoolMetrics poolMetrics = PoolMetrics.withDefaults("x");
        poolMetrics.setExpiredAt(LocalDateTime.now());

        repository.createOrUpdate(poolMetrics);

        poolMetrics = repository.findByName(poolMetrics.getPoolName()).orElseThrow();

        assertThat(poolMetrics.getPoolName()).isEqualTo("x");
    }
}
