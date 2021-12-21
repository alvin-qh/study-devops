package alvin.docker.testing;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public abstract class UnitTestSupport {
    @SuppressWarnings("UnstableApiUsage")
    protected String loadResourceAsString(String resourceName) throws IOException {
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(resourceName), Charsets.UTF_8)) {
            return CharStreams.toString(reader);
        }
    }

    protected void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }
}
