package alvin.docker.testing.jpa;

import java.io.Closeable;

public interface Transaction extends Closeable {
    void commit();

    void rollback();

    default void close() {
        commit();
    }
}
