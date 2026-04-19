import java.util.Arrays;

public class LargeInteger implements Comparable<LargeInteger> {
    private boolean negative = false;
    private byte[] integer;

    private long set_sign_and_reverse(long number) {
        if (number < 0) {
            negative = true;
            number = -number;
        }
        return number;
    }

    private void optimize() {
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
        if (integer.length > 0 && 0 == integer[integer.length - 1]) {
            negative = false;
            integer = new byte[1];
        }
    }

    private void expand(int length) {
        while (length > integer.length) {
            integer = Arrays.copyOf(integer, integer.length + ((integer.length + 1) >> 1));
        }
    }

    public LargeInteger(LargeInteger large_integer) {
        negative = large_integer.negative;
        integer = Arrays.copyOf(large_integer.integer, large_integer.integer.length);
    }

    public LargeInteger() {
        integer = new byte[1];
    }

    public LargeInteger(byte number) {
        long b = set_sign_and_reverse(number);
        integer = new byte[1];
        integer[0] = (byte) (b & 0xff);
    }

    public LargeInteger(short number) {
        long b = set_sign_and_reverse(number);
        integer = new byte[2];
        integer[0] = (byte) (b & 0xff);
        integer[1] = (byte) (b >> 8 & 0xff);
        optimize();
    }

    public LargeInteger(int number) {
        long b = set_sign_and_reverse(number);
        integer = new byte[4];
        integer[0] = (byte) (b & 0xff);
        integer[1] = (byte) (b >> 8 & 0xff);
        integer[2] = (byte) (b >> 16 & 0xff);
        integer[3] = (byte) (b >> 24 & 0xff);
        optimize();
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

    }

    public String toString() {
        return null;
    }

    public boolean is_negative() {
        return negative;
    }

    public LargeInteger negate() {
        LargeInteger result = new LargeInteger(this);
        result.negative = !negative;
        return result;
    }

    public LargeInteger abs() {
        LargeInteger result = new LargeInteger(this);
        result.negative = false;
        return result;
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
            if (integer[i] < other.integer[i]) {
                return negative ? 1 : -1;
            } else if (integer[i] > other.integer[i]) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }
}