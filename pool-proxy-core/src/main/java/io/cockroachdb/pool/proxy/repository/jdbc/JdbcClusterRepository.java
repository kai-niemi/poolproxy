package io.cockroachdb.pool.proxy.repository.jdbc;

import java.util.Objects;

import javax.sql.DataSource;

import io.cockroachdb.pool.proxy.model.ClusterInfo;
import io.cockroachdb.pool.proxy.repository.ClusterRepository;

/**
 * @author Kai Niemi
 */
public class JdbcClusterRepository extends AbstractPoolRepository implements ClusterRepository {
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

    public JdbcClusterRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ClusterInfo findClusterInfo() {
        Integer vCPUs = getJdbcTemplate().queryForObject(SQL_VCPU_TOTAL, Integer.class);

        return getJdbcTemplate().queryForObject(SQL_CLUSTER_SUMMARY,
                (rs, rowNum) -> {
                    ClusterInfo clusterInfo = new ClusterInfo();
                    clusterInfo.setNumVCPUs(Math.max(1, Objects.requireNonNull(vCPUs)));
                    clusterInfo.setClusterId(rs.getString("cluster_id"));
                    clusterInfo.setNumNodes(Math.max(1, rs.getInt("nodes")));
                    clusterInfo.setMinVersion(rs.getString("min_ver"));
                    clusterInfo.setMaxVersion(rs.getString("max_ver"));
                    return clusterInfo;
                });
    }
}
