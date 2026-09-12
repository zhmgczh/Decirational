import java.util.ArrayList;
import java.util.function.IntFunction;

public final class Parser<T extends CustomInteger<T>> {
    // parse_unary is the one point every recursive production in this
    // grammar passes through at least once per level of nesting: directly
    // for a chain of unary +/- (parse_unary calling itself), for bracket/
    // floor/absolute-value nesting (via the expression->term->unary->power
    // ->primary chain that runs once per level before the next '(', '[' or
    // '|'), and for right-associative '^' chains (parse_power calling
    // parse_unary for its exponent, which can lead straight back into
    // parse_power). Java's call stack has no bound of its own worth relying
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
        final Rational<T> result = parse_expression();
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

    private Rational<T> parse_expression() {
        Rational<T> result = parse_term();
        while (check(Operator.PLUS) || check(Operator.MINUS)) {
            final Operator operator = (Operator) advance();
            final Rational<T> right = parse_term();
            result = Operator.PLUS == operator ? result.plus(right) : result.minus(right);
        }
        return result;
    }

    private Rational<T> parse_term() {
        Rational<T> result = parse_unary();
        while (check(Operator.MULTIPLICATION) || check(Operator.DIVISION) || check(Operator.INTEGER_DIVISION) || check(Operator.MODULO)) {
            final Operator operator = (Operator) advance();
            final Rational<T> right = parse_unary();
            result = switch (operator) {
                case MULTIPLICATION -> result.multiply(right);
                case DIVISION -> result.divide_by(right);
                case INTEGER_DIVISION -> integer_divide(result, right);
                case MODULO -> modulo(result, right);
                default -> throw new IllegalStateException("unexpected operator: " + operator);
            };
        }
        return result;
    }

    private Rational<T> integer_divide(final Rational<T> a, final Rational<T> b) {
        if (!a.is_integer() || !b.is_integer()) {
            throw new IllegalArgumentException("integer division is only supported between integers");
        }
        return new Rational<>(a.get_numerator().divide_by(b.get_numerator()));
    }

    private Rational<T> modulo(final Rational<T> a, final Rational<T> b) {
        if (!a.is_integer() || !b.is_integer()) {
            throw new IllegalArgumentException("modulo is only supported between integers");
        }
        return new Rational<>(a.get_numerator().modulo(b.get_numerator()));
    }

    private Rational<T> parse_unary() {
        ++depth;
        try {
            if (depth > MAX_EXPRESSION_DEPTH) {
                throw new IllegalArgumentException("expression nested too deeply (max depth " + MAX_EXPRESSION_DEPTH + ")");
            }
            if (check(Operator.PLUS)) {
                advance();
                return parse_unary();
            }
            if (check(Operator.MINUS)) {
                advance();
                return parse_unary().negate();
            }
            return parse_power();
        } finally {
            --depth;
        }
    }

    private Rational<T> parse_power() {
        final Rational<T> base = parse_primary();
        if (check(Operator.POWER)) {
            advance();
            final Rational<T> exponent = parse_unary();
            return base.pow(to_int_exponent(exponent));
        }
        return base;
    }

    private int to_int_exponent(final Rational<T> exponent) {
        if (!exponent.is_integer()) {
            throw new IllegalArgumentException("exponent must be an integer: " + exponent);
        }
        try {
            return Integer.parseInt(exponent.get_numerator().toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("exponent out of range: " + exponent, e);
        }
    }

    private Rational<T> parse_primary() {
        final Token token = peek();
        if (null == token) {
            throw new IllegalArgumentException("unexpected end of input");
        }
        if (token instanceof Operand operand) {
            advance();
            return to_rational(operand);
        }
        if (token == Parenthesis.LEFT_PARENTHESIS) {
            advance();
            final Rational<T> result = parse_expression();
            expect(Parenthesis.RIGHT_PARENTHESIS);
            return result;
        }
        if (token == Floor.LEFT_FLOOR) {
            advance();
            final Rational<T> result = parse_expression();
            expect(Floor.RIGHT_FLOOR);
            return new Rational<>(floor(result));
        }
        if (token == Absolute.ABSOLUTE) {
            advance();
            final Rational<T> result = parse_expression();
            expect(Absolute.ABSOLUTE);
            return result.abs();
        }
        throw new IllegalArgumentException("unexpected token: " + token);
    }

    private Rational<T> to_rational(final Operand operand) {
        final Object value = operand.get_value();
        return switch (operand.get_operand_type()) {
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
        final T numerator = rational.get_numerator();
        if (rational.is_integer()) {
            return numerator;
        }
        final T denominator = rational.get_denominator();
        final T quotient = numerator.divide_by_and_modulo(denominator)[0];
        if (numerator.is_negative()) {
            return quotient.minus(denominator.pow(0));
        }
        return quotient;
    }
}
