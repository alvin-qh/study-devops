package alvin.study.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import com.google.common.base.Charsets;

import org.junit.jupiter.api.Test;

class MainTest {

    private PrintStream resetSystemOut(OutputStream out) {
        var ps = new PrintStream(out);

        var oldOut = System.out;
        System.setOut(ps);
        return oldOut;
    }

    @Test
    void shouldExecuteWorked() throws Exception {
        try (var out = new ByteArrayOutputStream()) {
            var oldOut = resetSystemOut(out);

            var main = new Main();
            main.execute(new String[] { "12", "13" });

            System.out.flush();
            var res = new String(out.toByteArray(), Charsets.UTF_8);
            assertEquals("Hello maven project\nThe result is: 25.0\n", res);

            System.setOut(oldOut);
        }
    }
}
