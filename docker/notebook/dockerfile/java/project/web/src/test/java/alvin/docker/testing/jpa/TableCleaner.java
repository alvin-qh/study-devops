package alvin.docker.testing.jpa;

import lombok.val;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class TableCleaner {
    private final EntityManager em;

    public TableCleaner(EntityManager em) {
        this.em = em;
    }

    @SuppressWarnings("unchecked")
    private List<String> listAllTables(String schema) {
        return em.createNativeQuery("SELECT `table_name` " +
                "FROM `information_schema`.`tables` " +
                "WHERE `table_schema`=:schame AND `table_type`='TABLE'")
                .setParameter("schame", schema)
                .getResultList();
    }

    public synchronized void clearAllTables(String... exclude) {
        final Session conn = em.unwrap(Session.class);
        conn.doWork(connection -> {
//            final String schema = connection.getCatalog();
            Set<String> excludeSet = newHashSet(exclude);
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            final val tables = listAllTables("PUBLIC");
            tables.stream()
                    .filter(t -> !excludeSet.contains(t))
                    .forEach(t ->
                            em.createNativeQuery("TRUNCATE TABLE " + t).executeUpdate());
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        });
    }
}
