package alvin.study.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.google.common.base.Charsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationTest {
    private ByteArrayOutputStream stream;
    private PrintStream orgOut;
    private PrintStream orgErr;

    @BeforeEach
    void beforeEach() {
        stream = new ByteArrayOutputStream();
        var out = new PrintStream(stream);

        orgOut = System.out;
        orgErr = System.err;

        System.setOut(out);
        System.setErr(out);
    }

    @AfterEach
    void afterEach() {
        if (orgOut != null) {
            System.out.close();
            System.setOut(orgOut);
        }
        if (orgErr != null) {
            System.setErr(orgErr);
        }
    }

    @Test
    void shouldRunWithNoArgs() {
        Application.main(new String[] {});
        System.out.flush();

        var output = new String(stream.toByteArray(), Charsets.UTF_8);
        assertEquals("Please press enter two numeric arguments\n", output);
    }

    @Test
    void shouldRunWithTwoArgs() {
        Application.main(new String[] { "12", "13" });
        System.out.flush();

        var output = new String(stream.toByteArray(), Charsets.UTF_8);
        assertEquals("The result is: 25.0\n", output);
    }

    @Test
    void shouldRunWithThreeArgs() {
        Application.main(new String[] { "12", "13", "SUB" });
        System.out.flush();

        var output = new String(stream.toByteArray(), Charsets.UTF_8);
        assertEquals("The result is: -1.0\n", output);
    }
}
