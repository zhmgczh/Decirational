import java.util.Objects;
import java.util.Arrays;
import java.util.Scanner;

public final class TightInteger implements CustomInteger<TightInteger> {
    private final boolean negative;
    private final int[] integer;
    public static final TightInteger ZERO = new TightInteger();
    public static final TightInteger ONE = new TightInteger(1);
    private static final TightInteger[] numbers = new TightInteger[]{ZERO, ONE};

    public static TightInteger get_digit(int index) {
        if (index < 0 || index > 1) {
            throw new IllegalArgumentException("The index must be either 0 or 1.");
        }
        return numbers[index];
    }

    public TightInteger(final TightInteger tight_integer) {
        negative = tight_integer.negative;
        integer = tight_integer.integer;
    }

    private TightInteger() {
        negative = false;
        integer = new int[1];
    }

    public TightInteger(final int[] integer, final boolean negative) {
        if (null == integer) {
            throw new IllegalArgumentException("Input int array cannot be null.");
        }
        if (0 == integer.length) {
            throw new IllegalArgumentException("Input int array cannot be empty.");
        }
        int[] new_integer = Arithmetic.optimize(integer);
        if (new_integer == integer) {
            new_integer = Arrays.copyOf(new_integer, new_integer.length);
        }
        this.integer = new_integer;
        this.negative = Arithmetic.optimize(negative, this.integer);
    }

    private TightInteger(final int[] integer, final boolean negative, boolean unsafe) {
        this.integer = Arithmetic.optimize(integer);
        this.negative = Arithmetic.optimize(negative, this.integer);
    }

    public TightInteger(final byte number) {
        this((int) number);
    }

    public TightInteger(final short number) {
        this((int) number);
    }

    public TightInteger(final int number) {
        final long b = Arithmetic.reverse_negative(number);
        final int[] integer = new int[1];
        integer[0] = (int) b;
        this.integer = integer;
        negative = b != number;
    }

    public TightInteger(final long number) {
        this(Long.toString(number));
    }

    public TightInteger(final DecimalInteger decimal_integer) {
        this(decimal_integer.to_tight_integer());
    }

    public TightInteger(final String number) {
        this(new DecimalInteger(number));
    }

    public String toString() {
        return to_decimal_integer().toString();
    }

    public DecimalInteger to_decimal_integer() {
        int decimal_length = (int) (integer.length * Arithmetic.tight_to_decimal_length_ratio + 1) + 1;
        byte[] digits = new byte[decimal_length];
        Arithmetic.convert_tight_to_decimal(digits, integer);
        return new DecimalInteger(digits, negative);
    }

    @Override
    public TightInteger get_zero() {
        return ZERO;
    }

    @Override
    public TightInteger get_one() {
        return ONE;
    }

    @Override
    public boolean is_zero() {
        return 1 == integer.length && 0 == integer[0];
    }

    @Override
    public boolean is_unit_abs() {
        return 1 == integer.length && 1 == integer[0];
    }

    @Override
    public boolean is_positive() {
        return !negative && !is_zero();
    }

    @Override
    public boolean is_negative() {
        return negative;
    }

    @Override
    public TightInteger negate() {
        return new TightInteger(integer, !negative, true);
    }

    @Override
    public TightInteger abs() {
        return new TightInteger(integer, false, true);
    }

    @Override
    public int compareTo(final TightInteger other) {
        if (negative && !other.negative) {
            return -1;
        } else if (!negative && other.negative) {
            return 1;
        }
        if (integer.length < other.integer.length) {
            return negative ? 1 : -1;
        } else if (integer.length > other.integer.length) {
            return negative ? -1 : 1;
        }
        for (int i = 0; i < integer.length; ++i) {
            if ((integer[i] & 0xffffffffL) < (other.integer[i] & 0xffffffffL)) {
                return negative ? 1 : -1;
            } else if ((integer[i] & 0xffffffffL) > (other.integer[i] & 0xffffffffL)) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TightInteger other = (TightInteger) o;
        return negative == other.negative && Arrays.equals(integer, other.integer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(negative, Arrays.hashCode(integer));
    }

    private TightInteger plus_raw(TightInteger other) {
        final int[] integer = Arithmetic.expand(this.integer, Math.max(this.integer.length, other.integer.length) + 1);
        Arithmetic.add(integer, other.integer);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger plus(final TightInteger other) {
        if (is_zero()) {
            return other;
        }
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return minus_raw(other.negate());
        }
        return plus_raw(other);
    }

    private TightInteger minus_raw(TightInteger other) {
        final int compare_to = abs().compareTo(other.abs());
        final TightInteger a, b;
        boolean negative = this.negative;
        if (0 == compare_to) {
            return ZERO;
        } else if (0 < compare_to) {
            a = this;
            b = other;
        } else {
            a = other;
            b = this;
            negative = !negative;
        }
        final int[] integer = Arrays.copyOf(a.integer, a.integer.length);
        Arithmetic.subtract(integer, b.integer);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger minus(final TightInteger other) {
        if (is_zero()) {
            return other.negate();
        }
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return plus_raw(other.negate());
        }
        return minus_raw(other);
    }

    @Override
    public TightInteger multiply(final TightInteger other) {
        if (is_zero() || other.is_zero()) {
            return ZERO;
        }
        if (is_unit_abs()) {
            if (is_positive()) {
                return other;
            } else {
                return other.negate();
            }
        }
        if (other.is_unit_abs()) {
            if (other.is_positive()) {
                return this;
            } else {
                return negate();
            }
        }
        final int[] integer = new int[this.integer.length + other.integer.length];
        Arithmetic.multiply(integer, this.integer, other.integer);
        return new TightInteger(integer, negative != other.negative, true);
    }

    public TightInteger multiply_4294967296(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        } else if (0 == times) {
            return this;
        } else if (is_zero()) {
            return ZERO;
        }
        final int[] integer = new int[this.integer.length + times];
        System.arraycopy(this.integer, 0, integer, 0, this.integer.length);
        return new TightInteger(integer, negative, true);
    }

    public TightInteger multiply_4294967296() {
        return multiply_4294967296(1);
    }

    public TightInteger divide_by_4294967296(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (0 == times) {
            return this;
        } else if (is_zero() || times >= this.integer.length) {
            return ZERO;
        }
        final int[] integer = new int[this.integer.length - times];
        System.arraycopy(this.integer, 0, integer, 0, integer.length);
        return new TightInteger(integer, negative, true);
    }

    public TightInteger divide_by_4294967296() {
        return divide_by_4294967296(1);
    }

    @Override
    public TightInteger divide_by(final TightInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        if (is_zero()) {
            return ZERO;
        }
        if (other.is_unit_abs()) {
            if (other.is_positive()) {
                return this;
            } else {
                return negate();
            }
        }
        final int[] integer = new int[this.integer.length];
        Arithmetic.divide(integer, this.integer, other.integer);
        return new TightInteger(integer, negative != other.negative, true);
    }

    @Override
    public TightInteger modulo(final TightInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        if (is_zero() || other.is_unit_abs()) {
            return ZERO;
        }
        final int[] integer = new int[other.integer.length];
        Arithmetic.modulo(integer, this.integer, other.integer);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger[] divide_by_and_modulo(final TightInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        if (is_zero()) {
            return new TightInteger[]{ZERO, ZERO};
        }
        if (other.is_unit_abs()) {
            if (other.is_positive()) {
                return new TightInteger[]{this, ZERO};
            } else {
                return new TightInteger[]{negate(), ZERO};
            }
        }
        final int[] quotient_integer = new int[integer.length];
        final int[] remainder_integer = new int[other.integer.length];
        Arithmetic.divide_and_modulo(quotient_integer, remainder_integer, integer, other.integer);
        final TightInteger quotient = new TightInteger(quotient_integer, negative != other.negative, true);
        final TightInteger remainder = new TightInteger(remainder_integer, negative, true);
        return new TightInteger[]{quotient, remainder};
    }

    @Override
    public TightInteger gcd(final TightInteger other) {
        if (is_zero() || other.is_zero() || is_unit_abs() || other.is_unit_abs()) {
            return ONE;
        }
        final int[] integer = new int[Math.min(this.integer.length, other.integer.length)];
        Arithmetic.gcd(integer, this.integer, other.integer);
        return new TightInteger(integer, false, true);
    }

    @Override
    public TightInteger lcm(final TightInteger other) {
        return multiply(other).divide_by(gcd(other));
    }

    @Override
    public TightInteger pow(final int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("Exponent cannot be negative!");
        } else if (0 == exponent) {
            return ONE;
        } else if (1 == exponent) {
            return this;
        }
        TightInteger result = ONE;
        TightInteger base = this;
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
            final TightInteger a = new TightInteger(a_str);
            final TightInteger b = new TightInteger(b_str);
            System.out.println(a);
            System.out.println(b);
            System.out.println(a.compareTo(b));
            System.out.println(a.plus(b));
            System.out.println(a.minus(b));
            System.out.println(a.multiply(b));
            System.out.println(a.divide_by(b));
            System.out.println(a.modulo(b));
            System.out.println(a.gcd(b));
            System.out.println(a.lcm(a));
            System.out.println(a.pow(2));
        }
    }
}