import java.util.Arrays;

public final class Arithmetic {
    public static final double tight_to_decimal_length_ratio = 32 * Math.log10(2);
    public static final double decimal_to_tight_length_ratio = Math.log(10) / (32 * Math.log(2));
    public static final char cyclic_begin = '[';
    public static final char cyclic_end = ']';

    public static boolean is_digit(final char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean is_plus(final char c) {
        return '+' == c;
    }

    public static boolean is_minus(final char c) {
        return '-' == c;
    }

    public static boolean is_fraction_bar(final char c) {
        return '/' == c;
    }

    public static boolean is_decimal_point(final char c) {
        return '.' == c;
    }

    public static boolean is_cyclic_begin(final char c) {
        return cyclic_begin == c;
    }

    public static boolean is_cyclic_end(final char c) {
        return cyclic_end == c;
    }

    public static byte to_byte(final char c) {
        return (byte) (c - '0');
    }

    public static char to_char(final byte c) {
        return (char) (c + '0');
    }

    private static int get_preceding_zeros(final byte[] digits, final int digits_s, final int digits_length) {
        int cut_length = digits_s;
        final int ending_point = digits_s + digits_length - 1;
        for (int i = digits_s; i <= ending_point; ++i) {
            if (0 != digits[i]) {
                break;
            }
            ++cut_length;
        }
        return cut_length;
    }

    private static int get_preceding_zeros(final int[] integer, final int integer_s, final int integer_length) {
        int cut_length = integer_s;
        final int ending_point = integer_s + integer_length - 1;
        for (int i = integer_s; i <= ending_point; ++i) {
            if (0 != integer[i]) {
                break;
            }
            ++cut_length;
        }
        return cut_length;
    }

    public static byte[] optimize(final byte[] digits) {
        final int cut_length = Math.min(get_preceding_zeros(digits, 0, digits.length), digits.length - 1);
        byte[] result = digits;
        if (0 != cut_length) {
            result = new byte[digits.length - cut_length];
            System.arraycopy(digits, cut_length, result, 0, result.length);
        }
        return result;
    }

    public static int[] optimize(final int[] integer) {
        final int cut_length = Math.min(get_preceding_zeros(integer, 0, integer.length), integer.length - 1);
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
        int other_index = other_digits.length - 1;
        for (int i = digits.length - 1; i >= diff; --i) {
            final byte sum = (byte) (digits[i] + other_digits[other_index] + carry);
            carry = pass_carry(digits, sum, i);
            --other_index;
        }
        for (int i = diff - 1; i >= 0; --i) {
            final byte sum = (byte) (digits[i] + carry);
            carry = pass_carry(digits, sum, i);
        }
    }

    public static void add(final byte[] digits, final int digits_s, final int digits_length, final byte[] other_digits, final int other_digits_s, final int other_digits_length) {
        final int diff = digits_s + digits_length - other_digits_length;
        byte carry = 0;
        int other_index = other_digits_s + other_digits_length - 1;
        for (int i = digits_s + digits_length - 1; i >= diff; --i) {
            final byte sum = (byte) (digits[i] + other_digits[other_index] + carry);
            carry = pass_carry(digits, sum, i);
            --other_index;
        }
        for (int i = diff - 1; i >= digits_s; --i) {
            final byte sum = (byte) (digits[i] + carry);
            carry = pass_carry(digits, sum, i);
        }
    }

    public static void add(final int[] integer, final int[] other_integer) {
        final int diff = integer.length - other_integer.length;
        int carry = 0;
        int other_index = other_integer.length - 1;
        for (int i = integer.length - 1; i >= diff; --i) {
            final long sum = (integer[i] & 0xffffffffL) + (other_integer[other_index] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, i);
            --other_index;
        }
        for (int i = diff - 1; i >= 0; --i) {
            final long sum = (integer[i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, i);
        }
    }

    public static void add(final int[] integer, final int integer_s, final int integer_length, final int[] other_integer, final int other_integer_s, final int other_integer_length) {
        final int diff = integer_s + integer_length - other_integer_length;
        int carry = 0;
        int other_index = other_integer_s + other_integer_length - 1;
        for (int i = integer_s + integer_length - 1; i >= diff; --i) {
            final long sum = (integer[i] & 0xffffffffL) + (other_integer[other_index] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, i);
            --other_index;
        }
        for (int i = diff - 1; i >= integer_s; --i) {
            final long sum = (integer[i] & 0xffffffffL) + (long) carry;
            carry = pass_carry(integer, sum, i);
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
        int other_index = other_digits.length - 1;
        for (int i = digits.length - 1; i >= diff; --i) {
            final byte sum = (byte) (digits[i] - other_digits[other_index] - borrow);
            borrow = pass_borrow(digits, sum, i);
            --other_index;
        }
        for (int i = diff - 1; i >= 0; --i) {
            final byte sum = (byte) (digits[i] - borrow);
            borrow = pass_borrow(digits, sum, i);
        }
    }

    public static void subtract(final byte[] digits, final int digits_s, final int digits_length, final byte[] other_digits, final int other_digits_s, final int other_digits_length) {
        final int diff = digits_s + digits_length - other_digits_length;
        byte borrow = 0;
        int other_index = other_digits_s + other_digits_length - 1;
        for (int i = digits_s + digits_length - 1; i >= diff; --i) {
            final byte sum = (byte) (digits[i] - other_digits[other_index] - borrow);
            borrow = pass_borrow(digits, sum, i);
            --other_index;
        }
        for (int i = diff - 1; i >= digits_s; --i) {
            final byte sum = (byte) (digits[i] - borrow);
            borrow = pass_borrow(digits, sum, i);
        }
    }

    public static void subtract(final int[] integer, final int[] other_integer) {
        final int diff = integer.length - other_integer.length;
        int borrow = 0;
        int other_index = other_integer.length - 1;
        for (int i = integer.length - 1; i >= diff; --i) {
            final long difference = (integer[i] & 0xffffffffL) - (other_integer[other_index] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, i);
            --other_index;
        }
        for (int i = diff - 1; i >= 0; --i) {
            final long difference = (integer[i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, i);
        }
    }

    public static void subtract(final int[] integer, final int integer_s, final int integer_length, final int[] other_integer, final int other_integer_s, final int other_integer_length) {
        final int diff = integer_s + integer_length - other_integer_length;
        int borrow = 0;
        int other_index = other_integer_s + other_integer_length - 1;
        for (int i = integer_s + integer_length - 1; i >= diff; --i) {
            final long difference = (integer[i] & 0xffffffffL) - (other_integer[other_index] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, i);
            --other_index;
        }
        for (int i = diff - 1; i >= integer_s; --i) {
            final long difference = (integer[i] & 0xffffffffL) - (long) borrow;
            borrow = pass_borrow(integer, difference, i);
        }
    }

    public static void multiply(final byte[] digits, final byte[] a_digits, final byte[] b_digits) {
        for (int i = 1; i <= a_digits.length; ++i) {
            byte carry = 0;
            int digits_index = digits.length - i;
            final int a_digits_index = a_digits.length - i;
            for (int b_digits_index = b_digits.length - 1; b_digits_index >= 0; --b_digits_index) {
                final byte sum = (byte) (digits[digits_index] + a_digits[a_digits_index] * b_digits[b_digits_index] + carry);
                carry = pass_carry(digits, sum, digits_index);
                --digits_index;
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
            for (int b_digits_index = b_digits_s + b_digits_length - 1; b_digits_index >= b_digits_s; --b_digits_index) {
                final byte sum = (byte) (digits[digits_index] + a_digits[a_digits_index] * b_digits[b_digits_index] + carry);
                carry = pass_carry(digits, sum, digits_index);
                --digits_index;
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
            for (int b_integer_index = b_integer.length - 1; b_integer_index >= 0; --b_integer_index) {
                final long sum = (integer[integer_index] & 0xffffffffL) + (a_integer[a_integer_index] & 0xffffffffL) * (b_integer[b_integer_index] & 0xffffffffL) + (carry & 0xffffffffL);
                carry = pass_carry(integer, sum, integer_index);
                --integer_index;
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
            for (int b_integer_index = b_integer_s + b_integer_length - 1; b_integer_index >= b_integer_s; --b_integer_index) {
                final long sum = (integer[integer_index] & 0xffffffffL) + (a_integer[a_integer_index] & 0xffffffffL) * (b_integer[b_integer_index] & 0xffffffffL) + (carry & 0xffffffffL);
                carry = pass_carry(integer, sum, integer_index);
                --integer_index;
            }
            if (carry != 0) {
                integer[integer_index] += carry;
            }
        }
    }

    public static void multiply(final byte[] digits, final byte a, final byte[] b_digits) {
        byte carry = 0;
        int digits_index = digits.length - 1;
        for (int b_digits_index = b_digits.length - 1; b_digits_index >= 0; --b_digits_index) {
            final byte sum = (byte) (digits[digits_index] + a * b_digits[b_digits_index] + carry);
            carry = pass_carry(digits, sum, digits_index);
            --digits_index;
        }
        if (carry != 0) {
            digits[digits_index] += carry;
        }
    }

    public static void multiply(final byte[] digits, final int digits_s, final int digits_length, final byte a, final byte[] b_digits, final int b_digits_s, final int b_digits_length) {
        byte carry = 0;
        int digits_index = digits_s + digits_length - 1;
        for (int b_digits_index = b_digits_s + b_digits_length - 1; b_digits_index >= b_digits_s; --b_digits_index) {
            final byte sum = (byte) (digits[digits_index] + a * b_digits[b_digits_index] + carry);
            carry = pass_carry(digits, sum, digits_index);
            --digits_index;
        }
        if (carry != 0) {
            digits[digits_index] += carry;
        }
    }

    public static void multiply(final int[] integer, final int a, final int[] b_integer) {
        int carry = 0;
        int integer_index = integer.length - 1;
        for (int b_integer_index = b_integer.length - 1; b_integer_index >= 0; --b_integer_index) {
            final long sum = (integer[integer_index] & 0xffffffffL) + (a & 0xffffffffL) * (b_integer[b_integer_index] & 0xffffffffL) + (carry & 0xffffffffL);
            carry = pass_carry(integer, sum, integer_index);
            --integer_index;
        }
        if (carry != 0) {
            integer[integer_index] += carry;
        }
    }

    public static void multiply(final int[] integer, final int integer_s, final int integer_length, final int a, final int[] b_integer, final int b_integer_s, final int b_integer_length) {
        int carry = 0;
        int integer_index = integer_s + integer_length - 1;
        for (int b_integer_index = b_integer_s + b_integer_length - 1; b_integer_index >= b_integer_s; --b_integer_index) {
            final long sum = (integer[integer_index] & 0xffffffffL) + (a & 0xffffffffL) * (b_integer[b_integer_index] & 0xffffffffL) + (carry & 0xffffffffL);
            carry = pass_carry(integer, sum, integer_index);
            --integer_index;
        }
        if (carry != 0) {
            integer[integer_index] += carry;
        }
    }

    public static int compare(final byte[] a, final int a_s, final int a_length, final byte[] b, final int b_s, final int b_length) {
        final int new_a_s = get_preceding_zeros(a, a_s, a_length);
        final int new_b_s = get_preceding_zeros(b, b_s, b_length);
        final int new_a_length = a_length - new_a_s + a_s;
        final int new_b_length = b_length - new_b_s + b_s;
        if (new_a_length > new_b_length) {
            return 1;
        } else if (new_a_length < new_b_length) {
            return -1;
        }
        final int new_a_e = new_a_s + new_a_length;
        int b_index = new_b_s;
        for (int i = new_a_s; i < new_a_e; ++i) {
            if (a[i] > b[b_index]) {
                return 1;
            } else if (a[i] < b[b_index]) {
                return -1;
            }
            ++b_index;
        }
        return 0;
    }

    public static int compare(final byte[] a, final byte[] b) {
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(final int[] a, final int a_s, final int a_length, final int[] b, final int b_s, final int b_length) {
        final int new_a_s = get_preceding_zeros(a, a_s, a_length);
        final int new_b_s = get_preceding_zeros(b, b_s, b_length);
        final int new_a_length = a_length - new_a_s + a_s;
        final int new_b_length = b_length - new_b_s + b_s;
        if (new_a_length > new_b_length) {
            return 1;
        } else if (new_a_length < new_b_length) {
            return -1;
        }
        final int new_a_e = new_a_s + new_a_length;
        int b_index = new_b_s;
        for (int i = new_a_s; i < new_a_e; ++i) {
            if ((a[i] & 0xffffffffL) > (b[b_index] & 0xffffffffL)) {
                return 1;
            } else if ((a[i] & 0xffffffffL) < (b[b_index] & 0xffffffffL)) {
                return -1;
            }
            ++b_index;
        }
        return 0;
    }

    public static int compare(final int[] a, final int[] b) {
        return compare(a, 0, a.length, b, 0, b.length);
    }

    private static int find_right_boundary(final byte[] dividend, final int dividend_s, final byte[] divisor, final int divisor_s, final int divisor_length) {
        final int divisor_e = divisor_s + divisor_length;
        final int dividend_bound = dividend_s + divisor_length - 1;
        int dividend_index = dividend_s;
        for (int i = divisor_s; i < divisor_e; ++i) {
            if (dividend_index >= dividend.length) {
                return dividend_bound + 1;
            }
            if (dividend[dividend_index] > divisor[i]) {
                return dividend_bound;
            } else if (dividend[dividend_index] < divisor[i]) {
                return dividend_bound + 1;
            }
            ++dividend_index;
        }
        return dividend_bound;
    }

    private static int find_right_boundary(final int[] dividend, final int dividend_s, final int[] divisor, final int divisor_s, final int divisor_length) {
        final int divisor_e = divisor_s + divisor_length;
        final int dividend_bound = dividend_s + divisor_length - 1;
        int dividend_index = dividend_s;
        for (int i = divisor_s; i < divisor_e; ++i) {
            if (dividend_index >= dividend.length) {
                return dividend_bound + 1;
            }
            if ((dividend[dividend_index] & 0xffffffffL) > (divisor[i] & 0xffffffffL)) {
                return dividend_bound;
            } else if ((dividend[dividend_index] & 0xffffffffL) < (divisor[i] & 0xffffffffL)) {
                return dividend_bound + 1;
            }
            ++dividend_index;
        }
        return dividend_bound;
    }

    private static byte multiplier(final byte[] temp, final int temp_s, final int temp_length, final byte[] dividend, final int dividend_s, final int dividend_length, final byte[] divisor, final int divisor_s, final int divisor_length) {
        byte left = 0;
        byte right = 9;
        byte mid = (byte) ((left + right + 1) >> 1);
        final int temp_e = temp_s + temp_length;
        OUTER:
        while (left < right) {
            mid = (byte) ((left + right + 1) >> 1);
            Arrays.fill(temp, temp_s, temp_e, (byte) 0);
            multiply(temp, temp_s, temp_length, mid, divisor, divisor_s, divisor_length);
            int compare_value = compare(temp, temp_s, temp_length, dividend, dividend_s, dividend_length);
            switch (compare_value) {
                case 0:
                    left = mid;
                    break OUTER;
                case -1:
                    left = mid;
                    break;
                case 1:
                    right = (byte) (mid - 1);
            }
        }
        if (mid != left) {
            Arrays.fill(temp, temp_s, temp_e, (byte) 0);
            multiply(temp, temp_s, temp_length, left, divisor, divisor_s, divisor_length);
        }
        return left;
    }

    private static int multiplier(final int[] temp, final int temp_s, final int temp_length, final int[] dividend, final int dividend_s, final int dividend_length, final int[] divisor, final int divisor_s, final int divisor_length) {
        int left = 0;
        int right = 0xffffffff;
        int mid = (int) (((left & 0xffffffffL) + (right & 0xffffffffL) + 1L) >> 1);
        final int temp_e = temp_s + temp_length;
        OUTER:
        while ((left & 0xffffffffL) < (right & 0xffffffffL)) {
            mid = (int) (((left & 0xffffffffL) + (right & 0xffffffffL) + 1L) >> 1);
            Arrays.fill(temp, temp_s, temp_e, 0);
            multiply(temp, temp_s, temp_length, mid, divisor, divisor_s, divisor_length);
            int compare_value = compare(temp, temp_s, temp_length, dividend, dividend_s, dividend_length);
            switch (compare_value) {
                case 0:
                    left = mid;
                    break OUTER;
                case -1:
                    left = mid;
                    break;
                case 1:
                    right = mid - 1;
            }
        }
        if (mid != left) {
            Arrays.fill(temp, temp_s, temp_e, 0);
            multiply(temp, temp_s, temp_length, left, divisor, divisor_s, divisor_length);
        }
        return left;
    }

    public static void divide(final byte[] quotient, final byte[] dividend, final byte[] divisor) {
        final int new_divisor_s = get_preceding_zeros(divisor, 0, divisor.length);
        final int new_divisor_length = divisor.length - new_divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final byte[] temp = new byte[new_divisor_length + 1];
        final byte[] remaining_dividend = Arrays.copyOf(dividend, dividend.length);
        final int diff = quotient.length - remaining_dividend.length;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
    }

    public static void divide(final byte[] quotient, final int quotient_s, final int quotient_length, final byte[] dividend, final int dividend_s, final int dividend_length, final byte[] divisor, final int divisor_s, final int divisor_length) {
        final int new_divisor_s = get_preceding_zeros(divisor, divisor_s, divisor_length);
        final int new_divisor_length = divisor_length - new_divisor_s + divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final byte[] temp = new byte[new_divisor_length + 1];
        final byte[] remaining_dividend = new byte[dividend_length];
        System.arraycopy(dividend, dividend_s, remaining_dividend, 0, remaining_dividend.length);
        final int diff = quotient_length - remaining_dividend.length + quotient_s;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
    }

    public static void divide(final int[] quotient, final int[] dividend, final int[] divisor) {
        final int new_divisor_s = get_preceding_zeros(divisor, 0, divisor.length);
        final int new_divisor_length = divisor.length - new_divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final int[] temp = new int[new_divisor_length + 1];
        final int[] remaining_dividend = Arrays.copyOf(dividend, dividend.length);
        final int diff = quotient.length - remaining_dividend.length;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
    }

    public static void divide(final int[] quotient, final int quotient_s, final int quotient_length, final int[] dividend, final int dividend_s, final int dividend_length, final int[] divisor, final int divisor_s, final int divisor_length) {
        final int new_divisor_s = get_preceding_zeros(divisor, divisor_s, divisor_length);
        final int new_divisor_length = divisor_length - new_divisor_s + divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final int[] temp = new int[new_divisor_length + 1];
        final int[] remaining_dividend = new int[dividend_length];
        System.arraycopy(dividend, dividend_s, remaining_dividend, 0, remaining_dividend.length);
        final int diff = quotient_length - remaining_dividend.length + quotient_s;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
    }

    public static void modulo(final byte[] remainder, final byte[] dividend, final byte[] divisor) {
        final int new_divisor_s = get_preceding_zeros(divisor, 0, divisor.length);
        final int new_divisor_length = divisor.length - new_divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final byte[] temp = new byte[new_divisor_length + 1];
        final byte[] remaining_dividend = Arrays.copyOf(dividend, dividend.length);
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder.length - valid_length, valid_length);
    }

    public static void modulo(final byte[] remainder, final int remainder_s, final int remainder_length, final byte[] dividend, final int dividend_s, final int dividend_length, final byte[] divisor, final int divisor_s, final int divisor_length) {
        final int new_divisor_s = get_preceding_zeros(divisor, divisor_s, divisor_length);
        final int new_divisor_length = divisor_length - new_divisor_s + divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final byte[] temp = new byte[new_divisor_length + 1];
        final byte[] remaining_dividend = new byte[dividend_length];
        System.arraycopy(dividend, dividend_s, remaining_dividend, 0, remaining_dividend.length);
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder_s + remainder_length - valid_length, valid_length);
    }

    public static void modulo(final int[] remainder, final int[] dividend, final int[] divisor) {
        final int new_divisor_s = get_preceding_zeros(divisor, 0, divisor.length);
        final int new_divisor_length = divisor.length - new_divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final int[] temp = new int[new_divisor_length + 1];
        final int[] remaining_dividend = Arrays.copyOf(dividend, dividend.length);
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder.length - valid_length, valid_length);
    }

    public static void modulo(final int[] remainder, final int remainder_s, final int remainder_length, final int[] dividend, final int dividend_s, final int dividend_length, final int[] divisor, final int divisor_s, final int divisor_length) {
        final int new_divisor_s = get_preceding_zeros(divisor, divisor_s, divisor_length);
        final int new_divisor_length = divisor_length - new_divisor_s + divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final int[] temp = new int[new_divisor_length + 1];
        final int[] remaining_dividend = new int[dividend_length];
        System.arraycopy(dividend, dividend_s, remaining_dividend, 0, remaining_dividend.length);
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder_s + remainder_length - valid_length, valid_length);
    }

    public static void divide_and_modulo(final byte[] quotient, final byte[] remainder, final byte[] dividend, final byte[] divisor) {
        final int new_divisor_s = get_preceding_zeros(divisor, 0, divisor.length);
        final int new_divisor_length = divisor.length - new_divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final byte[] temp = new byte[new_divisor_length + 1];
        final byte[] remaining_dividend = Arrays.copyOf(dividend, dividend.length);
        final int diff = quotient.length - remaining_dividend.length;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder.length - valid_length, valid_length);
    }

    public static void divide_and_modulo(final byte[] quotient, final int quotient_s, final int quotient_length, final byte[] remainder, final int remainder_s, final int remainder_length, final byte[] dividend, final int dividend_s, final int dividend_length, final byte[] divisor, final int divisor_s, final int divisor_length) {
        final int new_divisor_s = get_preceding_zeros(divisor, divisor_s, divisor_length);
        final int new_divisor_length = divisor_length - new_divisor_s + divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final byte[] temp = new byte[new_divisor_length + 1];
        final byte[] remaining_dividend = new byte[dividend_length];
        System.arraycopy(dividend, dividend_s, remaining_dividend, 0, remaining_dividend.length);
        final int diff = quotient_length - remaining_dividend.length + quotient_s;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder_s + remainder_length - valid_length, valid_length);
    }

    public static void divide_and_modulo(final int[] quotient, final int[] remainder, final int[] dividend, final int[] divisor) {
        final int new_divisor_s = get_preceding_zeros(divisor, 0, divisor.length);
        final int new_divisor_length = divisor.length - new_divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final int[] temp = new int[new_divisor_length + 1];
        final int[] remaining_dividend = Arrays.copyOf(dividend, dividend.length);
        final int diff = quotient.length - remaining_dividend.length;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder.length - valid_length, valid_length);
    }

    public static void divide_and_modulo(final int[] quotient, final int quotient_s, final int quotient_length, final int[] remainder, final int remainder_s, final int remainder_length, final int[] dividend, final int dividend_s, final int dividend_length, final int[] divisor, final int divisor_s, final int divisor_length) {
        final int new_divisor_s = get_preceding_zeros(divisor, divisor_s, divisor_length);
        final int new_divisor_length = divisor_length - new_divisor_s + divisor_s;
        if (0 == new_divisor_length) {
            throw new ArithmeticException("divide by 0");
        }
        final int[] temp = new int[new_divisor_length + 1];
        final int[] remaining_dividend = new int[dividend_length];
        System.arraycopy(dividend, dividend_s, remaining_dividend, 0, remaining_dividend.length);
        final int diff = quotient_length - remaining_dividend.length + quotient_s;
        int left_boundary = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        int right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        while (right_boundary < remaining_dividend.length) {
            final int current_dividend_length = right_boundary - left_boundary + 1;
            quotient[diff + right_boundary] = multiplier(temp, 0, temp.length, remaining_dividend, left_boundary, current_dividend_length, divisor, new_divisor_s, new_divisor_length);
            subtract(remaining_dividend, left_boundary, current_dividend_length, temp, 0 == temp[0] ? 1 : 0, 0 == temp[0] ? temp.length - 1 : temp.length);
            left_boundary = get_preceding_zeros(remaining_dividend, left_boundary, remaining_dividend.length - left_boundary);
            right_boundary = find_right_boundary(remaining_dividend, left_boundary, divisor, new_divisor_s, new_divisor_length);
        }
        final int starting_index = get_preceding_zeros(remaining_dividend, 0, remaining_dividend.length);
        final int valid_length = remaining_dividend.length - starting_index;
        System.arraycopy(remaining_dividend, starting_index, remainder, remainder_s + remainder_length - valid_length, valid_length);
    }

    private static final int[] decimal_base = new int[]{10};
    private static final byte[] tight_base = new byte[]{4, 2, 9, 4, 9, 6, 7, 2, 9, 6};

    public static void convert_tight_to_decimal(final byte[] digits, final int[] integer) {
        final int[] quotient_cache = Arrays.copyOf(integer, integer.length);
        final int[] temp = new int[quotient_cache.length];
        final int[] remainder_cache = new int[decimal_base.length];
        int digits_index = digits.length - 1;
        int left_boundary = get_preceding_zeros(quotient_cache, 0, quotient_cache.length);
        int remaining_length = quotient_cache.length - left_boundary;
        while (get_preceding_zeros(quotient_cache, left_boundary, remaining_length) < quotient_cache.length) {
            Arrays.fill(temp, left_boundary, temp.length, 0);
            remainder_cache[0] = 0;
            divide_and_modulo(temp, left_boundary, remaining_length, remainder_cache, 0, remainder_cache.length, quotient_cache, left_boundary, remaining_length, decimal_base, 0, decimal_base.length);
            digits[digits_index] = (byte) remainder_cache[0];
            left_boundary = get_preceding_zeros(temp, left_boundary, remaining_length);
            remaining_length = temp.length - left_boundary;
            System.arraycopy(temp, left_boundary, quotient_cache, left_boundary, remaining_length);
            --digits_index;
        }
    }

    public static void convert_tight_to_decimal(final byte[] digits, final int digits_s, final int digits_length, final int[] integer, final int integer_s, final int integer_length) {
        final int[] quotient_cache = new int[integer_length];
        System.arraycopy(integer, integer_s, quotient_cache, 0, integer_length);
        final int[] temp = new int[quotient_cache.length];
        final int[] remainder_cache = new int[decimal_base.length];
        int digits_index = digits_s + digits_length - 1;
        int left_boundary = get_preceding_zeros(quotient_cache, 0, quotient_cache.length);
        int remaining_length = quotient_cache.length - left_boundary;
        while (get_preceding_zeros(quotient_cache, left_boundary, remaining_length) < quotient_cache.length) {
            Arrays.fill(temp, left_boundary, temp.length, 0);
            remainder_cache[0] = 0;
            divide_and_modulo(temp, left_boundary, remaining_length, remainder_cache, 0, remainder_cache.length, quotient_cache, left_boundary, remaining_length, decimal_base, 0, decimal_base.length);
            digits[digits_index] = (byte) remainder_cache[0];
            left_boundary = get_preceding_zeros(temp, left_boundary, remaining_length);
            remaining_length = temp.length - left_boundary;
            System.arraycopy(temp, left_boundary, quotient_cache, left_boundary, remaining_length);
            --digits_index;
        }
    }

    private static int decimal_remainder_to_tight(final byte[] tight_remainder) {
        return tight_remainder[0] * 1000000000 + tight_remainder[1] * 100000000 + tight_remainder[2] * 10000000 + tight_remainder[3] * 1000000 + tight_remainder[4] * 100000 + tight_remainder[5] * 10000 + tight_remainder[6] * 1000 + tight_remainder[7] * 100 + tight_remainder[8] * 10 + tight_remainder[9];
    }

    public static void convert_decimal_to_tight(final int[] integer, final byte[] digits) {
        final byte[] quotient_cache = Arrays.copyOf(digits, digits.length);
        final byte[] temp = new byte[quotient_cache.length];
        final byte[] remainder_cache = new byte[tight_base.length];
        int integer_index = integer.length - 1;
        int left_boundary = get_preceding_zeros(quotient_cache, 0, quotient_cache.length);
        int remaining_length = quotient_cache.length - left_boundary;
        while (get_preceding_zeros(quotient_cache, left_boundary, remaining_length) < quotient_cache.length) {
            Arrays.fill(temp, left_boundary, temp.length, (byte) 0);
            Arrays.fill(remainder_cache, (byte) 0);
            divide_and_modulo(temp, left_boundary, remaining_length, remainder_cache, 0, remainder_cache.length, quotient_cache, left_boundary, remaining_length, tight_base, 0, tight_base.length);
            integer[integer_index] = decimal_remainder_to_tight(remainder_cache);
            left_boundary = get_preceding_zeros(temp, left_boundary, remaining_length);
            remaining_length = temp.length - left_boundary;
            System.arraycopy(temp, left_boundary, quotient_cache, left_boundary, remaining_length);
            --integer_index;
        }
    }

    public static void convert_decimal_to_tight(final int[] integer, final int integer_s, final int integer_length, final byte[] digits, final int digits_s, final int digits_length) {
        final byte[] quotient_cache = new byte[digits_length];
        System.arraycopy(digits, digits_s, quotient_cache, 0, digits_length);
        final byte[] temp = new byte[quotient_cache.length];
        final byte[] remainder_cache = new byte[tight_base.length];
        int integer_index = integer_s + integer_length - 1;
        int left_boundary = get_preceding_zeros(quotient_cache, 0, quotient_cache.length);
        int remaining_length = quotient_cache.length - left_boundary;
        while (get_preceding_zeros(quotient_cache, left_boundary, remaining_length) < quotient_cache.length) {
            Arrays.fill(temp, left_boundary, temp.length, (byte) 0);
            Arrays.fill(remainder_cache, (byte) 0);
            divide_and_modulo(temp, left_boundary, remaining_length, remainder_cache, 0, remainder_cache.length, quotient_cache, left_boundary, remaining_length, tight_base, 0, tight_base.length);
            integer[integer_index] = decimal_remainder_to_tight(remainder_cache);
            left_boundary = get_preceding_zeros(temp, left_boundary, remaining_length);
            remaining_length = temp.length - left_boundary;
            System.arraycopy(temp, left_boundary, quotient_cache, left_boundary, remaining_length);
            --integer_index;
        }
    }

    private static final byte[] decimal_zero = new byte[]{0};
    private static final int[] tight_zero = new int[]{0};

    public static void gcd(final byte[] result, final byte[] a, final byte[] b) {
        final int compare_value = compare(a, b);
        if (0 == compare_value) {
            int target_length = Math.min(result.length, Math.min(a.length, b.length));
            System.arraycopy(a, a.length - target_length, result, result.length - target_length, target_length);
            return;
        }
        final int maximum_length = Math.max(a.length, b.length);
        byte[] larger = new byte[maximum_length];
        byte[] smaller = new byte[maximum_length];
        if (0 < compare_value) {
            System.arraycopy(a, 0, larger, maximum_length - a.length, a.length);
            System.arraycopy(b, 0, smaller, maximum_length - b.length, b.length);
        } else {
            System.arraycopy(b, 0, larger, maximum_length - b.length, b.length);
            System.arraycopy(a, 0, smaller, maximum_length - a.length, a.length);
        }
        byte[] temp = new byte[maximum_length], temp_temp;
        int larger_left_boundary = get_preceding_zeros(larger, 0, maximum_length);
        int larger_length = maximum_length - larger_left_boundary;
        int smaller_left_boundary = get_preceding_zeros(smaller, 0, maximum_length);
        int smaller_length = maximum_length - smaller_left_boundary;
        while (0 != compare(smaller, smaller_left_boundary, smaller_length, decimal_zero, 0, decimal_zero.length)) {
            Arrays.fill(temp, smaller_left_boundary, maximum_length, (byte) 0);
            modulo(temp, smaller_left_boundary, smaller_length, larger, larger_left_boundary, larger_length, smaller, smaller_left_boundary, smaller_length);
            temp_temp = larger;
            larger = smaller;
            larger_left_boundary = smaller_left_boundary;
            larger_length = smaller_length;
            smaller = temp;
            smaller_left_boundary = get_preceding_zeros(smaller, smaller_left_boundary, smaller_length);
            smaller_length = maximum_length - smaller_left_boundary;
            temp = temp_temp;
        }
        System.arraycopy(larger, larger_left_boundary, result, result.length - larger_length, larger_length);
    }

    public static void gcd(final byte[] result, final int result_s, final int result_length, final byte[] a, final int a_s, final int a_length, final byte[] b, final int b_s, final int b_length) {
        final int compare_value = compare(a, a_s, a_length, b, b_s, b_length);
        if (0 == compare_value) {
            int target_length = Math.min(result_length, Math.min(a_length, b_length));
            System.arraycopy(a, a_s + a_length - target_length, result, result_s + result_length - target_length, target_length);
            return;
        }
        final int maximum_length = Math.max(a_length, b_length);
        byte[] larger = new byte[maximum_length];
        byte[] smaller = new byte[maximum_length];
        if (0 < compare_value) {
            System.arraycopy(a, a_s, larger, maximum_length - a_length, a_length);
            System.arraycopy(b, b_s, smaller, maximum_length - b_length, b_length);
        } else {
            System.arraycopy(b, b_s, larger, maximum_length - b_length, b_length);
            System.arraycopy(a, a_s, smaller, maximum_length - a_length, a_length);
        }
        byte[] temp = new byte[maximum_length], temp_temp;
        int larger_left_boundary = get_preceding_zeros(larger, 0, maximum_length);
        int larger_length = maximum_length - larger_left_boundary;
        int smaller_left_boundary = get_preceding_zeros(smaller, 0, maximum_length);
        int smaller_length = maximum_length - smaller_left_boundary;
        while (0 != compare(smaller, smaller_left_boundary, smaller_length, decimal_zero, 0, decimal_zero.length)) {
            Arrays.fill(temp, smaller_left_boundary, maximum_length, (byte) 0);
            modulo(temp, smaller_left_boundary, smaller_length, larger, larger_left_boundary, larger_length, smaller, smaller_left_boundary, smaller_length);
            temp_temp = larger;
            larger = smaller;
            larger_left_boundary = smaller_left_boundary;
            larger_length = smaller_length;
            smaller = temp;
            smaller_left_boundary = get_preceding_zeros(smaller, smaller_left_boundary, smaller_length);
            smaller_length = maximum_length - smaller_left_boundary;
            temp = temp_temp;
        }
        System.arraycopy(larger, larger_left_boundary, result, result_s + result_length - larger_length, larger_length);
    }

    public static void gcd(final int[] result, final int[] a, final int[] b) {
        final int compare_value = compare(a, b);
        if (0 == compare_value) {
            int target_length = Math.min(result.length, Math.min(a.length, b.length));
            System.arraycopy(a, a.length - target_length, result, result.length - target_length, target_length);
            return;
        }
        final int maximum_length = Math.max(a.length, b.length);
        int[] larger = new int[maximum_length];
        int[] smaller = new int[maximum_length];
        if (0 < compare_value) {
            System.arraycopy(a, 0, larger, maximum_length - a.length, a.length);
            System.arraycopy(b, 0, smaller, maximum_length - b.length, b.length);
        } else {
            System.arraycopy(b, 0, larger, maximum_length - b.length, b.length);
            System.arraycopy(a, 0, smaller, maximum_length - a.length, a.length);
        }
        int[] temp = new int[maximum_length], temp_temp;
        int larger_left_boundary = get_preceding_zeros(larger, 0, maximum_length);
        int larger_length = maximum_length - larger_left_boundary;
        int smaller_left_boundary = get_preceding_zeros(smaller, 0, maximum_length);
        int smaller_length = maximum_length - smaller_left_boundary;
        while (0 != compare(smaller, smaller_left_boundary, smaller_length, tight_zero, 0, tight_zero.length)) {
            Arrays.fill(temp, smaller_left_boundary, maximum_length, 0);
            modulo(temp, smaller_left_boundary, smaller_length, larger, larger_left_boundary, larger_length, smaller, smaller_left_boundary, smaller_length);
            temp_temp = larger;
            larger = smaller;
            larger_left_boundary = smaller_left_boundary;
            larger_length = smaller_length;
            smaller = temp;
            smaller_left_boundary = get_preceding_zeros(smaller, smaller_left_boundary, smaller_length);
            smaller_length = maximum_length - smaller_left_boundary;
            temp = temp_temp;
        }
        System.arraycopy(larger, larger_left_boundary, result, result.length - larger_length, larger_length);
    }

    public static void gcd(final int[] result, final int result_s, final int result_length, final int[] a, final int a_s, final int a_length, final int[] b, final int b_s, final int b_length) {
        final int compare_value = compare(a, a_s, a_length, b, b_s, b_length);
        if (0 == compare_value) {
            int target_length = Math.min(result_length, Math.min(a_length, b_length));
            System.arraycopy(a, a_s + a_length - target_length, result, result_s + result_length - target_length, target_length);
            return;
        }
        final int maximum_length = Math.max(a_length, b_length);
        int[] larger = new int[maximum_length];
        int[] smaller = new int[maximum_length];
        if (0 < compare_value) {
            System.arraycopy(a, a_s, larger, maximum_length - a_length, a_length);
            System.arraycopy(b, b_s, smaller, maximum_length - b_length, b_length);
        } else {
            System.arraycopy(b, b_s, larger, maximum_length - b_length, b_length);
            System.arraycopy(a, a_s, smaller, maximum_length - a_length, a_length);
        }
        int[] temp = new int[maximum_length], temp_temp;
        int larger_left_boundary = get_preceding_zeros(larger, 0, maximum_length);
        int larger_length = maximum_length - larger_left_boundary;
        int smaller_left_boundary = get_preceding_zeros(smaller, 0, maximum_length);
        int smaller_length = maximum_length - smaller_left_boundary;
        while (0 != compare(smaller, smaller_left_boundary, smaller_length, tight_zero, 0, tight_zero.length)) {
            Arrays.fill(temp, smaller_left_boundary, maximum_length, 0);
            modulo(temp, smaller_left_boundary, smaller_length, larger, larger_left_boundary, larger_length, smaller, smaller_left_boundary, smaller_length);
            temp_temp = larger;
            larger = smaller;
            larger_left_boundary = smaller_left_boundary;
            larger_length = smaller_length;
            smaller = temp;
            smaller_left_boundary = get_preceding_zeros(smaller, smaller_left_boundary, smaller_length);
            smaller_length = maximum_length - smaller_left_boundary;
            temp = temp_temp;
        }
        System.arraycopy(larger, larger_left_boundary, result, result_s + result_length - larger_length, larger_length);
    }
}
