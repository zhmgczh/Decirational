import java.util.Arrays;
import java.util.Scanner;

public class DecimalInteger implements Comparable<DecimalInteger> {
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

    public DecimalInteger(final DecimalInteger decimal_integer) {
        negative = decimal_integer.negative;
        digits = Arrays.copyOf(decimal_integer.digits, decimal_integer.digits.length);
    }

    private DecimalInteger() {
        negative = false;
        digits = new byte[1];
    }

    public DecimalInteger(final byte[] digits, final boolean negative) {
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

    public DecimalInteger(final LargeInteger large_integer) {
        this(large_integer.to_decimal_integer());
    }

    public DecimalInteger(String number) {
        number = number.trim();
        int starting_point = 0;
        boolean negative = false;
        if (Arithmetic.is_minus(number.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (Arithmetic.is_plus(number.charAt(0))) {
            starting_point = 1;
        }
        for (int i = starting_point; i < number.length(); ++i) {
            if (!Arithmetic.is_digit(number.charAt(i))) {
                throw new NumberFormatException("Your input " + number + " is invalid and cannot be converted to DecimalInteger.");
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

    public LargeInteger to_large_integer() {
        int tight_length = (int) (digits.length * Arithmetic.decimal_to_tight_length_ratio + 1);
        int[] integer = new int[tight_length];
        Arithmetic.convert_decimal_to_tight(integer, digits);
        return new LargeInteger(integer, negative);
    }

    public boolean is_zero() {
        return 1 == digits.length && 0 == digits[0];
    }

    public boolean is_positive() {
        return !negative && !is_zero();
    }

    public boolean is_negative() {
        return negative;
    }

    public DecimalInteger negate() {
        return new DecimalInteger(digits, !negative);
    }

    public DecimalInteger abs() {
        return new DecimalInteger(digits, false);
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


    public DecimalInteger plus(final DecimalInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return minus(other.negate());
        }
        final byte[] digits = Arithmetic.expand(this.digits, Math.max(this.digits.length, other.digits.length) + 1);
        Arithmetic.add(digits, other.digits);
        return new DecimalInteger(digits, this.negative);
    }

    public DecimalInteger minus(final DecimalInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return plus(other.negate());
        }
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
        return new DecimalInteger(digits, negative);
    }

    public DecimalInteger multiply(final DecimalInteger other) {
        final byte[] digits = new byte[this.digits.length + other.digits.length];
        Arithmetic.multiply(digits, this.digits, other.digits);
        return new DecimalInteger(digits, this.negative != other.negative);
    }

    public DecimalInteger multiply_10(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        }
        final byte[] digits = new byte[this.digits.length + times];
        System.arraycopy(this.digits, 0, digits, 0, this.digits.length);
        return new DecimalInteger(digits, this.negative);
    }

    public DecimalInteger multiply_10() {
        return multiply_10(1);
    }

    public DecimalInteger divide_by_10(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (times >= this.digits.length) {
            return ZERO;
        }
        final byte[] digits = new byte[this.digits.length - times];
        System.arraycopy(this.digits, 0, digits, 0, digits.length);
        return new DecimalInteger(digits, this.negative);
    }

    public DecimalInteger divide_by_10() {
        return divide_by_10(1);
    }

    public DecimalInteger divide_by(final DecimalInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        final byte[] digits = new byte[this.digits.length];
        Arithmetic.divide(digits, this.digits, other.digits);
        return new DecimalInteger(digits, this.negative != other.negative);
    }

    public DecimalInteger modulo(final DecimalInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        final byte[] digits = new byte[other.digits.length];
        Arithmetic.modulo(digits, this.digits, other.digits);
        return new DecimalInteger(digits, this.negative != other.negative);
    }

    public DecimalInteger[] divide_by_and_modulo(final DecimalInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        final byte[] quotient_digits = new byte[this.digits.length];
        final byte[] remainder_digits = new byte[other.digits.length];
        Arithmetic.divide_and_modulo(quotient_digits, remainder_digits, this.digits, other.digits);
        final DecimalInteger quotient = new DecimalInteger(quotient_digits, this.negative != other.negative);
        final DecimalInteger remainder = new DecimalInteger(remainder_digits, this.negative != other.negative);
        return new DecimalInteger[]{quotient, remainder};
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
        }
    }
}