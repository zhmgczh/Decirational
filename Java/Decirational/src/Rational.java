import java.util.HashMap;
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
        final T one = denominator.pow(0);
        return new Rational<>(one, one, true, true, true);
    }

    private T get_five() {
        final T one = denominator.pow(0);
        final T two = one.plus(one);
        final T four = two.plus(two);
        return four.plus(one);
    }

    private T get_ten() {
        final T one = denominator.pow(0);
        final T two = one.plus(one);
        final T four = two.plus(two);
        return four.multiply(two).plus(two);
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

    private Rational(T numerator, T denominator, final boolean input_unsafe) {
        final T gcd = numerator.gcd(denominator);
        if (denominator.is_negative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator.divide_by(gcd);
        this.denominator = denominator.divide_by(gcd);
    }

    private Rational(T numerator, T denominator, final boolean input_unsafe, final boolean gcd_unsafe) {
        if (denominator.is_negative()) {
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
            throw new NullPointerException("Integer cannot be null.");
        }
        this.numerator = integer;
        this.denominator = integer.pow(0);
    }

    public Rational(final T integer, final boolean input_unsafe) {
        this.numerator = integer;
        this.denominator = integer.pow(0);
    }

    public Rational(final Rational<T> rational) {
        if (null == rational) {
            throw new NullPointerException("Rational cannot be null.");
        }
        this.numerator = rational.numerator;
        this.denominator = rational.denominator;
    }

    public Rational(String string, final Class<T> integer_type) {
        if (null == string) {
            throw new NumberFormatException("Input is null.");
        }
        string = string.replaceAll("\\s", "");
        if (string.isEmpty()) {
            throw new NumberFormatException("Input is empty.");
        }
        boolean negative = false;
        int starting_point = 0;
        if (Arithmetic.is_minus(string.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (Arithmetic.is_plus(string.charAt(0))) {
            starting_point = 1;
        }
        if (starting_point >= string.length() || !Arithmetic.is_digit(string.charAt(starting_point))) {
            throw new NumberFormatException("The rational string does not have the right format!!");
        }
        int fraction_bar = -1;
        int decimal_point = -1;
        int cyclic_begin = -1;
        int cyclic_end = -1;
        for (int i = starting_point + 1; i < string.length(); ++i) {
            if (Arithmetic.is_fraction_bar(string.charAt(i))) {
                if (-1 != fraction_bar || -1 != decimal_point) {
                    throw new NumberFormatException("The rational string does not have the right format!!");
                }
                fraction_bar = i;
            } else if (Arithmetic.is_decimal_point(string.charAt(i))) {
                if (-1 != decimal_point || -1 != fraction_bar || i == string.length() - 1) {
                    throw new NumberFormatException("The rational string does not have the right format!!");
                }
                decimal_point = i;
            } else if (Arithmetic.is_cyclic_begin(string.charAt(i))) {
                if (-1 != cyclic_begin || -1 == decimal_point) {
                    throw new NumberFormatException("The rational string does not have the right format!!");
                }
                cyclic_begin = i;
            } else if (Arithmetic.is_cyclic_end(string.charAt(i))) {
                if (-1 == cyclic_begin || -1 != cyclic_end || i != string.length() - 1 || 1 == i - cyclic_begin) {
                    throw new NumberFormatException("The rational string does not have the right format!!");
                }
                cyclic_end = i;
            } else if (!Arithmetic.is_digit(string.charAt(i))) {
                throw new NumberFormatException("The rational string does not have the right format!!");
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
                throw new NumberFormatException("The rational string does not have the right format!!");
            }
            T numerator;
            final T denominator;
            try {
                numerator = integer_type.getConstructor(String.class).newInstance(numerator_string);
                denominator = integer_type.getConstructor(String.class).newInstance(denominator_string);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate a rational from the given integer type!", e);
            }
            if (denominator.is_zero()) {
                throw new IllegalArgumentException("Denominator cannot be zero.");
            }
            final T gcd = numerator.gcd(denominator);
            if (negative) {
                numerator = numerator.negate();
            }
            this.numerator = numerator.divide_by(gcd);
            this.denominator = denominator.divide_by(gcd);
        } else if (-1 == cyclic_begin) {
            final String numerator_string = string.substring(starting_point, decimal_point) + string.substring(decimal_point + 1);
            final String denominator_string = "1" + "0".repeat(string.length() - decimal_point - 1);
            T numerator;
            final T denominator;
            try {
                numerator = integer_type.getConstructor(String.class).newInstance(numerator_string);
                denominator = integer_type.getConstructor(String.class).newInstance(denominator_string);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate a rational from the given integer type!", e);
            }
            final T gcd = numerator.gcd(denominator);
            if (negative) {
                numerator = numerator.negate();
            }
            this.numerator = numerator.divide_by(gcd);
            this.denominator = denominator.divide_by(gcd);
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
                finite_numerator = integer_type.getConstructor(String.class).newInstance(finite_numerator_string);
                finite_denominator = integer_type.getConstructor(String.class).newInstance(finite_denominator_string);
                cyclic_numerator = integer_type.getConstructor(String.class).newInstance(cyclic_numerator_string);
                cyclic_denominator = integer_type.getConstructor(String.class).newInstance(cyclic_denominator_string);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate a rational from the given integer type!", e);
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
            throw new NumberFormatException("The rational string does not have the right format!!");
        }
    }

    @Override
    public String toString() {
        if (denominator.is_one()) {
            return numerator.toString();
        }
        return to_fraction_string();
    }

    public String to_fraction_string() {
        return numerator + "/" + denominator;
    }

    public String to_decimal_string() {
        final T ten = get_ten();
        T[] integer_and_remainder = numerator.abs().divide_by_and_modulo(denominator);
        final T whole_integer = integer_and_remainder[0];
        T remainder = integer_and_remainder[1];
        final StringBuilder decimal = new StringBuilder();
        decimal.append(numerator.is_negative() ? '-' : "").append(whole_integer).append(remainder.is_zero() ? "" : '.');
        final HashMap<T, Integer> remainder_map = new HashMap<>();
        remainder_map.put(remainder, decimal.length());
        int starting_cyclic = -1;
        while (!remainder.is_zero() && -1 == starting_cyclic) {
            remainder = remainder.multiply(ten);
            integer_and_remainder = remainder.divide_by_and_modulo(denominator);
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

    public String to_truncate_decimal_string(int round_to) {
        final T ten = get_ten();
        T[] integer_and_remainder = numerator.abs().divide_by_and_modulo(denominator);
        final T whole_integer = integer_and_remainder[0];
        final String whole_integer_string = whole_integer.toString();
        if (whole_integer_string.length() <= -round_to) {
            return "0";
        } else if (round_to < 0) {
            final T shift_base = ten.pow(-round_to);
            final T result = whole_integer.divide_by(shift_base).multiply(shift_base);
            return (numerator.is_negative() ? '-' : "") + result.toString();
        } else if (0 == round_to) {
            return (numerator.is_negative() ? '-' : "") + whole_integer_string;
        }
        T remainder = integer_and_remainder[1];
        final StringBuilder decimal = new StringBuilder();
        decimal.append(numerator.is_negative() ? '-' : "").append(whole_integer_string).append(remainder.is_zero() ? "" : '.');
        int index = 0;
        while (index < round_to && !remainder.is_zero()) {
            remainder = remainder.multiply(ten);
            integer_and_remainder = remainder.divide_by_and_modulo(denominator);
            final T integer = integer_and_remainder[0];
            remainder = integer_and_remainder[1];
            decimal.append(integer.toString().charAt(0));
            ++index;
        }
        return decimal.toString();
    }

    public String to_round_decimal_string(int round_to) {
        final T one = denominator.pow(0);
        final Rational<T> five = new Rational<>(get_five(), one, true, true, true);
        final Rational<T> ten = new Rational<>(get_ten(), one, true, true, true);
        final Rational<T> shift_base = ten.pow(-round_to - 1);
        final Rational<T> delta = five.multiply(shift_base);
        final Rational<T> temp_rational = is_negative() ? minus(delta) : plus(delta);
        return temp_rational.to_truncate_decimal_string(round_to);
    }

    public String to_ceil_decimal_string(int round_to) {
        if (is_negative()) {
            return '-' + negate().to_floor_decimal_string(round_to);
        }
        final T one = denominator.pow(0);
        final Rational<T> ten = new Rational<>(get_ten(), one, true, true, true);
        if (multiply(ten.pow(round_to)).is_integer()) {
            return to_truncate_decimal_string(round_to);
        }
        final Rational<T> shift_base = ten.pow(-round_to);
        final Rational<T> temp_rational = plus(shift_base);
        return temp_rational.to_truncate_decimal_string(round_to);
    }

    public String to_floor_decimal_string(int round_to) {
        if (is_negative()) {
            return '-' + negate().to_ceil_decimal_string(round_to);
        }
        return to_truncate_decimal_string(round_to);
    }

    public boolean is_integer() {
        return denominator.is_one();
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
        return new Rational<>(numerator.negate(), denominator, true, true, true);
    }

    public Rational<T> abs() {
        return new Rational<>(numerator.abs(), denominator, true, true, true);
    }

    public Rational<T> reciprocal() {
        if (is_zero()) {
            throw new ArithmeticException("Cannot get the reciprocal of zero!");
        }
        return new Rational<>(denominator, numerator, true, true);
    }

    private Rational<T> reciprocal(boolean unsafe) {
        return new Rational<>(denominator, numerator, true, true);
    }

    public T get_numerator_abs() {
        return numerator.abs();
    }

    public T get_denominator_abs() {
        return denominator.abs();
    }

    @Override
    public int compareTo(final Rational<T> other) {
        final T gcd = denominator.gcd(other.denominator);
        return numerator.multiply(other.denominator.divide_by(gcd)).compareTo(other.numerator.multiply(denominator.divide_by(gcd)));
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
        final T multiplier_1 = other.denominator.divide_by(gcd);
        final T multiplier_2 = this.denominator.divide_by(gcd);
        final T numerator = this.numerator.multiply(multiplier_1).plus(other.numerator.multiply(multiplier_2));
        final T denominator = gcd.multiply(multiplier_1).multiply(multiplier_2);
        return new Rational<>(numerator, denominator, true);
    }

    public Rational<T> minus(final Rational<T> other) {
        final T gcd = this.denominator.gcd(other.denominator);
        final T multiplier_1 = other.denominator.divide_by(gcd);
        final T multiplier_2 = this.denominator.divide_by(gcd);
        final T numerator = this.numerator.multiply(multiplier_1).minus(other.numerator.multiply(multiplier_2));
        final T denominator = gcd.multiply(multiplier_1).multiply(multiplier_2);
        return new Rational<>(numerator, denominator, true);
    }

    public Rational<T> multiply(final Rational<T> other) {
        final T gcd_1 = this.denominator.gcd(other.numerator);
        final T gcd_2 = other.denominator.gcd(this.numerator);
        final T denominator = this.denominator.divide_by(gcd_1).multiply(other.denominator.divide_by(gcd_2));
        final T numerator = this.numerator.divide_by(gcd_2).multiply(other.numerator.divide_by(gcd_1));
        return new Rational<>(numerator, denominator, true, true, true);
    }

    public Rational<T> divide_by(final Rational<T> other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        return multiply(other.reciprocal(true));
    }

    public Rational<T> pow(final int exponent) {
        if (exponent < 0) {
            if (Integer.MIN_VALUE == exponent) {
                return reciprocal(true).pow(Integer.MAX_VALUE).multiply(reciprocal(true));
            }
            if (is_zero()) {
                throw new ArithmeticException("Exponent cannot be negative for zero!");
            }
            return reciprocal(true).pow(-exponent);
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
            System.out.println(a.to_decimal_string());
            System.out.println(b.to_decimal_string());
            System.out.println(a.compareTo(b));
            System.out.println(a.plus(b));
            System.out.println(a.minus(b));
            System.out.println(a.multiply(b));
            System.out.println(a.divide_by(b));
            System.out.println(a.pow(2));
        }
    }
}