package alvin.gradle.checkstyle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckStyleDemoTest {

    @Test
    void checkStyle() {
        assertTrue(CheckStyleDemo.checkStyle());
    }
}
