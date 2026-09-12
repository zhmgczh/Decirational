package decirational;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class Main {
    private static final String USAGE = """
            Usage: Main [--integer=decimal|tight] [--format=<format>] [--precision=N]

              --integer=decimal   use DecimalInteger for large integers (default)
              --integer=tight     use TightInteger for large integers

              --format=default    Rational.toString() (default): fraction, or plain integer when the denominator is 1
              --format=fraction   original (improper) fraction: always numerator/denominator
              --format=mixed      mixed number: whole part and proper fraction, e.g. 1 3/4
              --format=decimal    decimal expansion, with repeating digits in {}
              --format=truncate   decimal expansion truncated to --precision digits after the point
              --format=round      decimal expansion rounded to --precision digits after the point
              --format=ceil       decimal expansion rounded up (toward +infinity) to --precision digits after the point
              --format=floor      decimal expansion rounded down (toward -infinity) to --precision digits after the point

              --precision=N       digits after the decimal point for truncate/round/ceil/floor (default 0);
                                   ignored by default/fraction/decimal. N may be negative to round to tens,
                                   hundreds, etc. before the point.

            Reads one arithmetic expression per line from standard input and prints its value.""";

    public static void main(final String[] args) {
        String integer_type_name = "decimal";
        String format = "default";
        int precision = 0;
        for (final String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                System.out.println(USAGE);
                return;
            } else if (arg.startsWith("--integer=")) {
                integer_type_name = arg.substring("--integer=".length());
            } else if (arg.startsWith("--format=")) {
                format = arg.substring("--format=".length());
            } else if (arg.startsWith("--precision=")) {
                final String value = arg.substring("--precision=".length());
                try {
                    precision = Integer.parseInt(value);
                } catch (final NumberFormatException e) {
                    System.err.println("Invalid --precision value: " + value);
                    System.err.println(USAGE);
                    System.exit(1);
                    return;
                }
            } else {
                System.err.println("Unknown argument: " + arg);
                System.err.println(USAGE);
                System.exit(1);
                return;
            }
        }
        try {
            switch (integer_type_name) {
                case "decimal" -> run(DecimalInteger::new, DecimalInteger::new, format, precision);
                case "tight" -> run(TightInteger::new, TightInteger::new, format, precision);
                default -> throw new IllegalArgumentException("unknown integer type: " + integer_type_name + " (expected 'decimal' or 'tight')");
            }
        } catch (final IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(USAGE);
            System.exit(1);
        }
    }

    private static <T extends CustomInteger<T>> void run(final Function<String, T> from_string, final IntFunction<T> from_int, final String format, final int precision) {
        final Function<Rational<T>, String> formatter = make_formatter(format, precision);
        final Scanner input = new Scanner(System.in);
        final Lexer<T> lexer = new Lexer<>(from_string);
        final Parser<T> parser = new Parser<>(from_int);
        while (input.hasNextLine()) {
            final String expression = input.nextLine();
            if (expression.isBlank()) {
                continue;
            }
            try {
                final ArrayList<Token> tokens = lexer.get_tokens(expression);
                final Rational<T> result = parser.parse(tokens);
                System.out.println(formatter.apply(result));
            } catch (final RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static <T extends CustomInteger<T>> Function<Rational<T>, String> make_formatter(final String format, final int precision) {
        return switch (format) {
            case "default" -> Rational::toString;
            case "fraction" -> Rational::to_fraction_string;
            case "mixed" -> Rational::to_mixed_string;
            case "decimal" -> Rational::to_decimal_string;
            case "truncate" -> r -> r.to_truncate_decimal_string(precision);
            case "round" -> r -> r.to_round_decimal_string(precision);
            case "ceil" -> r -> r.to_ceil_decimal_string(precision);
            case "floor" -> r -> r.to_floor_decimal_string(precision);
            default -> throw new IllegalArgumentException("unknown format: " + format + " (expected default, fraction, mixed, decimal, truncate, round, ceil, or floor)");
        };
    }
}
