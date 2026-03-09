package io.cockroachdb.pool.proxy;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestApplication.class}, useMainMethod = SpringBootTest.UseMainMethod.NEVER)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration-test")
@ActiveProfiles({"default", "test"})
@Import(SchemaConfiguration.class)
public abstract class AbstractIntegrationTest {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private SchemaConfiguration schemaConfiguration;

    protected DataSource getDataSource() {
        return dataSource;
    }

    @BeforeAll
    public void initSchemaBeforeAll() {
        schemaConfiguration.initSchema(dataSource);
    }

    @AfterAll
    public void dropSchemaAfterAll() {
        schemaConfiguration.dropSchema(dataSource);
    }
}
