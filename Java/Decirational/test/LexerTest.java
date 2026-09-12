package decirational;

import java.util.ArrayList;

public final class LexerTest {
    private static Lexer<TightInteger> newLexer() {
        return new Lexer<>(TightInteger::new);
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("Lexer");
        final Lexer<TightInteger> lexer = newLexer();

        t.check(lexer.getTokens(null).isEmpty(), "null expression yields no tokens");
        t.check(lexer.getTokens("").isEmpty(), "empty expression yields no tokens");
        t.check(lexer.getTokens("   ").isEmpty(), "whitespace-only expression yields no tokens");

        t.checkEquals(lexer.getTokens("1+2").toString(), lexer.getTokens(" 1 + 2 ").toString(), "whitespace is stripped before tokenizing");

        ArrayList<Token> tokens = lexer.getTokens("12+3*4");
        t.checkEquals(5, tokens.size(), "12+3*4 has 5 tokens");
        t.checkEquals('i', tokens.get(0).getTypeCode(), "first token is an integer operand");
        t.check(Operator.PLUS == tokens.get(1), "second token is PLUS");
        t.checkEquals('i', tokens.get(2).getTypeCode(), "third token is an integer operand");
        t.check(Operator.MULTIPLICATION == tokens.get(3), "fourth token is MULTIPLICATION");
        t.checkEquals('i', tokens.get(4).getTypeCode(), "fifth token is an integer operand");

        tokens = lexer.getTokens("2147483647");
        t.checkEquals(1, tokens.size(), "int-boundary literal is one token");
        t.checkEquals('i', tokens.get(0).getTypeCode(), "Integer.MAX_VALUE parses as plain integer");

        tokens = lexer.getTokens("2147483648");
        t.checkEquals('l', tokens.get(0).getTypeCode(), "Integer.MAX_VALUE+1 parses as large integer");

        tokens = lexer.getTokens("99999999999999999999");
        t.checkEquals('l', tokens.get(0).getTypeCode(), "20-digit literal parses as large integer");

        tokens = lexer.getTokens("3.14");
        t.checkEquals(1, tokens.size(), "decimal literal is a single token");
        t.checkEquals('r', tokens.get(0).getTypeCode(), "decimal literal parses as rational");

        tokens = lexer.getTokens("0.{3}");
        t.checkEquals(1, tokens.size(), "cyclic decimal literal is a single token");
        t.checkEquals('r', tokens.get(0).getTypeCode(), "cyclic decimal literal parses as rational");

        tokens = lexer.getTokens("1/2");
        t.checkEquals(3, tokens.size(), "1/2 splits into operand, DIVISION, operand (not a fraction literal)");
        t.check(Operator.DIVISION == tokens.get(1), "middle token of 1/2 is DIVISION");

        tokens = lexer.getTokens("([|1|])");
        t.checkEquals(7, tokens.size(), "([|1|]) has 7 tokens");
        t.checkEquals('(', tokens.get(0).getTypeCode(), "opening paren type code");
        t.checkEquals('[', tokens.get(1).getTypeCode(), "opening floor type code");
        t.checkEquals('|', tokens.get(2).getTypeCode(), "absolute bar type code");
        t.checkEquals('i', tokens.get(3).getTypeCode(), "inner operand type code");
        t.checkEquals('|', tokens.get(4).getTypeCode(), "closing absolute bar has the same type code as the opening one");
        t.checkEquals(']', tokens.get(5).getTypeCode(), "closing floor type code");
        t.checkEquals(')', tokens.get(6).getTypeCode(), "closing paren type code");

        tokens = lexer.getTokens("++");
        t.checkEquals(2, tokens.size(), "repeated operator characters yield one token per character");
        t.check(Operator.PLUS == tokens.get(0) && Operator.PLUS == tokens.get(1), "both tokens are PLUS");

        tokens = lexer.getTokens("12(3)");
        t.checkEquals(4, tokens.size(), "12(3) splits into operand, paren, operand, paren");

        t.checkThrows(IllegalArgumentException.class, () -> lexer.getTokens("2#3"), "illegal character is rejected");
        t.checkThrows(IllegalArgumentException.class, () -> lexer.getTokens("5!"), "illegal character ! is rejected");

        // Regression: an illegal character in the first position used to make
        // char_table.get(...) return null and go unchecked, so recordToken
        // threw a NullPointerException instead of IllegalArgumentException.
        t.checkThrows(IllegalArgumentException.class, () -> lexer.getTokens("a"), "illegal leading character is rejected, not an NPE");
        t.checkThrows(IllegalArgumentException.class, () -> lexer.getTokens("@"), "illegal leading character @ is rejected, not an NPE");
        t.checkThrows(IllegalArgumentException.class, () -> lexer.getTokens("a1"), "illegal leading character before a digit is rejected, not an NPE");

        try {
            lexer.getTokens("abc");
            t.check(false, "abc is rejected as illegal");
        } catch (final IllegalArgumentException e) {
            t.check(null != e.getMessage() && e.getMessage().contains("character a"), "abc reports the first illegal character (a), not a later one (expected message to contain <character a> but was <" + e.getMessage() + ">)");
        }

        return t;
    }
}
