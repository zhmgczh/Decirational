package decirational;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

public final class Rational<T extends CustomInteger<T>> implements Comparable<Rational<T>> {
    private final T numerator;
    private final T denominator;

    public T getNumerator() {
        return numerator;
    }

    public T getDenominator() {
        return denominator;
    }

    private Rational<T> getOne() {
        final T one = denominator.pow(0);
        return new Rational<>(one, one, true, true, true);
    }

    private T getFive() {
        final T one = denominator.pow(0);
        final T two = one.plus(one);
        final T four = two.plus(two);
        return four.plus(one);
    }

    private T getTen() {
        final T one = denominator.pow(0);
        final T two = one.plus(one);
        final T four = two.plus(two);
        return four.multiply(two).plus(two);
    }

    public Rational(T numerator, T denominator) {
        if (null == numerator || null == denominator) {
            throw new NullPointerException("numerator and denominator cannot be null");
        }
        if (denominator.isZero()) {
            throw new IllegalArgumentException("denominator cannot be zero");
        }
        final T gcd = numerator.gcd(denominator);
        if (denominator.isNegative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator.divideBy(gcd);
        this.denominator = denominator.divideBy(gcd);
    }

    private Rational(T numerator, T denominator, final boolean input_unsafe) {
        final T gcd = numerator.gcd(denominator);
        if (denominator.isNegative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator.divideBy(gcd);
        this.denominator = denominator.divideBy(gcd);
    }

    private Rational(T numerator, T denominator, final boolean input_unsafe, final boolean gcd_unsafe) {
        if (denominator.isNegative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }

    private Rational(final T numerator, final T denominator, final boolean input_unsafe, final boolean gcd_unsafe, final boolean sign_unsafe) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Rational(final T integer) {
        if (null == integer) {
            throw new NullPointerException("integer cannot be null");
        }
        this.numerator = integer;
        this.denominator = integer.pow(0);
    }

    private Rational(final T integer, final boolean input_unsafe) {
        this.numerator = integer;
        this.denominator = integer.pow(0);
    }

    public Rational(final Rational<T> rational) {
        if (null == rational) {
            throw new NullPointerException("rational cannot be null");
        }
        this.numerator = rational.numerator;
        this.denominator = rational.denominator;
    }

    public Rational(String string, final Function<String, T> from_string) {
        if (null == string) {
            throw new IllegalArgumentException("input is null");
        }
        if (null == from_string) {
            throw new IllegalArgumentException("integer constructor is null");
        }
        string = string.replaceAll("\\s", "");
        if (string.isEmpty()) {
            throw new NumberFormatException("input is empty");
        }
        boolean negative = false;
        int starting_point = 0;
        if (Arithmetic.isMinus(string.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (Arithmetic.isPlus(string.charAt(0))) {
            starting_point = 1;
        }
        if (starting_point >= string.length() || !Arithmetic.isDigit(string.charAt(starting_point))) {
            throw new NumberFormatException("the rational string does not have the right format");
        }
        int fraction_bar = -1;
        int decimal_point = -1;
        int cyclic_begin = -1;
        int cyclic_end = -1;
        for (int i = starting_point + 1; i < string.length(); ++i) {
            if (Arithmetic.isFractionBar(string.charAt(i))) {
                if (-1 != fraction_bar || -1 != decimal_point) {
                    throw new NumberFormatException("the rational string does not have the right format");
                }
                fraction_bar = i;
            } else if (Arithmetic.isDecimalPoint(string.charAt(i))) {
                if (-1 != decimal_point || -1 != fraction_bar || i == string.length() - 1) {
                    throw new NumberFormatException("the rational string does not have the right format");
                }
                decimal_point = i;
            } else if (Arithmetic.isCyclicBegin(string.charAt(i))) {
                if (-1 != cyclic_begin || -1 == decimal_point) {
                    throw new NumberFormatException("the rational string does not have the right format");
                }
                cyclic_begin = i;
            } else if (Arithmetic.isCyclicEnd(string.charAt(i))) {
                if (-1 == cyclic_begin || -1 != cyclic_end || i != string.length() - 1 || 1 == i - cyclic_begin) {
                    throw new NumberFormatException("the rational string does not have the right format");
                }
                cyclic_end = i;
            } else if (!Arithmetic.isDigit(string.charAt(i))) {
                throw new NumberFormatException("the rational string does not have the right format");
            }
        }
        if (-1 == decimal_point) {
            final String numerator_string, denominator_string;
            if (-1 == fraction_bar) {
                numerator_string = string.substring(starting_point);
                denominator_string = "1";
            } else if (fraction_bar != string.length() - 1) {
                numerator_string = string.substring(starting_point, fraction_bar);
                denominator_string = string.substring(fraction_bar + 1);
            } else {
                throw new NumberFormatException("the rational string does not have the right format");
            }
            T numerator;
            final T denominator;
            try {
                numerator = from_string.apply(numerator_string);
                denominator = from_string.apply(denominator_string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("cannot instantiate a rational from the given integer type", e);
            }
            if (denominator.isZero()) {
                throw new IllegalArgumentException("denominator cannot be zero");
            }
            final T gcd = numerator.gcd(denominator);
            if (negative) {
                numerator = numerator.negate();
            }
            this.numerator = numerator.divideBy(gcd);
            this.denominator = denominator.divideBy(gcd);
        } else if (-1 == cyclic_begin) {
            final String numerator_string = string.substring(starting_point, decimal_point) + string.substring(decimal_point + 1);
            final String denominator_string = "1" + "0".repeat(string.length() - decimal_point - 1);
            T numerator;
            final T denominator;
            try {
                numerator = from_string.apply(numerator_string);
                denominator = from_string.apply(denominator_string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("cannot instantiate a rational from the given integer type", e);
            }
            final T gcd = numerator.gcd(denominator);
            if (negative) {
                numerator = numerator.negate();
            }
            this.numerator = numerator.divideBy(gcd);
            this.denominator = denominator.divideBy(gcd);
        } else if (-1 != cyclic_end) {
            final String finite_numerator_string = string.substring(starting_point, decimal_point) + string.substring(decimal_point + 1, cyclic_begin);
            final String finite_denominator_string = "1" + "0".repeat(cyclic_begin - decimal_point - 1);
            final String cyclic_numerator_string = string.substring(cyclic_begin + 1, cyclic_end);
            final String cyclic_denominator_string = "9".repeat(cyclic_end - cyclic_begin - 1) + "0".repeat(cyclic_begin - decimal_point - 1);
            final T finite_numerator;
            final T finite_denominator;
            final T cyclic_numerator;
            final T cyclic_denominator;
            try {
                finite_numerator = from_string.apply(finite_numerator_string);
                finite_denominator = from_string.apply(finite_denominator_string);
                cyclic_numerator = from_string.apply(cyclic_numerator_string);
                cyclic_denominator = from_string.apply(cyclic_denominator_string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("cannot instantiate a rational from the given integer type", e);
            }
            final Rational<T> finite = new Rational<>(finite_numerator, finite_denominator, true);
            final Rational<T> cyclic = new Rational<>(cyclic_numerator, cyclic_denominator, true);
            Rational<T> result = finite.plus(cyclic);
            if (negative) {
                result = result.negate();
            }
            this.numerator = result.numerator;
            this.denominator = result.denominator;
        } else {
            throw new NumberFormatException("the rational string does not have the right format");
        }
    }

    @Override
    public String toString() {
        if (denominator.isOne()) {
            return numerator.toString();
        }
        return toFractionString();
    }

    public String toFractionString() {
        return numerator + "/" + denominator;
    }

    public String toMixedString() {
        if (denominator.isOne()) {
            return numerator.toString();
        }
        final T[] whole_and_remainder = numerator.abs().divideByAndModulo(denominator);
        final T whole = whole_and_remainder[0];
        final T remainder = whole_and_remainder[1];
        final StringBuilder sb = new StringBuilder();
        if (numerator.isNegative()) {
            sb.append('-');
        }
        if (!whole.isZero()) {
            sb.append(whole).append(' ');
        }
        sb.append(remainder).append('/').append(denominator);
        return sb.toString();
    }

    public String toDecimalString() {
        final T ten = getTen();
        T[] integer_and_remainder = numerator.abs().divideByAndModulo(denominator);
        final T whole_integer = integer_and_remainder[0];
        T remainder = integer_and_remainder[1];
        final StringBuilder decimal = new StringBuilder();
        decimal.append(numerator.isNegative() ? '-' : "").append(whole_integer).append(remainder.isZero() ? "" : '.');
        final HashMap<T, Integer> remainder_map = new HashMap<>();
        remainder_map.put(remainder, decimal.length());
        int starting_cyclic = -1;
        while (!remainder.isZero() && -1 == starting_cyclic) {
            remainder = remainder.multiply(ten);
            integer_and_remainder = remainder.divideByAndModulo(denominator);
            final T integer = integer_and_remainder[0];
            remainder = integer_and_remainder[1];
            decimal.append(integer.toString().charAt(0));
            if (remainder_map.containsKey(remainder)) {
                starting_cyclic = remainder_map.get(remainder);
            }
            remainder_map.put(remainder, decimal.length());
        }
        if (-1 != starting_cyclic) {
            decimal.insert(starting_cyclic, Arithmetic.cyclic_begin);
            decimal.append(Arithmetic.cyclic_end);
        }
        return decimal.toString();
    }

    public String toTruncateDecimalString(final int round_to) {
        final T ten = getTen();
        T[] integer_and_remainder = numerator.abs().divideByAndModulo(denominator);
        final T whole_integer = integer_and_remainder[0];
        final String whole_integer_string = whole_integer.toString();
        if (whole_integer_string.length() <= -round_to) {
            return (numerator.isNegative() ? '-' : "") + "0";
        } else if (round_to < 0) {
            if (Integer.MIN_VALUE == round_to) {
                throw new IllegalArgumentException("cannot round to the minimum representable precision");
            }
            final T shift_base = ten.pow(-round_to);
            final T result = whole_integer.divideBy(shift_base).multiply(shift_base);
            return (numerator.isNegative() ? '-' : "") + result.toString();
        } else if (0 == round_to) {
            return (numerator.isNegative() ? '-' : "") + whole_integer_string;
        }
        T remainder = integer_and_remainder[1];
        final StringBuilder decimal = new StringBuilder();
        decimal.append(numerator.isNegative() ? '-' : "").append(whole_integer_string).append(remainder.isZero() ? "" : '.');
        int index = 0;
        while (index < round_to && !remainder.isZero()) {
            remainder = remainder.multiply(ten);
            integer_and_remainder = remainder.divideByAndModulo(denominator);
            final T integer = integer_and_remainder[0];
            remainder = integer_and_remainder[1];
            decimal.append(integer.toString().charAt(0));
            ++index;
        }
        return decimal.toString();
    }

    public String toRoundDecimalString(final int round_to) {
        if (Integer.MIN_VALUE == round_to) {
            throw new IllegalArgumentException("cannot round to the minimum representable precision");
        }
        final Rational<T> five = new Rational<>(getFive(), true);
        final Rational<T> ten = new Rational<>(getTen(), true);
        final Rational<T> shift_base = ten.pow(-round_to - 1);
        final Rational<T> delta = five.multiply(shift_base);
        final Rational<T> temp_rational = isNegative() ? minus(delta) : plus(delta);
        return temp_rational.toTruncateDecimalString(round_to);
    }

    public String toCeilDecimalString(final int round_to) {
        if (isNegative()) {
            return '-' + negate().toFloorDecimalString(round_to);
        }
        if (Integer.MIN_VALUE == round_to) {
            throw new IllegalArgumentException("cannot round to the minimum representable precision");
        }
        final Rational<T> ten = new Rational<>(getTen(), true);
        if (multiply(ten.pow(round_to)).isInteger()) {
            return toTruncateDecimalString(round_to);
        }
        final Rational<T> shift_base = ten.pow(-round_to);
        final Rational<T> temp_rational = plus(shift_base);
        return temp_rational.toTruncateDecimalString(round_to);
    }

    public String toFloorDecimalString(final int round_to) {
        if (isNegative()) {
            return '-' + negate().toCeilDecimalString(round_to);
        }
        return toTruncateDecimalString(round_to);
    }

    public boolean isInteger() {
        return denominator.isOne();
    }

    public boolean isZero() {
        return numerator.isZero();
    }

    public boolean isPositive() {
        return numerator.isPositive();
    }

    public boolean isNegative() {
        return numerator.isNegative();
    }

    public Rational<T> negate() {
        return new Rational<>(numerator.negate(), denominator, true, true, true);
    }

    public Rational<T> abs() {
        return new Rational<>(numerator.abs(), denominator, true, true, true);
    }

    public Rational<T> reciprocal() {
        if (isZero()) {
            throw new ArithmeticException("cannot get the reciprocal of zero");
        }
        return new Rational<>(denominator, numerator, true, true);
    }

    private Rational<T> reciprocal(final boolean unsafe) {
        return new Rational<>(denominator, numerator, true, true);
    }

    public T getNumeratorAbs() {
        return numerator.abs();
    }

    public T getDenominatorAbs() {
        return denominator.abs();
    }

    @Override
    public int compareTo(final Rational<T> other) {
        final T gcd = denominator.gcd(other.denominator);
        return numerator.multiply(other.denominator.divideBy(gcd)).compareTo(other.numerator.multiply(denominator.divideBy(gcd)));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Rational<?> other = (Rational<?>) o;
        return numerator.equals(other.numerator) && denominator.equals(other.denominator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    public Rational<T> plus(final Rational<T> other) {
        final T gcd = this.denominator.gcd(other.denominator);
        final T multiplier_1 = other.denominator.divideBy(gcd);
        final T multiplier_2 = this.denominator.divideBy(gcd);
        final T numerator = this.numerator.multiply(multiplier_1).plus(other.numerator.multiply(multiplier_2));
        final T denominator = gcd.multiply(multiplier_1).multiply(multiplier_2);
        return new Rational<>(numerator, denominator, true);
    }

    public Rational<T> minus(final Rational<T> other) {
        final T gcd = this.denominator.gcd(other.denominator);
        final T multiplier_1 = other.denominator.divideBy(gcd);
        final T multiplier_2 = this.denominator.divideBy(gcd);
        final T numerator = this.numerator.multiply(multiplier_1).minus(other.numerator.multiply(multiplier_2));
        final T denominator = gcd.multiply(multiplier_1).multiply(multiplier_2);
        return new Rational<>(numerator, denominator, true);
    }

    public Rational<T> multiply(final Rational<T> other) {
        final T gcd_1 = this.denominator.gcd(other.numerator);
        final T gcd_2 = other.denominator.gcd(this.numerator);
        final T denominator = this.denominator.divideBy(gcd_1).multiply(other.denominator.divideBy(gcd_2));
        final T numerator = this.numerator.divideBy(gcd_2).multiply(other.numerator.divideBy(gcd_1));
        return new Rational<>(numerator, denominator, true, true, true);
    }

    public Rational<T> divideBy(final Rational<T> other) {
        if (other.isZero()) {
            throw new ArithmeticException("cannot divide by zero");
        }
        return multiply(other.reciprocal(true));
    }

    public Rational<T> pow(final int exponent) {
        if (exponent < 0) {
            if (isZero()) {
                throw new ArithmeticException("exponent cannot be negative for zero");
            }
            if (Integer.MIN_VALUE == exponent) {
                return reciprocal(true).pow(Integer.MAX_VALUE).multiply(reciprocal(true));
            }
            return reciprocal(true).pow(-exponent);
        } else if (0 == exponent) {
            return getOne();
        } else if (1 == exponent) {
            return this;
        }
        Rational<T> result = getOne();
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
}