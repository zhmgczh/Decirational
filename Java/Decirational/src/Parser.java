package decirational;

import java.util.ArrayList;
import java.util.function.IntFunction;

public final class Parser<T extends CustomInteger<T>> {
    // parseUnary is the one point every recursive production in this
    // grammar passes through at least once per level of nesting: directly
    // for a chain of unary +/- (parseUnary calling itself), for bracket/
    // floor/absolute-value nesting (via the expression->term->unary->power
    // ->primary chain that runs once per level before the next '(', '[' or
    // '|'), and for right-associative '^' chains (parsePower calling
    // parseUnary for its exponent, which can lead straight back into
    // parsePower). Java's call stack has no bound of its own worth relying
    // on - a deeply nested or chained expression overflows it with an
    // uncatchable StackOverflowError (an Error, not a RuntimeException, so
    // the REPL's catch block never sees it and the whole process dies) well
    // before this limit, so this is checked well short of that: crafting a
    // one-line expression this deep is trivial for an attacker, and no
    // legitimate expression needs anywhere near it.
    private static final int MAX_EXPRESSION_DEPTH = 1000;
    private final IntFunction<T> from_int;
    private ArrayList<Token> tokens;
    private int position;
    private int depth;

    public Parser(final IntFunction<T> from_int) {
        this.from_int = from_int;
    }

    public Rational<T> parse(final ArrayList<Token> tokens) {
        if (null == tokens || tokens.isEmpty()) {
            throw new IllegalArgumentException("no tokens to parse");
        }
        this.tokens = tokens;
        this.position = 0;
        this.depth = 0;
        final Rational<T> result = parseExpression();
        if (position != tokens.size()) {
            throw new IllegalArgumentException("unexpected token: " + tokens.get(position));
        }
        return result;
    }

    private Token peek() {
        return position < tokens.size() ? tokens.get(position) : null;
    }

    private Token advance() {
        return tokens.get(position++);
    }

    private boolean check(final Token expected) {
        final Token token = peek();
        return null != token && token == expected;
    }

    private void expect(final Token expected) {
        if (!check(expected)) {
            final Token found = peek();
            throw new IllegalArgumentException("expected " + expected + " but found " + (null == found ? "end of input" : found));
        }
        ++position;
    }

    private Rational<T> parseExpression() {
        Rational<T> result = parseTerm();
        while (check(Operator.PLUS) || check(Operator.MINUS)) {
            final Operator operator = (Operator) advance();
            final Rational<T> right = parseTerm();
            result = Operator.PLUS == operator ? result.plus(right) : result.minus(right);
        }
        return result;
    }

    private Rational<T> parseTerm() {
        Rational<T> result = parseUnary();
        while (check(Operator.MULTIPLICATION) || check(Operator.DIVISION) || check(Operator.INTEGER_DIVISION) || check(Operator.MODULO)) {
            final Operator operator = (Operator) advance();
            final Rational<T> right = parseUnary();
            result = switch (operator) {
                case MULTIPLICATION -> result.multiply(right);
                case DIVISION -> result.divideBy(right);
                case INTEGER_DIVISION -> integerDivide(result, right);
                case MODULO -> modulo(result, right);
                default -> throw new IllegalStateException("unexpected operator: " + operator);
            };
        }
        return result;
    }

    private Rational<T> integerDivide(final Rational<T> a, final Rational<T> b) {
        if (!a.isInteger() || !b.isInteger()) {
            throw new IllegalArgumentException("integer division is only supported between integers");
        }
        return new Rational<>(a.getNumerator().divideBy(b.getNumerator()));
    }

    private Rational<T> modulo(final Rational<T> a, final Rational<T> b) {
        if (!a.isInteger() || !b.isInteger()) {
            throw new IllegalArgumentException("modulo is only supported between integers");
        }
        return new Rational<>(a.getNumerator().modulo(b.getNumerator()));
    }

    private Rational<T> parseUnary() {
        ++depth;
        try {
            if (depth > MAX_EXPRESSION_DEPTH) {
                throw new IllegalArgumentException("expression nested too deeply (max depth " + MAX_EXPRESSION_DEPTH + ")");
            }
            if (check(Operator.PLUS)) {
                advance();
                return parseUnary();
            }
            if (check(Operator.MINUS)) {
                advance();
                return parseUnary().negate();
            }
            return parsePower();
        } finally {
            --depth;
        }
    }

    private Rational<T> parsePower() {
        final Rational<T> base = parsePrimary();
        if (check(Operator.POWER)) {
            advance();
            final Rational<T> exponent = parseUnary();
            return base.pow(toIntExponent(exponent));
        }
        return base;
    }

    private int toIntExponent(final Rational<T> exponent) {
        if (!exponent.isInteger()) {
            throw new IllegalArgumentException("exponent must be an integer: " + exponent);
        }
        try {
            return Integer.parseInt(exponent.getNumerator().toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("exponent out of range: " + exponent, e);
        }
    }

    private Rational<T> parsePrimary() {
        final Token token = peek();
        if (null == token) {
            throw new IllegalArgumentException("unexpected end of input");
        }
        if (token instanceof Operand operand) {
            advance();
            return toRational(operand);
        }
        if (token == Parenthesis.LEFT_PARENTHESIS) {
            advance();
            final Rational<T> result = parseExpression();
            expect(Parenthesis.RIGHT_PARENTHESIS);
            return result;
        }
        if (token == Floor.LEFT_FLOOR) {
            advance();
            final Rational<T> result = parseExpression();
            expect(Floor.RIGHT_FLOOR);
            return new Rational<>(floor(result));
        }
        if (token == Absolute.ABSOLUTE) {
            advance();
            final Rational<T> result = parseExpression();
            expect(Absolute.ABSOLUTE);
            return result.abs();
        }
        throw new IllegalArgumentException("unexpected token: " + token);
    }

    private Rational<T> toRational(final Operand operand) {
        final Object value = operand.getValue();
        return switch (operand.getOperandType()) {
            case RATIONAL -> {
                @SuppressWarnings("unchecked") final Rational<T> rational = (Rational<T>) value;
                yield rational;
            }
            case LARGE_INTEGER -> {
                @SuppressWarnings("unchecked") final T large_integer = (T) value;
                yield new Rational<>(large_integer);
            }
            case INTEGER -> new Rational<>(from_int.apply((Integer) value));
        };
    }

    private T floor(final Rational<T> rational) {
        final T numerator = rational.getNumerator();
        if (rational.isInteger()) {
            return numerator;
        }
        final T denominator = rational.getDenominator();
        final T quotient = numerator.divideByAndModulo(denominator)[0];
        if (numerator.isNegative()) {
            return quotient.minus(denominator.pow(0));
        }
        return quotient;
    }
}
