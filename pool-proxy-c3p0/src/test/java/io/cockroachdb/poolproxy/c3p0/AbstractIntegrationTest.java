package io.cockroachdb.poolproxy.c3p0;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.cockroachdb.poolproxy.repository.jdbc.JdbcClusterRepository;

@SpringBootTest(classes = {TestApplication.class}, useMainMethod = SpringBootTest.UseMainMethod.NEVER)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration-test")
@ActiveProfiles({"default", "test"})
public abstract class AbstractIntegrationTest {
    @Autowired
    private DataSource dataSource;

    protected DataSource getDataSource() {
        return dataSource;
    }

    @BeforeAll
    public void initSchemaBeforeAll() {
        new JdbcClusterRepository(getDataSource()).initSchema();
    }

    @AfterAll
    public void dropSchemaAfterAll() {
        new JdbcClusterRepository(getDataSource()).dropSchema();
    }
}
