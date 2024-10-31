package alvin.study.maven.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnitTest {
    private final Unit unit = new Unit();

    @Test
    void shouldCalculateAsAdd() {
        var exp = new Expression(10, 20, Operator.ADD);
        var result = unit.calculate(exp);
        assertEquals(30.0, result);
    }

    @Test
    void shouldCalculateAsSub() {
        var exp = new Expression(10, 20, Operator.SUB);
        var result = unit.calculate(exp);
        assertEquals(-10.0, result);
    }

    @Test
    void shouldCalculateAsMul() {
        var exp = new Expression(10, 20, Operator.MUL);
        var result = unit.calculate(exp);
        assertEquals(200.0, result);
    }

    @Test
    void shouldCalculateAsDiv() {
        var exp = new Expression(10, 20, Operator.DIV);
        var result = unit.calculate(exp);
        assertEquals(0.5, result);
    }

    @Test
    void shouldCalculateAsMod() {
        var exp = new Expression(10, 20, Operator.MOD);
        var result = unit.calculate(exp);
        assertEquals(10.0, result);
    }
}
