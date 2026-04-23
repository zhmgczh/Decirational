import java.util.Arrays;
import java.util.Scanner;

public class LargeInteger implements Comparable<LargeInteger> {
    private final boolean negative;
    private final int[] integer;
    public static final LargeInteger ZERO = new LargeInteger();

    public LargeInteger(final LargeInteger large_integer) {
        negative = large_integer.negative;
        integer = Arrays.copyOf(large_integer.integer, large_integer.integer.length);
    }

    private LargeInteger() {
        negative = false;
        integer = new int[1];
    }

    public LargeInteger(final int[] integer, final boolean negative) {
        this.integer = Arithmetic.optimize(integer);
        this.negative = Arithmetic.optimize(negative, this.integer);
    }

    public LargeInteger(final byte number) {
        this((int) number);
    }

    public LargeInteger(final short number) {
        this((int) number);
    }

    public LargeInteger(final int number) {
        final long b = Arithmetic.reverse_negative(number);
        final int[] integer = new int[1];
        integer[0] = number;
        this.integer = integer;
        this.negative = b != number;
    }

    public LargeInteger(final long number) {
        this(Long.toString(number));
    }

    public LargeInteger(final DecimalInteger decimal_integer) {
        this(decimal_integer.to_large_integer());
    }

    public LargeInteger(final String number) {
        this(new DecimalInteger(number));
    }

    public String toString() {
        return to_decimal_integer().toString();
    }

    public DecimalInteger to_decimal_integer() {
        int decimal_length = (int) (integer.length * Arithmetic.tight_to_decimal_length_ratio + 1);
        byte[] digits = new byte[decimal_length];
        Arithmetic.convert_tight_to_decimal(digits, integer);
        return new DecimalInteger(digits, negative);
    }

    public boolean is_zero() {
        return 1 == integer.length && 0 == integer[0];
    }

    public boolean is_positive() {
        return !negative && !is_zero();
    }

    public boolean is_negative() {
        return negative;
    }

    public LargeInteger negate() {
        return new LargeInteger(integer, !negative);
    }

    public LargeInteger abs() {
        return new LargeInteger(integer, false);
    }

    @Override
    public int compareTo(final LargeInteger other) {
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

    public LargeInteger plus(final LargeInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return minus(other.negate());
        }
        final int[] integer = Arithmetic.expand(this.integer, Math.max(this.integer.length, other.integer.length) + 1);
        Arithmetic.add(integer, other.integer);
        return new LargeInteger(integer, this.negative);
    }

    private static void pass_borrow(final long[] integer, final int i) {
        if (integer[i] < 0) {
            integer[i] += 1 << 8;
            --integer[i - 1];
        }
    }

    public LargeInteger minus(final LargeInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return plus(other.negate());
        }
        final int compare_to = abs().compareTo(other.abs());
        final LargeInteger a, b;
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
        return new LargeInteger(integer, negative);
    }

    public LargeInteger multiply(final LargeInteger other) {
        final int[] integer = new int[this.integer.length + other.integer.length];
        Arithmetic.multiply(integer, this.integer, other.integer);
        return new LargeInteger(integer, this.negative != other.negative);
    }

    public LargeInteger multiply_4294967296(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        }
        final int[] integer = new int[this.integer.length + times];
        System.arraycopy(this.integer, 0, integer, 0, this.integer.length);
        return new LargeInteger(integer, this.negative);
    }

    public LargeInteger multiply_4294967296() {
        return multiply_4294967296(1);
    }

    public LargeInteger divide_by_4294967296(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (times >= this.integer.length) {
            return ZERO;
        }
        final int[] integer = new int[this.integer.length - times];
        System.arraycopy(this.integer, 0, integer, 0, integer.length);
        return new LargeInteger(integer, this.negative);
    }

    public LargeInteger divide_by_4294967296() {
        return divide_by_4294967296(1);
    }

    public LargeInteger divide_by(final LargeInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        final int[] integer = new int[this.integer.length];
        Arithmetic.divide(integer, this.integer, other.integer);
        return new LargeInteger(integer, this.negative != other.negative);
    }

    public LargeInteger modulo(final LargeInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        final int[] integer = new int[other.integer.length];
        Arithmetic.modulo(integer, this.integer, other.integer);
        return new LargeInteger(integer, this.negative != other.negative);
    }

    public LargeInteger[] divide_by_and_modulo(final LargeInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        final int[] quotient_integer = new int[this.integer.length];
        final int[] remainder_integer = new int[other.integer.length];
        Arithmetic.divide_and_modulo(quotient_integer, remainder_integer, this.integer, other.integer);
        final LargeInteger quotient = new LargeInteger(quotient_integer, this.negative != other.negative);
        final LargeInteger remainder = new LargeInteger(remainder_integer, this.negative != other.negative);
        return new LargeInteger[]{quotient, remainder};
    }

    public static void main(final String[] args) {
        final Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            final String a_str = input.nextLine().trim();
            final String b_str = input.nextLine().trim();
            final LargeInteger a = new LargeInteger(a_str);
            final LargeInteger b = new LargeInteger(b_str);
            System.out.println(a);
            System.out.println(b);
            System.out.println(a.compareTo(b));
            System.out.println(a.plus(b));
            System.out.println(a.minus(b));
            System.out.println(a.multiply(b));
            System.out.println(a.divide_by(b));
            System.out.println(a.modulo(b));
        }
    }
}