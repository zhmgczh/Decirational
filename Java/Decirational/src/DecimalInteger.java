import java.util.Arrays;

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
    public static final DecimalInteger[] numbers = new DecimalInteger[]{ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE};

    public DecimalInteger(DecimalInteger decimal_integer) {
        negative = decimal_integer.negative;
        digits = Arrays.copyOf(decimal_integer.digits, decimal_integer.digits.length);
    }

    private DecimalInteger() {
        negative = false;
        digits = new byte[1];
    }

    private DecimalInteger(byte[] digits, boolean negative) {
        this.digits = Arithmetic.optimize(digits);
        this.negative = Arithmetic.optimize(negative, this.digits);
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
        byte[] digits = new byte[number.length() - starting_point];
        for (int i = starting_point; i < number.length(); ++i) {
            digits[i - starting_point] = Arithmetic.to_byte(number.charAt(i));
        }
        this(digits, negative);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(negative ? '-' : "");
        for (byte digit : digits) {
            sb.append(Arithmetic.to_char(digit));
        }
        return sb.toString();
    }

    public boolean is_zero() {
        return 1 == digits.length && digits[0] == 0;
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
        for (int i = 0; i < digits.length; ++i) {
            if (digits[i] < other.digits[i]) {
                return negative ? 1 : -1;
            } else if (digits[i] > other.digits[i]) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }


    public DecimalInteger plus(DecimalInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return minus(other.negate());
        }
        byte[] digits = Arithmetic.expand(this.digits, Math.max(this.digits.length, other.digits.length) + 1);
        Arithmetic.add(digits, other.digits);
        return new DecimalInteger(digits, this.negative);
    }

    public DecimalInteger minus(DecimalInteger other) {
        if (other.is_zero()) {
            return this;
        }
        if (negative != other.negative) {
            return plus(other.negate());
        }
        int compare_to = abs().compareTo(other.abs());
        DecimalInteger a, b;
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
        byte[] digits = Arrays.copyOf(a.digits, a.digits.length);
        Arithmetic.subtract(digits, b.digits);
        return new DecimalInteger(digits, negative);
    }

    public DecimalInteger multiply(DecimalInteger other) {
        byte[] digits = new byte[this.digits.length + other.digits.length];
        Arithmetic.multiply(digits, this.digits, other.digits);
        return new DecimalInteger(digits, this.negative != other.negative);
    }

    private byte upper_multiplier(DecimalInteger dividend, DecimalInteger divisor) {
        int left = 0;
        int right = 9;
        while (left < right) {
            int mid = (left + right + 1) >> 1;
            DecimalInteger multiplier = numbers[mid];
            if (divisor.multiply(multiplier).compareTo(dividend) <= 0) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }
        return (byte) left;
    }

    public DecimalInteger multiply_10(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Multiplication times cannot be negative!");
        }
        byte[] digits = new byte[this.digits.length + times];
        System.arraycopy(this.digits, 0, digits, 0, this.digits.length);
        return new DecimalInteger(digits, this.negative);
    }

    public DecimalInteger multiply_10() {
        return multiply_10(1);
    }

    public DecimalInteger divide_by_10(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Division times cannot be negative!");
        } else if (times >= this.digits.length) {
            return ZERO;
        }
        byte[] digits = new byte[this.digits.length - times];
        System.arraycopy(this.digits, 0, digits, 0, digits.length);
        return new DecimalInteger(digits, this.negative);
    }

    public DecimalInteger divide_by_10() {
        return divide_by_10(1);
    }

    public DecimalInteger divide_by(DecimalInteger other) {
        if (other.is_zero()) {
            throw new ArithmeticException("Cannot divide by zero!");
        }
        byte[] digits = new byte[this.digits.length];
        DecimalInteger remaining_dividend = abs();
        DecimalInteger divisor = other.abs().multiply_10(digits.length);
        for (int i = 0; i < digits.length; ++i) {
            divisor = divisor.divide_by_10();
            byte multiplier = upper_multiplier(remaining_dividend, divisor);
            digits[i] = multiplier;
            remaining_dividend = remaining_dividend.minus(divisor.multiply(numbers[multiplier]));
        }
        return new DecimalInteger(digits, this.negative != other.negative);
    }

    public DecimalInteger mod(DecimalInteger other) {
        return minus(divide_by(other).multiply(other));
    }
}
