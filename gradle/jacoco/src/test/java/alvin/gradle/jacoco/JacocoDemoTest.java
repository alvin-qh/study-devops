package alvin.gradle.jacoco;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JacocoDemoTest {

    @Test
    void shouldCheckNumberIsPrime() {
        var demo = new JacocoDemo();

        assertTrue(demo.isPrime(2));
        assertTrue(demo.isPrime(3));
        assertFalse(demo.isPrime(4));
        assertTrue(demo.isPrime(5));
        assertTrue(demo.isPrime(26881));
        assertFalse(demo.isPrime(26882));
        assertTrue(demo.isPrime(49999));
    }
}
