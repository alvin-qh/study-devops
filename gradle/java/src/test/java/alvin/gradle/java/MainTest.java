package alvin.gradle.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void shouldCommandLineArgsFormatted() {
        var main = new Main();
        var actual = main.format(new String[] { "A", "B", "C" });
        assertThat(actual, is(equalTo("Main class is running, arguments is A,B,C ...")));
    }
}
