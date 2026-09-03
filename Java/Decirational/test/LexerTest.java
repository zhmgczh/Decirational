import java.util.ArrayList;

public final class LexerTest {
    @SuppressWarnings("unchecked")
    private static Lexer<TightInteger> new_lexer() {
        return new Lexer<>(TightInteger.class, (Class<Rational<TightInteger>>) (Class<?>) Rational.class);
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("Lexer");
        final Lexer<TightInteger> lexer = new_lexer();

        t.check(lexer.get_tokens(null).isEmpty(), "null expression yields no tokens");
        t.check(lexer.get_tokens("").isEmpty(), "empty expression yields no tokens");
        t.check(lexer.get_tokens("   ").isEmpty(), "whitespace-only expression yields no tokens");

        t.check_equals(lexer.get_tokens("1+2").toString(), lexer.get_tokens(" 1 + 2 ").toString(), "whitespace is stripped before tokenizing");

        ArrayList<Token> tokens = lexer.get_tokens("12+3*4");
        t.check_equals(5, tokens.size(), "12+3*4 has 5 tokens");
        t.check_equals('i', tokens.get(0).get_type_code(), "first token is an integer operand");
        t.check(Operator.PLUS == tokens.get(1), "second token is PLUS");
        t.check_equals('i', tokens.get(2).get_type_code(), "third token is an integer operand");
        t.check(Operator.MULTIPLICATION == tokens.get(3), "fourth token is MULTIPLICATION");
        t.check_equals('i', tokens.get(4).get_type_code(), "fifth token is an integer operand");

        tokens = lexer.get_tokens("2147483647");
        t.check_equals(1, tokens.size(), "int-boundary literal is one token");
        t.check_equals('i', tokens.get(0).get_type_code(), "Integer.MAX_VALUE parses as plain integer");

        tokens = lexer.get_tokens("2147483648");
        t.check_equals('l', tokens.get(0).get_type_code(), "Integer.MAX_VALUE+1 parses as large integer");

        tokens = lexer.get_tokens("99999999999999999999");
        t.check_equals('l', tokens.get(0).get_type_code(), "20-digit literal parses as large integer");

        tokens = lexer.get_tokens("3.14");
        t.check_equals(1, tokens.size(), "decimal literal is a single token");
        t.check_equals('r', tokens.get(0).get_type_code(), "decimal literal parses as rational");

        tokens = lexer.get_tokens("0.{3}");
        t.check_equals(1, tokens.size(), "cyclic decimal literal is a single token");
        t.check_equals('r', tokens.get(0).get_type_code(), "cyclic decimal literal parses as rational");

        tokens = lexer.get_tokens("1/2");
        t.check_equals(3, tokens.size(), "1/2 splits into operand, DIVISION, operand (not a fraction literal)");
        t.check(Operator.DIVISION == tokens.get(1), "middle token of 1/2 is DIVISION");

        tokens = lexer.get_tokens("([|1|])");
        t.check_equals(7, tokens.size(), "([|1|]) has 7 tokens");
        t.check_equals('(', tokens.get(0).get_type_code(), "opening paren type code");
        t.check_equals('[', tokens.get(1).get_type_code(), "opening floor type code");
        t.check_equals('|', tokens.get(2).get_type_code(), "absolute bar type code");
        t.check_equals('i', tokens.get(3).get_type_code(), "inner operand type code");
        t.check_equals('|', tokens.get(4).get_type_code(), "closing absolute bar has the same type code as the opening one");
        t.check_equals(']', tokens.get(5).get_type_code(), "closing floor type code");
        t.check_equals(')', tokens.get(6).get_type_code(), "closing paren type code");

        tokens = lexer.get_tokens("++");
        t.check_equals(2, tokens.size(), "repeated operator characters yield one token per character");
        t.check(Operator.PLUS == tokens.get(0) && Operator.PLUS == tokens.get(1), "both tokens are PLUS");

        tokens = lexer.get_tokens("12(3)");
        t.check_equals(4, tokens.size(), "12(3) splits into operand, paren, operand, paren");

        t.check_throws(IllegalArgumentException.class, () -> lexer.get_tokens("2#3"), "illegal character is rejected");
        t.check_throws(IllegalArgumentException.class, () -> lexer.get_tokens("5!"), "illegal character ! is rejected");

        return t;
    }
}
