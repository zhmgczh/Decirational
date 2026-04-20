import java.util.Arrays;

public class LargeInteger implements Comparable<LargeInteger> {
    private final boolean negative;
    private final byte[] integer;
    public static final LargeInteger ZERO = new LargeInteger();
    public static final LargeInteger[] numbers;

    static {
        numbers = new LargeInteger[1 << 8];
        for (short i = 0; i < 1 << 8; ++i) {
            numbers[i] = new LargeInteger(i);
        }
    }

    private static long reverse_negative(long number) {
        if (number < 0) {
            number = -number;
        }
        return number;
    }

    private static byte[] optimize(byte[] integer) {
        int cut_length = 0;
        for (int i = integer.length - 1; i > 0; --i) {
            if (0 != integer[i]) {
                break;
            }
            ++cut_length;
        }
        if (0 != cut_length) {
            integer = Arrays.copyOf(integer, integer.length - cut_length);
        }
        return integer;
    }

    private static boolean optimize(boolean negative, byte[] integer) {
        if (1 == integer.length && 0 == integer[integer.length - 1]) {
            return true;
        }
        return negative;
    }

    private static short[] bytes_to_shorts(byte[] bytes) {
        short[] shorts = new short[bytes.length];
        for (int i = 0; i < shorts.length; ++i) {
            shorts[i] = (short) (bytes[i] & 0xff);
        }
        return shorts;
    }

    private static byte[] shorts_to_bytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) shorts[i];
        }
        return bytes;
    }

    private static short[] expand(byte[] integer, int length) {
        int target_length = Math.max(integer.length, length);
        short[] shorts = new short[target_length];
        for (int i = 0; i < integer.length; ++i) {
            shorts[i] = (short) (integer[i] & 0xff);
        }
        return shorts;
    }

    public LargeInteger(LargeInteger large_integer) {
        negative = large_integer.negative;
        integer = Arrays.copyOf(large_integer.integer, large_integer.integer.length);
    }

    private LargeInteger() {
        negative = false;
        integer = new byte[1];
    }

    private LargeInteger(byte[] integer, boolean negative) {
        this.integer = optimize(integer);
        this.negative = optimize(negative, this.integer);
    }

    public LargeInteger(byte number) {
        long b = reverse_negative(number);
        byte[] integer = new byte[1];
        integer[0] = (byte) (b & 0xff);
        this.integer = integer;
        this.negative = b != number;
    }

    public LargeInteger(short number) {
        long b = reverse_negative(number);
        byte[] integer = new byte[2];
        integer[0] = (byte) (b & 0xff);
        integer[1] = (byte) (b >> 8 & 0xff);
        this.integer = optimize(integer);
        this.negative = b != number;
    }

    public LargeInteger(int number) {
        long b = reverse_negative(number);
        byte[] integer = new byte[4];
        integer[0] = (byte) (b & 0xff);
        integer[1] = (byte) (b >> 8 & 0xff);
        integer[2] = (byte) (b >> 16 & 0xff);
        integer[3] = (byte) (b >> 24 & 0xff);
        this.integer = optimize(integer);
        this.negative = b != number;
    }

    public LargeInteger(long number) {
        this(Long.toString(number));
    }

    private static boolean is_digit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean is_plus(char c) {
        return '+' == c;
    }

    private static boolean is_minus(char c) {
        return '-' == c;
    }

    private static byte to_byte(char c) {
        return (byte) (c - '0');
    }

    private static char to_char(byte c) {
        return (char) (c + '0');
    }

    private static boolean is_overflow(short number) {
        return 0 != (number >> 8);
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
        for (int i = integer.length - 1; i >= 0; --i) {
            if ((integer[i] & 0xff) < (other.integer[i] & 0xff)) {
                return negative ? 1 : -1;
            } else if ((integer[i] & 0xff) > (other.integer[i] & 0xff)) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }

    private static void pass_carry(short[] integer, int i) {
        short carry = (short) (integer[i] >> 8);
        if (0 != carry) {
            integer[i + 1] += carry;
            integer[i] = (short) (integer[i] & 0xff);
        }
    }

    public LargeInteger plus(LargeInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return minus(other.negate());
        }
        short[] integer = expand(this.integer, Math.max(this.integer.length, other.integer.length) + 1);
        for (int i = 0; i < other.integer.length; ++i) {
            integer[i] += other.integer[i];
            pass_carry(integer, i);
        }
        for (int i = other.integer.length; i < integer.length; ++i) {
            pass_carry(integer, i);
        }
        return new LargeInteger(shorts_to_bytes(integer), this.negative);
    }

    private static void pass_borrow(short[] integer, int i) {
        if (integer[i] < 0) {
            integer[i] += 1 << 8;
            --integer[i + 1];
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
        short[] integer = bytes_to_shorts(a.integer);
        for (int i = 0; i < b.integer.length; ++i) {
            integer[i] -= b.integer[i];
            pass_borrow(integer, i);
        }
        for (int i = b.integer.length; i < a.integer.length; ++i) {
            pass_borrow(integer, i);
        }
        return new LargeInteger(shorts_to_bytes(integer), negative);
    }

    public LargeInteger multiply(LargeInteger other) {
        short[] integer = new short[this.integer.length + other.integer.length];
        for (int i = 0; i < this.integer.length; ++i) {
            for (int j = 0; j < other.integer.length; ++j) {
                integer[i + j] += (short) (this.integer[i] * other.integer[j]);
                pass_carry(integer, i + j);
            }
        }
        return new LargeInteger(shorts_to_bytes(integer), this.negative != other.negative);
    }

    private short upper_multiplier(LargeInteger dividend, LargeInteger divisor) {
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
        return (short) left;
    }

    public LargeInteger multiply_256(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        }
        byte[] integer = new byte[this.integer.length + times];
        System.arraycopy(this.integer, 0, integer, times, this.integer.length);
        return new LargeInteger(integer, this.negative);
    }

    public LargeInteger multiply_256() {
        return multiply_256(1);
    }

    public LargeInteger divide_by_256(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (times >= this.integer.length) {
            return ZERO;
        }
        byte[] integer = new byte[this.integer.length - times];
        System.arraycopy(this.integer, times, integer, 0, integer.length);
        return new LargeInteger(integer, this.negative);
    }

    public LargeInteger divide_by_256() {
        return divide_by_256(1);
    }

    public LargeInteger divide_by(LargeInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        byte[] integer = new byte[this.integer.length];
        LargeInteger remaining_dividend = abs();
        LargeInteger divisor = other.abs().multiply_256(integer.length);
        for (int i = integer.length - 1; i >= 0; --i) {
            divisor = divisor.divide_by_256();
            short multiplier = upper_multiplier(remaining_dividend, divisor);
            integer[i] = (byte) multiplier;
            remaining_dividend = remaining_dividend.minus(divisor.multiply(numbers[multiplier]));
        }
        return new LargeInteger(integer, this.negative != other.negative);
    }

    public LargeInteger mod(LargeInteger other) {
        return minus(divide_by(other).multiply(other));
    }
}