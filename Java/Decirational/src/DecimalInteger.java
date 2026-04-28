import java.util.Objects;
import java.util.Arrays;
import java.util.Scanner;

public final class DecimalInteger implements CustomInteger<DecimalInteger> {
    private final boolean negative;
    private final byte[] digits;
    public static final DecimalInteger ZERO = new DecimalInteger();
    public static final DecimalInteger ONE = new DecimalInteger("1");
    public static final DecimalInteger TWO = new DecimalInteger("2");
    public static final DecimalInteger THREE = new DecimalInteger("3");
    public static final DecimalInteger FOUR = new DecimalInteger("4");
    public static final DecimalInteger FIVE = new DecimalInteger("5");
    public static final DecimalInteger SIX = new DecimalInteger("6");
    public static final DecimalInteger SEVEN = new DecimalInteger("7");
    public static final DecimalInteger EIGHT = new DecimalInteger("8");
    public static final DecimalInteger NINE = new DecimalInteger("9");
    private static final DecimalInteger[] numbers = new DecimalInteger[]{ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE};

    public static DecimalInteger get_digit(int index) {
        if (index < 0 || index > 9) {
            throw new IllegalArgumentException("The index must be between 0 and 9.");
        }
        return numbers[index];
    }

    public DecimalInteger(final DecimalInteger decimal_integer) {
        negative = decimal_integer.negative;
        digits = decimal_integer.digits;
    }

    private DecimalInteger() {
        negative = false;
        digits = new byte[1];
    }

    public DecimalInteger(final byte[] digits, final boolean negative) {
        if (null == digits) {
            throw new NullPointerException("Input byte array cannot be null.");
        }
        if (0 == digits.length) {
            throw new IllegalArgumentException("Input byte array cannot be empty.");
        }
        for (byte digit : digits) {
            if (digit < 0 || digit > 9) {
                throw new NumberFormatException("Input byte array is not a valid decimal integer.");
            }
        }
        byte[] new_digits = Arithmetic.optimize(digits);
        if (new_digits == digits) {
            new_digits = Arrays.copyOf(new_digits, new_digits.length);
        }
        this.digits = new_digits;
        this.negative = Arithmetic.optimize(negative, this.digits);
    }

    private DecimalInteger(final byte[] digits, final boolean negative, boolean unsafe) {
        this.digits = Arithmetic.optimize(digits);
        this.negative = Arithmetic.optimize(negative, this.digits);
    }

    public DecimalInteger(final byte number) {
        this(Byte.toString(number));
    }

    public DecimalInteger(final short number) {
        this(Short.toString(number));
    }

    public DecimalInteger(final int number) {
        this(Integer.toString(number));
    }

    public DecimalInteger(final long number) {
        this(Long.toString(number));
    }

    public DecimalInteger(final TightInteger tight_integer) {
        this(tight_integer.to_decimal_integer());
    }

    public DecimalInteger(String number) {
        if (null == number) {
            throw new NumberFormatException("Input is null.");
        }
        number = number.replaceAll("\\s", "");
        if (number.isEmpty()) {
            throw new NumberFormatException("Input is empty.");
        }
        int starting_point = 0;
        boolean negative = false;
        if (Arithmetic.is_minus(number.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (Arithmetic.is_plus(number.charAt(0))) {
            starting_point = 1;
        }
        if (starting_point == number.length()) {
            throw new NumberFormatException("Input \"" + number + "\" has no digits.");
        }
        for (int i = starting_point; i < number.length(); ++i) {
            if (!Arithmetic.is_digit(number.charAt(i))) {
                throw new NumberFormatException("Input \"" + number + "\" is invalid and cannot be parsed as a decimal integer.");
            }
        }
        final byte[] digits = new byte[number.length() - starting_point];
        for (int i = starting_point; i < number.length(); ++i) {
            digits[i - starting_point] = Arithmetic.to_byte(number.charAt(i));
        }
        this.digits = Arithmetic.optimize(digits);
        this.negative = Arithmetic.optimize(negative, this.digits);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(negative ? '-' : "");
        for (byte digit : digits) {
            sb.append(Arithmetic.to_char(digit));
        }
        return sb.toString();
    }

    public TightInteger to_tight_integer() {
        int tight_length = (int) (digits.length * Arithmetic.decimal_to_tight_length_ratio + 1) + 1;
        int[] integer = new int[tight_length];
        Arithmetic.convert_decimal_to_tight(integer, digits);
        return new TightInteger(integer, negative);
    }

    @Override
    public boolean is_zero() {
        return 1 == digits.length && 0 == digits[0];
    }

    @Override
    public boolean is_one() {
        return !negative && 1 == digits.length && 1 == digits[0];
    }

    @Override
    public boolean is_unit_abs() {
        return 1 == digits.length && 1 == digits[0];
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
    public DecimalInteger negate() {
        return new DecimalInteger(digits, !negative, true);
    }

    @Override
    public DecimalInteger abs() {
        return new DecimalInteger(digits, false, true);
    }

    @Override
    public int compareTo(final DecimalInteger other) {
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
        for (int i = 0; i < digits.length; ++i) {
            if (digits[i] < other.digits[i]) {
                return negative ? 1 : -1;
            } else if (digits[i] > other.digits[i]) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DecimalInteger other = (DecimalInteger) o;
        return negative == other.negative && Arrays.equals(digits, other.digits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(negative, Arrays.hashCode(digits));
    }

    private DecimalInteger plus_raw(DecimalInteger other) {
        final byte[] digits = Arithmetic.expand(this.digits, Math.max(this.digits.length, other.digits.length) + 1);
        Arithmetic.add(digits, other.digits);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger plus(final DecimalInteger other) {
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

    private DecimalInteger minus_raw(DecimalInteger other) {
        final int compare_to = abs().compareTo(other.abs());
        final DecimalInteger a, b;
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
        final byte[] digits = Arrays.copyOf(a.digits, a.digits.length);
        Arithmetic.subtract(digits, b.digits);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger minus(final DecimalInteger other) {
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
    public DecimalInteger multiply(final DecimalInteger other) {
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
        final byte[] digits = new byte[this.digits.length + other.digits.length];
        Arithmetic.multiply(digits, this.digits, other.digits);
        return new DecimalInteger(digits, negative != other.negative, true);
    }

    @Override
    public DecimalInteger multiply_base(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        } else if (0 == times) {
            return this;
        } else if (is_zero()) {
            return ZERO;
        }
        final byte[] digits = new byte[this.digits.length + times];
        System.arraycopy(this.digits, 0, digits, 0, this.digits.length);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger multiply_base() {
        return multiply_base(1);
    }

    @Override
    public DecimalInteger divide_by_base(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (0 == times) {
            return this;
        } else if (is_zero() || times >= this.digits.length) {
            return ZERO;
        }
        final byte[] digits = new byte[this.digits.length - times];
        System.arraycopy(this.digits, 0, digits, 0, digits.length);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger divide_by_base() {
        return divide_by_base(1);
    }

    @Override
    public DecimalInteger divide_by(final DecimalInteger other) {
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
        final byte[] digits = new byte[this.digits.length];
        Arithmetic.divide(digits, this.digits, other.digits);
        return new DecimalInteger(digits, negative != other.negative, true);
    }

    @Override
    public DecimalInteger modulo(final DecimalInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        if (is_zero() || other.is_unit_abs()) {
            return ZERO;
        }
        final byte[] digits = new byte[other.digits.length];
        Arithmetic.modulo(digits, this.digits, other.digits);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger[] divide_by_and_modulo(final DecimalInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        if (is_zero()) {
            return new DecimalInteger[]{ZERO, ZERO};
        }
        if (other.is_unit_abs()) {
            if (other.is_positive()) {
                return new DecimalInteger[]{this, ZERO};
            } else {
                return new DecimalInteger[]{negate(), ZERO};
            }
        }
        final byte[] quotient_digits = new byte[digits.length];
        final byte[] remainder_digits = new byte[other.digits.length];
        Arithmetic.divide_and_modulo(quotient_digits, remainder_digits, digits, other.digits);
        final DecimalInteger quotient = new DecimalInteger(quotient_digits, negative != other.negative, true);
        final DecimalInteger remainder = new DecimalInteger(remainder_digits, negative, true);
        return new DecimalInteger[]{quotient, remainder};
    }

    @Override
    public DecimalInteger gcd(final DecimalInteger other) {
        if (is_zero()) {
            return other;
        }
        if (other.is_zero()) {
            return this;
        }
        if (is_unit_abs() || other.is_unit_abs()) {
            return ONE;
        }
        final byte[] digits = new byte[Math.min(this.digits.length, other.digits.length)];
        Arithmetic.gcd(digits, this.digits, other.digits);
        return new DecimalInteger(digits, false, true);
    }

    @Override
    public DecimalInteger lcm(final DecimalInteger other) {
        return divide_by(gcd(other)).multiply(other);
    }

    @Override
    public DecimalInteger pow(final int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("Exponent cannot be negative!");
        } else if (0 == exponent) {
            return ONE;
        } else if (1 == exponent) {
            return this;
        }
        DecimalInteger result = ONE;
        DecimalInteger base = this;
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
            final DecimalInteger a = new DecimalInteger(a_str);
            final DecimalInteger b = new DecimalInteger(b_str);
            System.out.println(a);
            System.out.println(b);
            System.out.println(a.compareTo(b));
            System.out.println(a.plus(b));
            System.out.println(a.minus(b));
            System.out.println(a.multiply(b));
            System.out.println(a.divide_by(b));
            System.out.println(a.modulo(b));
            System.out.println(a.gcd(b));
            System.out.println(a.lcm(b));
            System.out.println(a.pow(2));
        }
    }
}