package alvin.docker.testing;

import alvin.docker.Main;
import alvin.docker.infra.model.BaseEntity;
import alvin.docker.testing.builder.BuilderFactory;
import alvin.docker.testing.jpa.TableCleaner;
import alvin.docker.testing.jpa.Transaction;
import alvin.docker.testing.jpa.TransactionManager;
import alvin.docker.testing.web.ContextMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Main.class, Config.class}, webEnvironment = RANDOM_PORT,
        properties = {
                "spring.activiti.check-process-definitions=false",
                "spring.datasource.hikari.pool-name=cp-wg.microservice.workflow",
                "spring.datasource.hikari.auto-commit=true",
                "spring.jpa.show-sql=true",
                "spring.jpa.open-in-view=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                "spring.flyway.table=" + Main.TABLE_SCHEMA_VERSION,
                "spring.main.banner-mode=off",
                "spring.flyway.locations=classpath:migrations"
        })
@ActiveProfiles("test")
public abstract class IntegrationTestSupport extends UnitTestSupport {
    private static boolean tableCleaned = false;

    @PersistenceContext
    private EntityManager em;

    @Inject
    private TransactionManager tm;

    @Inject
    private TableCleaner tableCleaner;

    @Inject
    private ContextMocker contextMocker;

    @Inject
    private BuilderFactory builderFactory;

    protected Transaction beginTx() {
        return tm.begin();
    }

    protected void clearJPASession() {
        em.clear();
    }

    @SuppressWarnings("unchecked")
    protected <T extends BaseEntity> T refreshEntity(T old) {
        try {
            var field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);

            return (T) em.find(old.getClass(), field.get(old));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T newBuilder(Class<T> type) {
        return builderFactory.newBuilder(type);
    }

    @BeforeEach
    protected void setUp() throws Exception {
        synchronized (IntegrationTestSupport.class) {
            if (!tableCleaned) {
                try (var ignore = beginTx()) {
                    tableCleaner.clearAllTables(Main.TABLE_SCHEMA_VERSION);
                }
                tableCleaned = true;
            }
        }
        contextMocker.mock();
    }

    @AfterEach
    protected void tearDown() {
        contextMocker.clear();
    }
}
