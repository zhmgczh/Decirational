import java.util.Arrays;

public class Arithmetic {
    public static boolean is_digit(final char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean is_plus(final char c) {
        return '+' == c;
    }

    public static boolean is_minus(final char c) {
        return '-' == c;
    }

    public static byte to_byte(final char c) {
        return (byte) (c - '0');
    }

    public static char to_char(final byte c) {
        return (char) (c + '0');
    }

    private static int get_preceding_zeros(final byte[] digits) {
        int cut_length = 0;
        for (int i = 0; i < digits.length - 1; ++i) {
            if (0 != digits[i]) {
                break;
            }
            ++cut_length;
        }
        return cut_length;
    }

    private static int get_preceding_zeros(final int[] integer) {
        int cut_length = 0;
        for (int i = 0; i < integer.length - 1; ++i) {
            if (0 != integer[i]) {
                break;
            }
            ++cut_length;
        }
        return cut_length;
    }

    public static byte[] optimize(final byte[] digits) {
        int cut_length = get_preceding_zeros(digits);
        byte[] result = digits;
        if (0 != cut_length) {
            result = new byte[digits.length - cut_length];
            System.arraycopy(digits, cut_length, result, 0, result.length);
        }
        return result;
    }

    public static int[] optimize(final int[] integer) {
        int cut_length = get_preceding_zeros(integer);
        int[] result = integer;
        if (0 != cut_length) {
            result = new int[integer.length - cut_length];
            System.arraycopy(integer, cut_length, result, 0, result.length);
        }
        return result;
    }

    public static boolean optimize(final boolean negative, final byte[] digits) {
        if (1 == digits.length && 0 == digits[digits.length - 1]) {
            return false;
        }
        return negative;
    }

    public static boolean optimize(final boolean negative, final int[] integer) {
        if (1 == integer.length && 0 == integer[integer.length - 1]) {
            return false;
        }
        return negative;
    }

    public static byte[] expand(final byte[] digits, final int length) {
        byte[] result = digits;
        if (length > digits.length) {
            result = new byte[length];
            System.arraycopy(digits, 0, result, length - digits.length, digits.length);
        }
        return result;
    }

    public static int[] expand(final int[] integer, final int length) {
        int[] result = integer;
        if (length > integer.length) {
            result = new int[length];
            System.arraycopy(integer, 0, result, length - integer.length, integer.length);
        }
        return result;
    }

    public static long reverse_negative(long number) {
        if (number < 0) {
            number = -number;
        }
        return number;
    }

    private static byte pass_carry(final byte[] digits, final byte sum, final int i) {
        byte carry = (byte) (sum / 10);
        digits[i] = (byte) (0 == carry ? sum : (sum % 10));
        return carry;
    }

    private static int pass_carry(final int[] digits, final long sum, final int i) {
        int carry = (int) (sum >>> 32);
        digits[i] = (int) (sum & 0xffffffffL);
        return carry;
    }

    public static void add(final byte[] digits, final byte[] other_digits) {
        int diff = digits.length - other_digits.length;
        byte carry = 0;
        for (int i = digits.length - 1; i >= diff; --i) {
            byte sum = (byte) (digits[i] + other_digits[i - diff] + carry);
            carry = pass_carry(digits, sum, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            byte sum = (byte) (digits[i] + carry);
            carry = pass_carry(digits, sum, i);
        }
    }

    public static void add(final byte[] digits, final int digits_s, final int digits_length, final byte[] other_digits, final int other_digits_s, final int other_digits_length) {
        int diff = digits_length - other_digits_length;
        byte carry = 0;
        for (int i = digits_length - 1; i >= diff; --i) {
            byte sum = (byte) (digits[digits_s + i] + other_digits[other_digits_s + i - diff] + carry);
            carry = pass_carry(digits, sum, digits_s + i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            byte sum = (byte) (digits[digits_s + i] + carry);
            carry = pass_carry(digits, sum, digits_s + i);
        }
    }

    public static void add(final int[] digits, final int[] other_digits) {
        int diff = digits.length - other_digits.length;
        int carry = 0;
        for (int i = digits.length - 1; i >= diff; --i) {
            long sum = (digits[i] & 0xffffffffL) + (other_digits[i - diff] & 0xffffffffL) + (long) carry;
            carry = pass_carry(digits, sum, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            long sum = (digits[i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(digits, sum, i);
        }
    }

    public static void add(final int[] digits, final int digits_s, final int digits_length, final int[] other_digits, final int other_digits_s, final int other_digits_length) {
        int diff = digits_length - other_digits_length;
        int carry = 0;
        for (int i = digits_length - 1; i >= diff; --i) {
            long sum = (digits[digits_s + i] & 0xffffffffL) + (other_digits[other_digits_s + i - diff] & 0xffffffffL) + (long) carry;
            carry = pass_carry(digits, sum, digits_s + i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            long sum = (digits[digits_s + i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(digits, sum, digits_s + i);
        }
    }

    public static byte pass_borrow(final byte[] digits, final byte difference, final int i) {
        byte borrow = (byte) (difference >= 0 ? 0 : 1);
        digits[i] = (byte) (difference + (0 == borrow ? 0 : 10));
        return borrow;
    }

    public static int pass_borrow(final int[] digits, final long difference, final int i) {
        int borrow = (difference >= 0 ? 0 : 1);
        digits[i] = (int) difference;
        return borrow;
    }

    public static void subtract(final byte[] digits, final byte[] other_digits) {
        int diff = digits.length - other_digits.length;
        byte borrow = 0;
        for (int i = digits.length - 1; i >= diff; --i) {
            byte sum = (byte) (digits[i] - other_digits[i - diff] - borrow);
            borrow = pass_borrow(digits, sum, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            byte sum = (byte) (digits[i] - borrow);
            borrow = pass_borrow(digits, sum, i);
        }
    }

    public static void subtract(final byte[] digits, final int digits_s, final int digits_length, final byte[] other_digits, final int other_digits_s, final int other_digits_length) {
        int diff = digits_length - other_digits_length;
        byte borrow = 0;
        for (int i = digits_length - 1; i >= diff; --i) {
            byte sum = (byte) (digits[digits_s + i] - other_digits[other_digits_s + i - diff] - borrow);
            borrow = pass_borrow(digits, sum, digits_s + i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            byte sum = (byte) (digits[digits_s + i] - borrow);
            borrow = pass_borrow(digits, sum, digits_s + i);
        }
    }

    public static void subtract(final int[] digits, final int[] other_digits) {
        int diff = digits.length - other_digits.length;
        int borrow = 0;
        for (int i = digits.length - 1; i >= diff; --i) {
            long difference = (digits[i] & 0xffffffffL) - (other_digits[i - diff] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(digits, difference, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            long difference = (digits[i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(digits, difference, i);
        }
    }

    public static void subtract(final int[] digits, final int digits_s, final int digits_length, final int[] other_digits, final int other_digits_s, final int other_digits_length) {
        int diff = digits_length - other_digits_length;
        int borrow = 0;
        for (int i = digits_length - 1; i >= diff; --i) {
            long difference = (digits[digits_s + i] & 0xffffffffL) - (other_digits[other_digits_s + i - diff] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(digits, difference, digits_s + i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            long difference = (digits[digits_s + i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(digits, difference, digits_s + i);
        }
    }

    public static void multiply(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
        for (int i = 1; i <= a_digits.length; ++i) {
            byte carry = 0;
            for (int j = 1; j <= b_digits.length; ++j) {
                int index = digits.length + 1 - (i + j);
                byte sum = (byte) (digits[index] + a_digits[a_digits.length - i] * b_digits[b_digits.length - j] + carry);
                carry = pass_carry(digits, sum, index);
            }
            if (carry != 0) {
                digits[digits.length - (i + b_digits.length)] += carry;
            }
        }
    }

    public static void multiply(final byte[] digits, final int digits_s, final int digits_length, final byte[] a_digits, final int a_digits_s, final int a_digits_length, final byte[] b_digits, final int b_digits_s, final int b_digits_length) {
        for (int i = 1; i <= a_digits_length; ++i) {
            byte carry = 0;
            for (int j = 1; j <= b_digits_length; ++j) {
                int index = digits_s + digits_length + 1 - (i + j);
                byte sum = (byte) (digits[index] + a_digits[a_digits_s + a_digits_length - i] * b_digits[b_digits_s + b_digits_length - j] + carry);
                carry = pass_carry(digits, sum, index);
            }
            if (carry != 0) {
                digits[digits_s + digits_length - (i + b_digits_length)] += carry;
            }
        }
    }

    public static void multiply(final int[] digits, final int[] a_digits, final int[] b_digits) {
        for (int i = 1; i <= a_digits.length; ++i) {
            int carry = 0;
            for (int j = 1; j <= b_digits.length; ++j) {
                int index = digits.length + 1 - (i + j);
                long sum = (digits[index] & 0xffffffffL) + (a_digits[a_digits.length - i] & 0xffffffffL) * (b_digits[b_digits.length - j] & 0xffffffffL) + (carry & 0xffffffffL);
                carry = pass_carry(digits, sum, index);
            }
            if (carry != 0) {
                digits[digits.length - (i + b_digits.length)] += carry;
            }
        }
    }

    public static void multiply(final int[] digits, final int digits_s, final int digits_length, final int[] a_digits, final int a_digits_s, final int a_digits_length, final int[] b_digits, final int b_digits_s, final int b_digits_length) {
        for (int i = 1; i <= a_digits_length; ++i) {
            int carry = 0;
            for (int j = 1; j <= b_digits_length; ++j) {
                int index = digits_s + digits_length + 1 - (i + j);
                long sum = (digits[index] & 0xffffffffL) + (a_digits[a_digits_s + a_digits_length - i] & 0xffffffffL) * (b_digits[b_digits_s + b_digits_length - j] & 0xffffffffL) + (carry & 0xffffffffL);
                carry = pass_carry(digits, sum, index);
            }
            if (carry != 0) {
                digits[digits_s + digits_length - (i + b_digits_length)] += carry;
            }
        }
    }

    public static void divide(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
    }

    public static void divide(final int[] digits, final int[] a_digits, final int[] b_digits) {
    }

    public static void mod(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
    }

    public static void mod(final int[] digits, final int[] a_digits, final int[] b_digits) {
    }
}
