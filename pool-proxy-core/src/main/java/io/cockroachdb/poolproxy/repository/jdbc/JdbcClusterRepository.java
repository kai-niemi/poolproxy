package io.cockroachdb.poolproxy.repository.jdbc;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;

import io.cockroachdb.poolproxy.model.ClusterInfo;
import io.cockroachdb.poolproxy.repository.ClusterRepository;

/**
 * @author Kai Niemi
 */
public class JdbcClusterRepository implements ClusterRepository {
    private static final String SQL_VCPU_TOTAL = """
            SELECT ceil((
                (SELECT value FROM crdb_internal.node_metrics WHERE name = 'sys.cpu.user.percent')
                + (SELECT value FROM crdb_internal.node_metrics WHERE name = 'sys.cpu.sys.percent'))
                * (SELECT value FROM crdb_internal.node_metrics WHERE name = 'liveness.livenodes')
                / (SELECT value FROM crdb_internal.node_metrics WHERE name = 'sys.cpu.combined.percent-normalized')
            ) AS vcpus
            """;

    private static final String SQL_CLUSTER_SUMMARY = """
            SELECT crdb_internal.cluster_id() as cluster_id,
                   count(node_id) as nodes,
                   min(build_tag) as min_ver,
                   max(build_tag) as max_ver
            FROM crdb_internal.gossip_nodes
            WHERE is_live = true
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcClusterRepository(DataSource dataSource) {
        Assert.notNull(dataSource, "dataSource is null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void initSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setCommentPrefix("--");
        populator.setIgnoreFailedDrops(true);
        populator.addScript(new ClassPathResource("db/create.sql"));

        DatabasePopulatorUtils.execute(populator, jdbcTemplate.getDataSource());
    }

    @Override
    public void dropSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setCommentPrefix("--");
        populator.setIgnoreFailedDrops(true);
        populator.addScript(new ClassPathResource("db/drop.sql"));

        DatabasePopulatorUtils.execute(populator, jdbcTemplate.getDataSource());
    }

    @Override
    public ClusterInfo findClusterInfo() {
        Integer vCPUs = jdbcTemplate.queryForObject(SQL_VCPU_TOTAL, Integer.class);
        return jdbcTemplate.queryForObject(SQL_CLUSTER_SUMMARY,
                (rs, rowNum) -> {
                    ClusterInfo clusterInfo = new ClusterInfo();
                    clusterInfo.setNumVCPUs(Math.max(1, vCPUs));
                    clusterInfo.setClusterId(rs.getString("cluster_id"));
                    clusterInfo.setNumNodes(Math.max(1, rs.getInt("nodes")));
                    clusterInfo.setMinVersion(rs.getString("min_ver"));
                    clusterInfo.setMaxVersion(rs.getString("max_ver"));
                    return clusterInfo;
                });
    }
}
