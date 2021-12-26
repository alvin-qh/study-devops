package alvin.gradle.java;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void test_format() {
        final Main main = new Main();
        final String actual = main.format(new String[]{"A", "B", "C"});
        assertThat(actual, is("Main class is running, arguments is A,B,C ..."));
    }
}
