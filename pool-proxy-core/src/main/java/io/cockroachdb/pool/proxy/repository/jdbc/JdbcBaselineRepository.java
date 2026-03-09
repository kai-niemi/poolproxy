package io.cockroachdb.pool.proxy.repository.jdbc;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;

import io.cockroachdb.pool.proxy.model.PoolBaseline;
import io.cockroachdb.pool.proxy.model.PoolStrategy;
import io.cockroachdb.pool.proxy.repository.BaselineRepository;

/**
 * @author Kai Niemi
 */
public class JdbcBaselineRepository extends AbstractPoolRepository implements BaselineRepository {
    public JdbcBaselineRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Optional<PoolBaseline> findByName(String name) {
        PoolBaseline baseline = getJdbcTemplate().queryForObject(
                "select * from %sbaseline where name=?".formatted(getTablePrefix()),
                (rs, rowNum) -> {
                    PoolBaseline entity = new PoolBaseline();
                    entity.setName(rs.getString("name"));
                    entity.setLivenessInterval(rs.getInt("liveness_interval"));
                    entity.setConnectionTimeout(rs.getInt("connection_timeout"));
                    entity.setMinIdle(rs.getInt("min_idle"));
                    entity.setMaxSize(rs.getInt("max_size"));
                    entity.setPoolingStrategy(PoolStrategy.valueOf(rs.getString("strategy")));
                    return entity;
                }, name);
        return Optional.ofNullable(baseline);
    }


    @Override
    public void update(PoolBaseline baseline) {
        final String sql = """
                update %sbaseline set liveness_interval=?,
                                         connection_timeout=?,
                                         min_idle=?,
                                         max_size=?,
                                         strategy=?
                 where name=?
                """.formatted(getTablePrefix());

        int rows = getJdbcTemplate().update(sql, ps -> {
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
