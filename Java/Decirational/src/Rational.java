import java.util.Objects;
import java.util.Scanner;

public final class Rational<T extends CustomInteger<T>> implements Comparable<Rational<T>> {
    private final T numerator;
    private final T denominator;

    public T get_numerator() {
        return numerator;
    }

    public T get_denominator() {
        return denominator;
    }

    private Rational<T> get_one() {
        return new Rational<>(denominator.pow(0), denominator.pow(0));
    }

    public Rational(T numerator, T denominator) {
        if (null == numerator || null == denominator) {
            throw new NullPointerException("Numerator and denominator cannot be null.");
        }
        if (denominator.is_zero()) {
            throw new IllegalArgumentException("Denominator cannot be zero.");
        }
        final T gcd = numerator.gcd(denominator);
        if (denominator.is_negative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator.divide_by(gcd);
        this.denominator = denominator.divide_by(gcd);
    }

    public Rational(final T integer) {
        if (null == integer) {
            throw new NullPointerException("Integer cannot be null.");
        }
        this.numerator = integer;
        this.denominator = integer.pow(0);
    }

    public Rational(final Rational<T> integer) {
        this.numerator = integer.numerator;
        this.denominator = integer.denominator;
    }

    public Rational(String string, final Class<T> integer_type) {
        string = string.replaceAll("\\s", "");
        boolean negative = false;
        int starting_point = 0;
        if (Arithmetic.is_minus(string.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (Arithmetic.is_plus(string.charAt(0))) {
            starting_point = 1;
        }
        int fraction_bar = -1;
        for (int i = starting_point; i < string.length(); ++i) {
            if (Arithmetic.is_fraction_bar(string.charAt(i))) {
                if (-1 != fraction_bar) {
                    throw new NumberFormatException("The rational string cannot contain more than one fraction bars!");
                }
                fraction_bar = i;
            } else if (!Arithmetic.is_digit(string.charAt(i))) {
                throw new NumberFormatException("The rational string does not have the right format!!");
            }
        }
        final String numerator_string, denominator_string;
        if (-1 == fraction_bar) {
            numerator_string = string.substring(starting_point);
            denominator_string = "1";
        } else {
            numerator_string = string.substring(starting_point, fraction_bar);
            denominator_string = string.substring(fraction_bar + 1);
        }
        try {
            T numerator = integer_type.getConstructor(String.class).newInstance(numerator_string);
            final T denominator = integer_type.getConstructor(String.class).newInstance(denominator_string);
            if (denominator.is_zero()) {
                throw new IllegalArgumentException("Denominator cannot be zero.");
            }
            final T gcd = numerator.gcd(denominator);
            if (negative) {
                numerator = numerator.negate();
            }
            this.numerator = numerator.divide_by(gcd);
            this.denominator = denominator.divide_by(gcd);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate a rational from the given integer type!", e);
        }
    }

    @Override
    public String toString() {
        return this.numerator + "/" + this.denominator;
    }

    @Override
    public int compareTo(final Rational<T> other) {
        final Rational<T> difference = minus(other);
        if (difference.is_zero()) {
            return 0;
        } else if (difference.is_positive()) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Rational<?> other = (Rational<?>) o;
        return numerator.equals(other.numerator) && denominator == other.denominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator.hashCode(), denominator.hashCode());
    }

    public boolean is_zero() {
        return numerator.is_zero();
    }

    public boolean is_positive() {
        return numerator.is_positive();
    }

    public boolean is_negative() {
        return numerator.is_negative();
    }

    public Rational<T> negate() {
        return new Rational<>(numerator.negate(), denominator);
    }

    public Rational<T> reciprocal() {
        if (is_zero()) {
            throw new ArithmeticException("Cannot get the reciprocal of zero!");
        }
        return new Rational<>(denominator, numerator);
    }

    public Rational<T> plus(final Rational<T> other) {
        final T denominator = this.denominator.multiply(other.denominator);
        final T numerator = this.numerator.multiply(other.denominator).plus(other.numerator.multiply(this.denominator));
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> minus(final Rational<T> other) {
        final T denominator = this.denominator.multiply(other.denominator);
        final T numerator = this.numerator.multiply(other.denominator).minus(other.numerator.multiply(this.denominator));
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> multiply(final Rational<T> other) {
        final T denominator = this.denominator.multiply(other.denominator);
        final T numerator = this.numerator.multiply(other.numerator);
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> divide_by(final Rational<T> other) {
        final T denominator = this.denominator.multiply(other.numerator);
        final T numerator = this.numerator.multiply(other.denominator);
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> pow(final int exponent) {
        if (exponent < 0) {
            if (Integer.MIN_VALUE == exponent) {
                throw new IllegalArgumentException("Exponent cannot be the minimum value of int.");
            }
            return reciprocal().pow(-exponent);
        } else if (0 == exponent) {
            return get_one();
        } else if (1 == exponent) {
            return this;
        }
        Rational<T> result = get_one();
        Rational<T> base = this;
        int power = exponent;
        while (true) {
            if (1 == (power & 1)) {
                result = result.multiply(base);
            }
            power >>= 1;
            if (power > 0) {
                base = base.multiply(base);
            } else {
                break;
            }
        }
        return result;
    }

    public static void main(final String[] args) {
        final Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            final String a_str = input.nextLine().trim();
            final String b_str = input.nextLine().trim();
            final Rational<TightInteger> a = new Rational<>(a_str, TightInteger.class);
            final Rational<TightInteger> b = new Rational<>(b_str, TightInteger.class);
            System.out.println(a);
            System.out.println(b);
            System.out.println(a.compareTo(b));
            System.out.println(a.plus(b));
            System.out.println(a.minus(b));
            System.out.println(a.multiply(b));
            System.out.println(a.divide_by(b));
            System.out.println(a.pow(2));
        }
    }
}