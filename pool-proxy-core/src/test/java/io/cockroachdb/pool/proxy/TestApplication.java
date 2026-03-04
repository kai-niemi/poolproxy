package io.cockroachdb.pool.proxy;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@SpringBootApplication
@Configuration
public class TestApplication {
    private final Logger logger = LoggerFactory.getLogger("io.cockroachdb.SQL_TRACE");

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return logger.isTraceEnabled() ? loggingProxy(targetDataSource()) : targetDataSource();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource targetDataSource() {
        HikariDataSource ds = dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.addDataSourceProperty("reWriteBatchedInserts", true);
        ds.addDataSourceProperty("ApplicationName", "pool-proxy");
        return ds;
    }

    private DataSource loggingProxy(DataSource dataSource) {
        final Formatter formatterBasic = FormatStyle.BASIC.getFormatter();
        final Formatter formatterHighlight = FormatStyle.HIGHLIGHT.getFormatter();

        DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator() {
            @Override
            protected String formatQuery(String query) {
                return formatterHighlight.format(formatterBasic.format(query));
            }
        };
        creator.setMultiline(true);

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setLogger(logger);
        listener.setLogLevel(SLF4JLogLevel.TRACE);
        listener.setWriteConnectionId(true);
        listener.setWriteIsolation(true);
        listener.setQueryLogEntryCreator(creator);

        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL-Trace")
                .asJson()
                .listener(listener)
                .build();
    }
}
