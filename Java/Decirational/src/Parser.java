import java.util.ArrayList;

public final class Parser<T extends CustomInteger<T>> {
    private final Class<T> large_integer_type;
    private ArrayList<Token> tokens;
    private int position;

    public Parser(final Class<T> large_integer_type) {
        this.large_integer_type = large_integer_type;
    }

    public Rational<T> parse(final ArrayList<Token> tokens) {
        if (null == tokens || tokens.isEmpty()) {
            throw new IllegalArgumentException("no tokens to parse");
        }
        this.tokens = tokens;
        this.position = 0;
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
        if (check(Operator.PLUS)) {
            advance();
            return parse_unary();
        }
        if (check(Operator.MINUS)) {
            advance();
            return parse_unary().negate();
        }
        return parse_power();
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
        if (value instanceof Rational) {
            @SuppressWarnings("unchecked") final Rational<T> rational = (Rational<T>) value;
            return rational;
        }
        if (large_integer_type.isInstance(value)) {
            return new Rational<>(large_integer_type.cast(value));
        }
        if (value instanceof Integer integer) {
            try {
                final T large_integer = large_integer_type.getConstructor(int.class).newInstance(integer);
                return new Rational<>(large_integer);
            } catch (Exception e) {
                throw new IllegalArgumentException("cannot instantiate integer type from int value", e);
            }
        }
        throw new IllegalArgumentException("unrecognized operand value: " + value);
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
