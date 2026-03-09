package io.cockroachdb.pool.proxy.repository.jdbc;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

public abstract class AbstractPoolRepository {
    public static final String TABLE_PREFIX = "pool_";

    private String tablePrefix = TABLE_PREFIX;

    private final DataSource dataSource;

    private final JdbcTemplate jdbcTemplate;

    protected AbstractPoolRepository(DataSource dataSource) {
        Assert.notNull(dataSource, "dataSource is null");
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }
}
