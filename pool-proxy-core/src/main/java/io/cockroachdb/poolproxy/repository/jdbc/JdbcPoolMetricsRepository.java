package io.cockroachdb.poolproxy.repository.jdbc;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import io.cockroachdb.poolproxy.model.PoolMetrics;
import io.cockroachdb.poolproxy.repository.PoolMetricsRepository;

public class JdbcPoolMetricsRepository implements PoolMetricsRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPoolMetricsRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void createOrUpdate(PoolMetrics poolMetrics) {
        jdbcTemplate.update(
                """
                        UPSERT INTO pool_metrics
                               (name,expired_at,app_name,app_version,
                                min_idle,max_size,idle_count,active_count,
                                total_connections,threads_awaiting_connection)
                               VALUES (?,?,?,?,?,?,?,?,?,?)
                        """,
                ps -> {
                    ps.setObject(1, poolMetrics.getPoolName());
                    ps.setObject(2, poolMetrics.getExpiredAt());
                    ps.setObject(3, poolMetrics.getAppName());
                    ps.setObject(4, poolMetrics.getAppVersion());
                    ps.setObject(5, poolMetrics.getMinIdle());
                    ps.setObject(6, poolMetrics.getMaxSize());
                    ps.setObject(7, poolMetrics.getIdleCount());
                    ps.setObject(8, poolMetrics.getActiveCount());
                    ps.setObject(9, poolMetrics.getTotalConnections());
                    ps.setObject(10, poolMetrics.getThreadsAwaitingConnection());
                });
    }

    @Override
    public Optional<PoolMetrics> findByName(String poolName) {
        return DataAccessUtils.optionalResult(
                jdbcTemplate.query("select * from pool_metrics where name=?", rowMapper(), poolName));
    }

    @Override
    public List<PoolMetrics> findAll(int limit) {
        return jdbcTemplate.query("select * from pool_metrics order by name limit ?", rowMapper(), limit);
    }

    private static RowMapper<PoolMetrics> rowMapper() {
        return (rs, rowNum) -> {
            PoolMetrics poolMetrics = new PoolMetrics();
            poolMetrics.setPoolName(rs.getString("name"));
            poolMetrics.setExpiredAt(rs.getTimestamp("expired_at").toLocalDateTime());
            poolMetrics.setAppName(rs.getString("app_name"));
            poolMetrics.setAppVersion(rs.getString("app_version"));
            poolMetrics.setMinIdle(rs.getInt("min_idle"));
            poolMetrics.setMaxSize(rs.getInt("max_size"));
            poolMetrics.setIdleCount(rs.getInt("idle_count"));
            poolMetrics.setActiveCount(rs.getInt("active_count"));
            poolMetrics.setTotalConnections(rs.getInt("total_connections"));
            poolMetrics.setThreadsAwaitingConnection(rs.getInt("threads_awaiting_connection"));
            return poolMetrics;
        };
    }

    @Override
    public void deleteByName(String poolName) {
        jdbcTemplate.update(
                "DELETE from pool_metrics WHERE name=?",
                ps -> {
                    ps.setObject(1, poolName);
                });
    }

    @Override
    public Integer countAll() {
        return jdbcTemplate.queryForObject("select count(1) from pool_metrics", Integer.class);
    }
}
