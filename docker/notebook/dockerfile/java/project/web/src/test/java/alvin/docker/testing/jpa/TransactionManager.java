package alvin.docker.testing.jpa;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TransactionManager {
    private PlatformTransactionManager tm;

    class TransactionImpl implements Transaction {
        private TransactionStatus status;

        TransactionImpl() {
            this.status = tm.getTransaction(new DefaultTransactionDefinition());
        }

        @Override
        public void commit() {
            if (status != null) {
                tm.commit(status);
                status = null;
            }
        }

        @Override
        public void rollback() {
            if (status != null) {
                tm.rollback(status);
                status = null;
            }
        }
    }

    public TransactionManager(PlatformTransactionManager tm) {
        this.tm = tm;
    }

    public Transaction begin() {
        return new TransactionImpl();
    }
}
