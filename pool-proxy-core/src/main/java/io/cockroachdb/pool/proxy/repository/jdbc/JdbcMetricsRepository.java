package io.cockroachdb.pool.proxy.repository.jdbc;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;

import io.cockroachdb.pool.proxy.model.PoolMetrics;
import io.cockroachdb.pool.proxy.repository.MetricsRepository;

public class JdbcMetricsRepository extends AbstractPoolRepository implements MetricsRepository {
    public JdbcMetricsRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void createOrUpdate(PoolMetrics poolMetrics) {
        getJdbcTemplate().update(
                """
                        UPSERT INTO %smetrics
                               (name,expired_at,app_name,app_version,
                                min_idle,max_size,idle_count,active_count,
                                total_connections,threads_awaiting_connection)
                               VALUES (?,?,?,?,?,?,?,?,?,?)
                        """.formatted(getTablePrefix()),
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
                getJdbcTemplate().query("select * from %smetrics where name=?".formatted(getTablePrefix()), rowMapper(),
                        poolName));
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
        getJdbcTemplate().update(
                "DELETE from %smetrics WHERE name=?".formatted(getTablePrefix()),
                ps -> {
                    ps.setObject(1, poolName);
                });
    }

    @Override
    public Integer countAll() {
        return getJdbcTemplate().queryForObject("select count(1) from %smetrics".formatted(getTablePrefix()),
                Integer.class);
    }
}
