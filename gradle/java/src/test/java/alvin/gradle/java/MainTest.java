package alvin.gradle.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void shouldCommandLineArgsFormatted() {
        final Main main = new Main();
        final String actual = main.format(new String[] { "A", "B", "C" });
        assertThat(actual, is("Main class is running, arguments is A,B,C ..."));
    }
}
