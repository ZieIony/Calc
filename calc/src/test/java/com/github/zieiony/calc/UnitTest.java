package com.github.zieiony.calc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnitTest {
    Calc calc;

    @BeforeAll
    public void setUp(){
        calc = new Calc();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator('.');
        calc.getDecimalFormat().setDecimalFormatSymbols(symbols);
    }

    @Test
    public void testSingle() {
        assert 4 == calc.evaluate(" 4 ");
    }

    @Test
    void testSign() {
        assert -4 == calc.evaluate(" -  4 ");
        assert 4 == calc.evaluate(" +  4 ");
        assertThrows(NotANumberException.class, () -> {
            calc.evaluate(" --  4 ");
        });
    }

    @Test
    void testSum() {
        assert 4 == calc.evaluate("2 +3 - 1 ");
        assertThrows(EndOfExpressionException.class, () -> {
            calc.evaluate("2 +3 -  ");
        });
    }

    @Test
    void testMul() {
        assert 6 == calc.evaluate("4 *3 / 2 ");
        assertThrows(DivisionByZeroException.class, () -> {
            calc.evaluate("3/0");
        });
    }

    @Test
    void testPow() {
        assert 4 == calc.evaluate("2 ^4 ^0.5 ");
    }

    @Test
    void testBraces() {
        assert 2 == calc.evaluate("((2) + 2)/2 ");
        assertThrows(EndOfExpressionException.class, () -> {
            calc.evaluate("(((");
        });
        assertThrows(MissingBracketException.class, () -> {
            calc.evaluate("))");
        });
    }

    @Test
    void testFunc() {
        assert 0.01 < Math.abs(1 - calc.evaluate("cos 0"));
        assert 0.01 < Math.abs(1 - calc.evaluate("cos (2 -2)"));
        assertThrows(UnknownFunctionException.class, () -> {
            calc.evaluate("hello 5");
        });
    }
}
