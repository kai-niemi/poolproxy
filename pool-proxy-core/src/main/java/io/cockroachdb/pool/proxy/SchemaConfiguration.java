package io.cockroachdb.pool.proxy;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class SchemaConfiguration {
    public void initSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setCommentPrefix("--");
        populator.setIgnoreFailedDrops(true);
        populator.addScript(new ClassPathResource("io/cockroachdb/pool/proxy/schema-cockroachdb.sql"));

        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    public void dropSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setCommentPrefix("--");
        populator.setIgnoreFailedDrops(false);
        populator.addScript(new ClassPathResource("io/cockroachdb/pool/proxy/schema-drop-cockroachdb.sql"));

        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
