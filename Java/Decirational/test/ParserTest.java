package decirational;

import java.util.ArrayList;

public final class ParserTest {
    private static Lexer<TightInteger> new_lexer() {
        return new Lexer<>(TightInteger::new);
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("Parser");
        final Lexer<TightInteger> lexer = new_lexer();
        final Parser<TightInteger> parser = new Parser<>(TightInteger::new);

        // eval(expr) returns the decimal-string result of lexing+parsing.
        final java.util.function.Function<String, String> eval = expr -> parser.parse(lexer.get_tokens(expr)).to_decimal_string();

        // Precedence and associativity
        t.check_equals("14", eval.apply("2+3*4"), "* binds tighter than +");
        t.check_equals("20", eval.apply("(2+3)*4"), "parentheses override precedence");
        t.check_equals("1", eval.apply("10%3"), "modulo");
        t.check_equals("1024", eval.apply("2^10"), "exponentiation");
        t.check_equals("0.25", eval.apply("2^-2"), "negative exponent yields a fraction");
        t.check_equals("512", eval.apply("2^3^2"), "^ is right-associative: 2^(3^2)");
        t.check_equals("-4", eval.apply("-2^2"), "unary minus binds looser than ^: -(2^2)");
        t.check_equals("4", eval.apply("(-2)^2"), "parenthesized unary minus is squared away");
        t.check_equals("81", eval.apply("((1+2)*3)^2"), "nested parentheses");

        // Unary operators
        t.check_equals("2", eval.apply("-3+5"), "leading unary minus");
        t.check_equals("2", eval.apply("+3-1"), "leading unary plus is a no-op");
        t.check_equals("8", eval.apply("5++3"), "chained unary plus");
        t.check_equals("8", eval.apply("5--3"), "double negative acts like a plus");
        t.check_equals("5", eval.apply("--5"), "double leading negation cancels");
        t.check_equals("2", eval.apply("5-+3"), "mixed unary plus after minus");

        // Floor
        t.check_equals("3", eval.apply("[3.7]"), "floor of positive fraction");
        t.check_equals("-4", eval.apply("[-3.7]"), "floor of negative fraction rounds toward -infinity");
        t.check_equals("5", eval.apply("[5]"), "floor of an integer is a no-op");
        t.check_equals("-5", eval.apply("[-5]"), "floor of a negative integer is a no-op");
        t.check_equals("0", eval.apply("[0]"), "floor of zero");

        // Absolute value
        t.check_equals("5", eval.apply("|-5|"), "absolute value of a negative literal");
        t.check_equals("5", eval.apply("|5|"), "absolute value of a positive literal");
        t.check_equals("7", eval.apply("|3-10|"), "absolute value of a subexpression");
        t.check_equals("14", eval.apply("|3-10|*2"), "absolute value combined with multiplication");

        // Nesting floor and absolute
        t.check_equals("8", eval.apply("|[-7.5]|"), "absolute value of a floor");
        t.check_equals("7", eval.apply("[|-7.5|]"), "floor of an absolute value");

        // Big integers
        t.check_equals("246913578024691357802469135780", eval.apply("123456789012345678901234567890*2"), "big-integer multiplication");

        // Rational literals and repeating decimals
        t.check_equals("0.{3}", eval.apply("0.{3}"), "cyclic decimal literal passes through unchanged");
        t.check_equals("14.{285714}", eval.apply("100/7"), "division producing a repeating decimal");
        t.check_equals("0.5", eval.apply("1/3+1/6"), "fraction sum reduces to a terminating decimal");

        // Modulo restricted to integers
        t.check_equals("-1", eval.apply("-10%3"), "modulo follows the dividend's sign");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("5%2.5"), "modulo of a non-integer is rejected");

        // Integer division ("//"), paired with modulo the same way divide_by/modulo pair at the CustomInteger level
        t.check_equals("3", eval.apply("7//2"), "7//2 truncates toward zero");
        t.check_equals("-3", eval.apply("-7//2"), "-7//2 truncates toward zero");
        t.check_equals("-3", eval.apply("7//-2"), "7//-2 truncates toward zero");
        t.check_equals("3", eval.apply("-7//-2"), "-7//-2 truncates toward zero");
        t.check_equals("14", eval.apply("100//7"), "100//7");
        t.check_equals("16", eval.apply("100//3//2"), "// is left-associative");
        t.check_equals("3", eval.apply("2+3//2"), "// binds tighter than +");
        t.check_equals("3", eval.apply("2*3//2"), "// is same precedence as * (left to right)");
        t.check_equals("1", eval.apply("(1+2)//2"), "// on a parenthesized expression");
        t.check_equals("-7", eval.apply("(-7//2)*2+(-7%2)"), "(a//b)*b+(a%b)==a, matching divide_by_and_modulo");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("7.5//2"), "integer division of a non-integer is rejected");
        t.check_throws(ArithmeticException.class, () -> eval.apply("5//0"), "integer division by zero is rejected");
        t.check_equals("[//, /]", lexer.get_tokens("///").toString(), "an odd run of '/' greedily pairs into // then a lone /");
        t.check_equals("[//, //]", lexer.get_tokens("////").toString(), "an even run of '/' greedily pairs into // tokens");

        // Division by zero
        t.check_throws(ArithmeticException.class, () -> eval.apply("1/0"), "division by zero is rejected");
        t.check_throws(ArithmeticException.class, () -> eval.apply("5/(2-2)"), "division by a computed zero is rejected");

        // Exponent must be an integer within int range
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("2^2.5"), "non-integer exponent is rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("2^99999999999999999999"), "exponent outside int range is rejected");

        // Syntax errors
        t.check_throws(IllegalArgumentException.class, () -> parser.parse(new ArrayList<>()), "empty token list is rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("2+"), "trailing operator with no operand is rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("(1+2"), "unmatched opening parenthesis is rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("()"), "empty parentheses are rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("2(3)"), "trailing tokens after a complete expression are rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("[5"), "unmatched opening floor bracket is rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("|5"), "unmatched opening absolute-value bar is rejected");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("2#3"), "illegal characters surface as errors through the full pipeline");

        // Expression-depth limit: a deeply nested/chained expression must be
        // rejected as a normal, catchable error instead of crashing the
        // whole process with an uncatchable StackOverflowError. Regression
        // test for a real DoS: any of these three independent recursion
        // paths (bracket/floor/absolute nesting, chained unary +/-, chained
        // right-associative ^) previously took down the whole interpreter on
        // a single malicious input line.
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("(".repeat(10_000) + "1" + ")".repeat(10_000)), "deeply nested parentheses are rejected, not a StackOverflowError");
        t.check_throws(IllegalArgumentException.class, () -> eval.apply("-".repeat(10_000) + "5"), "a long chain of unary minus is rejected, not a StackOverflowError");
        // Base 1 (not 2): 1^1^1^...^1 never produces an exponent outside int
        // range, so this exercises the depth guard itself rather than
        // coincidentally failing "exponent out of range" first.
        t.check_throws(IllegalArgumentException.class, () -> eval.apply(String.join("^", java.util.Collections.nCopies(10_000, "1"))), "a long chain of ^ is rejected, not a StackOverflowError");
        t.check_equals("1", eval.apply("(".repeat(500) + "1" + ")".repeat(500)), "nesting well under the depth limit still works");

        return t;
    }
}
