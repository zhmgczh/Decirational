import java.util.Arrays;

public class DecimalInteger implements Comparable<DecimalInteger> {
    private boolean negative = false;
    private byte[] digits;

    private void optimize() {
        int cut_length = 0;
        for (int i = digits.length - 1; i > 0; --i) {
            if (0 != digits[i]) {
                break;
            }
            ++cut_length;
        }
        if (0 != cut_length) {
            digits = Arrays.copyOf(digits, digits.length - cut_length);
        }
        if (digits.length > 0 && 0 == digits[digits.length - 1]) {
            negative = false;
            digits = new byte[1];
        }
    }

    private void expand(int length) {
        int target_length = digits.length;
        while (length > target_length) {
            target_length += (target_length + 1) >> 1;
        }
        if (target_length > digits.length) {
            digits = Arrays.copyOf(digits, target_length);
        }
    }

    public DecimalInteger(DecimalInteger decimal_integer) {
        negative = decimal_integer.negative;
        digits = Arrays.copyOf(decimal_integer.digits, decimal_integer.digits.length);
    }

    public DecimalInteger() {
        digits = new byte[1];
    }

    public DecimalInteger(byte number) {
        this(Byte.toString(number));
    }

    public DecimalInteger(short number) {
        this(Short.toString(number));
    }

    public DecimalInteger(int number) {
        this(Integer.toString(number));
    }

    public DecimalInteger(long number) {
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

    public DecimalInteger(String number) {
        number = number.trim();
        int starting_point = 0;
        if (is_minus(number.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (is_plus(number.charAt(0))) {
            starting_point = 1;
        }
        for (int i = starting_point; i < number.length(); ++i) {
            if (!is_digit(number.charAt(i))) {
                throw new NumberFormatException("Your input " + number + " is invalid and cannot be converted to DecimalInteger.");
            }
        }
        digits = new byte[8];
        int cursor = 0;
        for (int i = number.length() - 1; i >= starting_point; --i) {
            expand(cursor + 1);
            digits[cursor++] = to_byte(number.charAt(i));
        }
        optimize();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte digit : digits) {
            sb.append(to_char(digit));
        }
        sb.append(negative ? '-' : "");
        return sb.reverse().toString();
    }

    public boolean is_negative() {
        return negative;
    }

    public DecimalInteger negate() {
        DecimalInteger result = new DecimalInteger(this);
        result.negative = !negative;
        return result;
    }

    public DecimalInteger abs() {
        DecimalInteger result = new DecimalInteger(this);
        result.negative = false;
        return result;
    }

    @Override
    public int compareTo(DecimalInteger other) {
        if (negative && !other.negative) {
            return -1;
        } else if (!negative && other.negative) {
            return 1;
        }
        if (digits.length < other.digits.length) {
            return negative ? 1 : -1;
        } else if (digits.length > other.digits.length) {
            return negative ? -1 : 1;
        }
        for (int i = digits.length - 1; i >= 0; --i) {
            if (digits[i] < other.digits[i]) {
                return negative ? 1 : -1;
            } else if (digits[i] > other.digits[i]) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }

    private static void pass_carry(DecimalInteger result, int i) {
        byte carry = (byte) (result.digits[i] / 10);
        if (0 != carry) {
            result.digits[i + 1] += carry;
            result.digits[i] %= 10;
        }
    }

    public DecimalInteger plus(DecimalInteger other) {
        if (negative != other.negative) {
            return minus(other.negate());
        }
        DecimalInteger result = new DecimalInteger(this);
        result.expand(Math.max(digits.length, other.digits.length) + 1);
        for (int i = 0; i < other.digits.length; ++i) {
            result.digits[i] += other.digits[i];
            pass_carry(result, i);
        }
        for (int i = other.digits.length; i < result.digits.length; ++i) {
            pass_carry(result, i);
        }
        result.optimize();
        return result;
    }

    private static void pass_borrow(DecimalInteger result, int i) {
        if (result.digits[i] < 0) {
            result.digits[i] += 10;
            --result.digits[i + 1];
        }
    }

    public DecimalInteger minus(DecimalInteger other) {
        if (negative != other.negative) {
            return plus(other.negate());
        }
        int compare_to = abs().compareTo(other.abs());
        DecimalInteger a, b;
        boolean negative = false;
        if (0 == compare_to) {
            return new DecimalInteger();
        } else if (0 < compare_to) {
            a = this;
            b = other;
        } else {
            a = other;
            b = this;
            negative = true;
        }
        DecimalInteger result = new DecimalInteger(a);
        result.negative = negative;
        for (int i = 0; i < b.digits.length; ++i) {
            result.digits[i] -= b.digits[i];
            pass_borrow(result, i);
        }
        for (int i = b.digits.length; i < result.digits.length; ++i) {
            pass_borrow(result, i);
        }
        result.optimize();
        return result;
    }
}
