package alvin.docker.testing;

import alvin.docker.testing.jpa.TableCleaner;
import alvin.docker.testing.jpa.TransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Configuration
public class Config {
    @PersistenceContext
    private EntityManager em;

    @Bean
    public TransactionManager makeTransactionManager(PlatformTransactionManager ptm) {
        return new TransactionManager(ptm);
    }

    @Bean
    public TableCleaner tableCleaner() {
        return new TableCleaner(em);
    }
}
