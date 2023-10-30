package alvin.study.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SimpleTest {
    @Test
    void shouldAddTwoNumbers() {
        var simple = new Simple();
        var r = simple.add(12, 13);
        assertEquals(25, r);
    }
}
