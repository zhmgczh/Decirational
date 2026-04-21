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
        final int cut_length = get_preceding_zeros(digits);
        byte[] result = digits;
        if (0 != cut_length) {
            result = new byte[digits.length - cut_length];
            System.arraycopy(digits, cut_length, result, 0, result.length);
        }
        return result;
    }

    public static int[] optimize(final int[] integer) {
        final int cut_length = get_preceding_zeros(integer);
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
        final byte carry = (byte) (sum / 10);
        digits[i] = (byte) (0 == carry ? sum : (sum % 10));
        return carry;
    }

    private static int pass_carry(final int[] integer, final long sum, final int i) {
        final int carry = (int) (sum >>> 32);
        integer[i] = (int) (sum & 0xffffffffL);
        return carry;
    }

    public static void add(final byte[] digits, final byte[] other_digits) {
        final int diff = digits.length - other_digits.length;
        byte carry = 0;
        final int other_offset = -diff;
        for (int i = digits.length - 1; i >= diff; --i) {
            final byte sum = (byte) (digits[i] + other_digits[other_offset + i] + carry);
            carry = pass_carry(digits, sum, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final byte sum = (byte) (digits[i] + carry);
            carry = pass_carry(digits, sum, i);
        }
    }

    public static void add(final byte[] digits, final int digits_s, final int digits_length, final byte[] other_digits, final int other_digits_s, final int other_digits_length) {
        final int diff = digits_length - other_digits_length;
        byte carry = 0;
        final int other_offset = other_digits_s - diff;
        for (int i = digits_length - 1; i >= diff; --i) {
            final int index = digits_s + i;
            final byte sum = (byte) (digits[index] + other_digits[other_offset + i] + carry);
            carry = pass_carry(digits, sum, index);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final int index = digits_s + i;
            final byte sum = (byte) (digits[index] + carry);
            carry = pass_carry(digits, sum, index);
        }
    }

    public static void add(final int[] integer, final int[] other_integer) {
        final int diff = integer.length - other_integer.length;
        int carry = 0;
        final int other_offset = -diff;
        for (int i = integer.length - 1; i >= diff; --i) {
            final long sum = (integer[i] & 0xffffffffL) + (other_integer[other_offset + i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final long sum = (integer[i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, i);
        }
    }

    public static void add(final int[] integer, final int integer_s, final int integer_length, final int[] other_integer, final int other_integer_s, final int other_integer_length) {
        final int diff = integer_length - other_integer_length;
        int carry = 0;
        final int other_offset = other_integer_s - diff;
        for (int i = integer_length - 1; i >= diff; --i) {
            final int index = integer_s + i;
            final long sum = (integer[index] & 0xffffffffL) + (other_integer[other_offset + i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, index);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final int index = integer_s + i;
            final long sum = (integer[index] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, index);
        }
    }

    private static byte pass_borrow(final byte[] digits, final byte difference, final int i) {
        final byte borrow = (byte) (difference >= 0 ? 0 : 1);
        digits[i] = (byte) (difference + (0 == borrow ? 0 : 10));
        return borrow;
    }

    private static int pass_borrow(final int[] integer, final long difference, final int i) {
        final int borrow = (difference >= 0 ? 0 : 1);
        integer[i] = (int) difference;
        return borrow;
    }

    public static void subtract(final byte[] digits, final byte[] other_digits) {
        final int diff = digits.length - other_digits.length;
        byte borrow = 0;
        final int other_offset = -diff;
        for (int i = digits.length - 1; i >= diff; --i) {
            final byte sum = (byte) (digits[i] - other_digits[other_offset + i] - borrow);
            borrow = pass_borrow(digits, sum, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final byte sum = (byte) (digits[i] - borrow);
            borrow = pass_borrow(digits, sum, i);
        }
    }

    public static void subtract(final byte[] digits, final int digits_s, final int digits_length, final byte[] other_digits, final int other_digits_s, final int other_digits_length) {
        final int diff = digits_length - other_digits_length;
        byte borrow = 0;
        final int other_offset = other_digits_s - diff;
        for (int i = digits_length - 1; i >= diff; --i) {
            final int index = digits_s + i;
            final byte sum = (byte) (digits[index] - other_digits[other_offset + i] - borrow);
            borrow = pass_borrow(digits, sum, index);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final int index = digits_s + i;
            final byte sum = (byte) (digits[index] - borrow);
            borrow = pass_borrow(digits, sum, index);
        }
    }

    public static void subtract(final int[] integer, final int[] other_integer) {
        final int diff = integer.length - other_integer.length;
        int borrow = 0;
        final int other_offset = -diff;
        for (int i = integer.length - 1; i >= diff; --i) {
            final long difference = (integer[i] & 0xffffffffL) - (other_integer[other_offset + i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, i);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final long difference = (integer[i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, i);
        }
    }

    public static void subtract(final int[] integer, final int integer_s, final int integer_length, final int[] other_integer, final int other_integer_s, final int other_integer_length) {
        final int diff = integer_length - other_integer_length;
        int borrow = 0;
        final int other_offset = other_integer_s - diff;
        for (int i = integer_length - 1; i >= diff; --i) {
            final int index = integer_s + i;
            final long difference = (integer[index] & 0xffffffffL) - (other_integer[other_offset + i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, index);
        }
        for (int i = diff - 1; i >= 0; --i) {
            final int index = integer_s + i;
            final long difference = (integer[index] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, index);
        }
    }

    public static void multiply(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
        for (int i = 1; i <= a_digits.length; ++i) {
            byte carry = 0;
            int digits_index = digits.length - i;
            final int a_digits_index = a_digits.length - i;
            int b_digits_index = b_digits.length - 1;
            for (int j = 1; j <= b_digits.length; ++j) {
                final byte sum = (byte) (digits[digits_index] + a_digits[a_digits_index] * b_digits[b_digits_index] + carry);
                carry = pass_carry(digits, sum, digits_index);
                --digits_index;
                --b_digits_index;
            }
            if (carry != 0) {
                digits[digits_index] += carry;
            }
        }
    }

    public static void multiply(final byte[] digits, final int digits_s, final int digits_length, final byte[] a_digits, final int a_digits_s, final int a_digits_length, final byte[] b_digits, final int b_digits_s, final int b_digits_length) {
        for (int i = 1; i <= a_digits_length; ++i) {
            byte carry = 0;
            int digits_index = digits_s + digits_length - i;
            final int a_digits_index = a_digits_s + a_digits_length - i;
            int b_digits_index = b_digits_s + b_digits_length - 1;
            for (int j = 1; j <= b_digits_length; ++j) {
                final byte sum = (byte) (digits[digits_index] + a_digits[a_digits_index] * b_digits[b_digits_index] + carry);
                carry = pass_carry(digits, sum, digits_index);
                --digits_index;
                --b_digits_index;
            }
            if (carry != 0) {
                digits[digits_index] += carry;
            }
        }
    }

    public static void multiply(final int[] integer, final int[] a_integer, final int[] b_integer) {
        for (int i = 1; i <= a_integer.length; ++i) {
            int carry = 0;
            int integer_index = integer.length - i;
            final int a_integer_index = a_integer.length - i;
            int b_integer_index = b_integer.length - 1;
            for (int j = 1; j <= b_integer.length; ++j) {
                final long sum = (integer[integer_index] & 0xffffffffL) + (a_integer[a_integer_index] & 0xffffffffL) * (b_integer[b_integer_index] & 0xffffffffL) + (carry & 0xffffffffL);
                carry = pass_carry(integer, sum, integer_index);
                --integer_index;
                --b_integer_index;
            }
            if (carry != 0) {
                integer[integer_index] += carry;
            }
        }
    }

    public static void multiply(final int[] integer, final int integer_s, final int integer_length, final int[] a_integer, final int a_integer_s, final int a_integer_length, final int[] b_integer, final int b_integer_s, final int b_integer_length) {
        for (int i = 1; i <= a_integer_length; ++i) {
            int carry = 0;
            int integer_index = integer_s + integer_length - i;
            final int a_integer_index = a_integer_s + a_integer_length - i;
            int b_integer_index = b_integer_s + b_integer_length - 1;
            for (int j = 1; j <= b_integer_length; ++j) {
                final long sum = (integer[integer_index] & 0xffffffffL) + (a_integer[a_integer_index] & 0xffffffffL) * (b_integer[b_integer_index] & 0xffffffffL) + (carry & 0xffffffffL);
                carry = pass_carry(integer, sum, integer_index);
                --integer_index;
                --b_integer_index;
            }
            if (carry != 0) {
                integer[integer_index] += carry;
            }
        }
    }

    public static void divide(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
    }

    public static void divide(final byte[] digits, final int digits_s, final int digits_length, final byte[] a_digits, final int a_digits_s, final int a_digits_length, final byte[] b_digits, final int b_digits_s, final int b_digits_length) {
    }

    public static void divide(final int[] integer, final int[] a_integer, final int[] b_integer) {
    }

    public static void divide(final int[] integer, final int integer_s, final int integer_length, final int[] a_integer, final int a_integer_s, final int a_integer_length, final int[] b_integer, final int b_integer_s, final int b_integer_length) {
    }

    public static void mod(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
    }

    public static void mod(final byte[] digits, final int digits_s, final int digits_length, final byte[] a_digits, final int a_digits_s, final int a_digits_length, final byte[] b_digits, final int b_digits_s, final int b_digits_length) {
    }

    public static void mod(final int[] integer, final int[] a_integer, final int[] b_integer) {
    }

    public static void mod(final int[] integer, final int integer_s, final int integer_length, final int[] a_integer, final int a_integer_s, final int a_integer_length, final int[] b_integer, final int b_integer_s, final int b_integer_length) {
    }
}
