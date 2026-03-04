package io.cockroachdb.pool.proxy.repository.jdbc;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import io.cockroachdb.pool.proxy.PoolingStrategy;
import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.repository.PoolBaselineRepository;

/**
 * @author Kai Niemi
 */
public class JdbcPoolBaselineRepository implements PoolBaselineRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPoolBaselineRepository(DataSource dataSource) {
        Assert.notNull(dataSource, "dataSource is null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<PoolBaseline> findByName(String name) {
        PoolBaseline baseline = jdbcTemplate.queryForObject("select * from pool_baseline where name=?",
                (rs, rowNum) -> {
                    PoolBaseline entity = new PoolBaseline();
                    entity.setName(rs.getString("name"));
                    entity.setLivenessInterval(rs.getInt("liveness_interval"));
                    entity.setConnectionTimeout(rs.getInt("connection_timeout"));
                    entity.setMinIdle(rs.getInt("min_idle"));
                    entity.setMaxSize(rs.getInt("max_size"));
                    entity.setPoolingStrategy(PoolingStrategy.valueOf(rs.getString("strategy")));
                    return entity;
                }, name);
        return Optional.ofNullable(baseline);
    }


    @Override
    public void update(PoolBaseline baseline) {
        final String sql = """
                update pool_baseline set liveness_interval=?,
                                         connection_timeout=?,
                                         min_idle=?,
                                         max_size=?,
                                         strategy=?
                 where name=?
                """;

        int rows = jdbcTemplate.update(sql, ps -> {
            ps.setInt(1, baseline.getLivenessInterval());
            ps.setInt(2, baseline.getConnectionTimeout());
            ps.setInt(3, baseline.getMinIdle());
            ps.setInt(4, baseline.getMaxSize());
            ps.setString(5, baseline.getPoolingStrategy().name());
            ps.setString(6, baseline.getName());
        });
        if (rows != 1) {
            throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, 1, rows);
        }
    }
}
