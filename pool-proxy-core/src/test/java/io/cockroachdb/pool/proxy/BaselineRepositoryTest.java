package io.cockroachdb.pool.proxy;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.model.PoolStrategy;
import io.cockroachdb.pool.proxy.repository.BaselineRepository;
import io.cockroachdb.pool.proxy.repository.jdbc.JdbcBaselineRepository;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kai Niemi
 */
public class BaselineRepositoryTest extends AbstractIntegrationTest {
    @Test
    @Order(2)
    public void givenDefault_whenFindByName_thenReturnDefaultValues() {
        BaselineRepository repository = new JdbcBaselineRepository(getDataSource());

        PoolBaseline baseline = repository.findByName("default").orElseThrow();
        assertThat(baseline.getConnectionTimeout()).isGreaterThan(0);
    }

    @Test
    @Order(3)
    public void giveDefault_whenUpdate_thenCommit() {
        BaselineRepository repository = new JdbcBaselineRepository(getDataSource());

        PoolBaseline baseline = repository.findByName("default").orElseThrow();
        baseline.setMinIdle(10);
        baseline.setMaxSize(11);
        baseline.setConnectionTimeout(15);
        baseline.setLivenessInterval(20);
        baseline.setPoolingStrategy(PoolStrategy.FIXED_SIZE);

        repository.update(baseline);
    }

    @Test
    @Order(4)
    public void givenModified_whenFindByName_thenReturnChangedValues() {
        BaselineRepository repository = new JdbcBaselineRepository(getDataSource());

        PoolBaseline baseline = repository.findByName("default").orElseThrow();
        assertThat(baseline.getMinIdle()).isEqualTo(10);
        assertThat(baseline.getMaxSize()).isEqualTo(11);
        assertThat(baseline.getConnectionTimeout()).isEqualTo(15);
        assertThat(baseline.getLivenessInterval()).isEqualTo(20);
        assertThat(baseline.getPoolingStrategy()).isEqualTo(PoolStrategy.FIXED_SIZE);
    }

}
