package io.cockroachdb.poolproxy;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.cockroachdb.poolproxy.model.ClusterInfo;
import io.cockroachdb.poolproxy.repository.ClusterRepository;
import io.cockroachdb.poolproxy.repository.jdbc.JdbcClusterRepository;
import static org.assertj.core.api.Assertions.assertThat;

public class ClusterRepositoryTest extends AbstractIntegrationTest {
    @Test
    @Order(1)
    public void givenDefault_whenFindClusterInfo_thenReturnDefaultValues() {
        ClusterRepository clusterRepository = new JdbcClusterRepository(getDataSource());
        ClusterInfo clusterInfo = clusterRepository.findClusterInfo();

        assertThat(clusterInfo.getNumVCPUs()).isGreaterThan(0);
        assertThat(clusterInfo.getNumNodes()).isGreaterThan(0);
        assertThat(clusterInfo.getNumVCPUsPerNode()).isGreaterThan(0);
        assertThat(clusterInfo.getClusterId()).isNotNull();
        assertThat(clusterInfo.getClusterId()).isNotBlank();
    }
}

