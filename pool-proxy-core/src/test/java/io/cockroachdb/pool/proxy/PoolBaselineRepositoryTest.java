package io.cockroachdb.pool.proxy;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.repository.PoolBaselineRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcPoolBaselineRepository;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kai Niemi
 */
public class PoolBaselineRepositoryTest extends AbstractIntegrationTest {
    @Test
    @Order(2)
    public void givenDefault_whenFindByName_thenReturnDefaultValues() {
        PoolBaselineRepository repository = new JdbcPoolBaselineRepository(getDataSource());

        PoolBaseline baseline = repository.findByName("default").orElseThrow();
        assertThat(baseline.getConnectionTimeout()).isGreaterThan(0);
    }

    @Test
    @Order(3)
    public void giveDefault_whenUpdate_thenCommit() {
        PoolBaselineRepository repository = new JdbcPoolBaselineRepository(getDataSource());

        PoolBaseline baseline = repository.findByName("default").orElseThrow();
        baseline.setMinIdle(10);
        baseline.setMaxSize(11);
        baseline.setConnectionTimeout(15);
        baseline.setLivenessInterval(20);
        baseline.setPoolingStrategy(PoolingStrategy.FIXED_SIZE);

        repository.update(baseline);
    }

    @Test
    @Order(4)
    public void givenModified_whenFindByName_thenReturnChangedValues() {
        PoolBaselineRepository repository = new JdbcPoolBaselineRepository(getDataSource());

        PoolBaseline baseline = repository.findByName("default").orElseThrow();
        assertThat(baseline.getMinIdle()).isEqualTo(10);
        assertThat(baseline.getMaxSize()).isEqualTo(11);
        assertThat(baseline.getConnectionTimeout()).isEqualTo(15);
        assertThat(baseline.getLivenessInterval()).isEqualTo(20);
        assertThat(baseline.getPoolingStrategy()).isEqualTo(PoolingStrategy.FIXED_SIZE);
    }

}
