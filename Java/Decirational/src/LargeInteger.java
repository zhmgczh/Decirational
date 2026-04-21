import java.util.Arrays;

public class LargeInteger implements Comparable<LargeInteger> {
    private final boolean negative;
    private final int[] integer;
    public static final LargeInteger ZERO = new LargeInteger();
    public static final LargeInteger[] numbers;

    static {
        numbers = new LargeInteger[1 << 8];
        for (int i = 0; i < 1 << 8; ++i) {
            numbers[i] = new LargeInteger(i);
        }
    }

    private static long[] ints_to_longs(int[] ints) {
        long[] longs = new long[ints.length];
        for (int i = 0; i < longs.length; ++i) {
            longs[i] = ints[i] & 0xffffffffL;
        }
        return longs;
    }

    private static int[] longs_to_ints(long[] longs) {
        int[] ints = new int[longs.length];
        for (int i = 0; i < ints.length; ++i) {
            ints[i] = (int) longs[i];
        }
        return ints;
    }

    public LargeInteger(LargeInteger large_integer) {
        negative = large_integer.negative;
        integer = Arrays.copyOf(large_integer.integer, large_integer.integer.length);
    }

    private LargeInteger() {
        negative = false;
        integer = new int[1];
    }

    private LargeInteger(int[] integer, boolean negative) {
        this.integer = Arithmetic.optimize(integer);
        this.negative = Arithmetic.optimize(negative, this.integer);
    }

    public LargeInteger(byte number) {
        long b = Arithmetic.reverse_negative(number);
        int[] integer = new int[1];
        integer[0] = number;
        this.integer = integer;
        this.negative = b != number;
    }

    public LargeInteger(short number) {
        long b = Arithmetic.reverse_negative(number);
        int[] integer = new int[1];
        integer[0] = number;
        this.integer = integer;
        this.negative = b != number;
    }

    public LargeInteger(int number) {
        long b = Arithmetic.reverse_negative(number);
        int[] integer = new int[1];
        integer[0] = number;
        this.integer = integer;
        this.negative = b != number;
    }

    public LargeInteger(long number) {
        long b = Arithmetic.reverse_negative(number);
        int[] integer = new int[2];
        integer[0] = (int) (number & 0xffffffffL);
        integer[1] = (int) (number >>> 32);
        this.integer = Arithmetic.optimize(integer);
        this.negative = b != number;
    }

    public LargeInteger(String number) {
        DecimalInteger decimal_integer = new DecimalInteger(number);
        this(null, false);
    }

    public String toString() {
        return null;
    }

    public boolean is_zero() {
        return 1 == integer.length && integer[0] == 0;
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
    public int compareTo(LargeInteger other) {
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
            if (integer[i] < other.integer[i]) {
                return negative ? 1 : -1;
            } else if (integer[i] > other.integer[i]) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }

    public LargeInteger plus(LargeInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return minus(other.negate());
        }
        int[] integer = Arithmetic.expand(this.integer, Math.max(this.integer.length, other.integer.length) + 1);
        Arithmetic.add(integer, other.integer);
        return new LargeInteger(integer, this.negative);
    }

    private static void pass_borrow(long[] integer, int i) {
        if (integer[i] < 0) {
            integer[i] += 1 << 8;
            --integer[i - 1];
        }
    }

    public LargeInteger minus(LargeInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return plus(other.negate());
        }
        int compare_to = abs().compareTo(other.abs());
        LargeInteger a, b;
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
        int[] integer = Arrays.copyOf(a.integer, a.integer.length);
        Arithmetic.subtract(integer, b.integer);
        return new LargeInteger(integer, negative);
    }

    public LargeInteger multiply(LargeInteger other) {
        int[] integer = new int[this.integer.length + other.integer.length];
        Arithmetic.multiply(integer, this.integer, other.integer);
        return new LargeInteger(integer, this.negative != other.negative);
    }

    private int upper_multiplier(LargeInteger dividend, LargeInteger divisor) {
        int left = 0;
        int right = (1 << 8) - 1;
        while (left < right) {
            int mid = (left + right + 1) >> 1;
            LargeInteger multiplier = numbers[mid];
            if (divisor.multiply(multiplier).compareTo(dividend) <= 0) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }
        return left;
    }

    public LargeInteger multiply_4294967296(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        }
        int[] integer = new int[this.integer.length + times];
        System.arraycopy(this.integer, 0, integer, 0, this.integer.length);
        return new LargeInteger(integer, this.negative);
    }

    public LargeInteger multiply_4294967296() {
        return multiply_4294967296(1);
    }

    public LargeInteger divide_by_4294967296(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (times >= this.integer.length) {
            return ZERO;
        }
        int[] integer = new int[this.integer.length - times];
        System.arraycopy(this.integer, 0, integer, 0, integer.length);
        return new LargeInteger(integer, this.negative);
    }

    public LargeInteger divide_by_4294967296() {
        return divide_by_4294967296(1);
    }

    public LargeInteger divide_by(LargeInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        int[] integer = new int[this.integer.length];
        LargeInteger remaining_dividend = abs();
        LargeInteger divisor = other.abs().multiply_4294967296(integer.length);
        for (int i = 0; i < integer.length; ++i) {
            divisor = divisor.divide_by_4294967296();
            int multiplier = upper_multiplier(remaining_dividend, divisor);
            integer[i] = multiplier;
            remaining_dividend = remaining_dividend.minus(divisor.multiply(numbers[multiplier]));
        }
        return new LargeInteger(integer, this.negative != other.negative);
    }

    public LargeInteger mod(LargeInteger other) {
        return minus(divide_by(other).multiply(other));
    }
}