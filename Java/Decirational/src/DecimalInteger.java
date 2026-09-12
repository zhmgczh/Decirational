package decirational;

import java.util.Objects;
import java.util.Arrays;

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

    public static DecimalInteger getDigit(int index) {
        if (index < 0 || index > 9) {
            throw new IllegalArgumentException("the index must be between 0 and 9");
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
            throw new NullPointerException("input byte array cannot be null");
        }
        if (0 == digits.length) {
            throw new IllegalArgumentException("input byte array cannot be empty");
        }
        for (byte digit : digits) {
            if (digit < 0 || digit > 9) {
                throw new NumberFormatException("input byte array is not a valid decimal integer");
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
        this(tight_integer.toDecimalInteger());
    }

    public DecimalInteger(String number) {
        if (null == number) {
            throw new NumberFormatException("input is null");
        }
        number = number.replaceAll("\\s", "");
        if (number.isEmpty()) {
            throw new NumberFormatException("input is empty");
        }
        int starting_point = 0;
        boolean negative = false;
        if (Arithmetic.isMinus(number.charAt(0))) {
            negative = true;
            starting_point = 1;
        } else if (Arithmetic.isPlus(number.charAt(0))) {
            starting_point = 1;
        }
        if (starting_point == number.length()) {
            throw new NumberFormatException("input \"" + number + "\" has no digits");
        }
        for (int i = starting_point; i < number.length(); ++i) {
            if (!Arithmetic.isDigit(number.charAt(i))) {
                throw new NumberFormatException("input \"" + number + "\" is invalid and cannot be parsed as a decimal integer");
            }
        }
        final byte[] digits = new byte[number.length() - starting_point];
        for (int i = starting_point; i < number.length(); ++i) {
            digits[i - starting_point] = Arithmetic.toByte(number.charAt(i));
        }
        this.digits = Arithmetic.optimize(digits);
        this.negative = Arithmetic.optimize(negative, this.digits);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(negative ? '-' : "");
        for (byte digit : digits) {
            sb.append(Arithmetic.toChar(digit));
        }
        return sb.toString();
    }

    public TightInteger toTightInteger() {
        int tight_length = (int) (digits.length * Arithmetic.decimal_to_tight_length_ratio + 1) + 1;
        int[] integer = new int[tight_length];
        Arithmetic.convertDecimalToTight(integer, digits);
        return new TightInteger(integer, negative);
    }

    @Override
    public boolean isZero() {
        return 1 == digits.length && 0 == digits[0];
    }

    @Override
    public boolean isOne() {
        return !negative && 1 == digits.length && 1 == digits[0];
    }

    @Override
    public boolean isUnitAbs() {
        return 1 == digits.length && 1 == digits[0];
    }

    @Override
    public boolean isPositive() {
        return !negative && !isZero();
    }

    @Override
    public boolean isNegative() {
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

    private DecimalInteger plusRaw(DecimalInteger other) {
        final byte[] digits = Arithmetic.expand(this.digits, Math.max(this.digits.length, other.digits.length) + 1);
        Arithmetic.add(digits, other.digits);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger plus(final DecimalInteger other) {
        if (isZero()) {
            return other;
        }
        if (other.isZero()) {
            return this;
        }
        if (negative != other.negative) {
            return minusRaw(other.negate());
        }
        return plusRaw(other);
    }

    private DecimalInteger minusRaw(DecimalInteger other) {
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
        if (isZero()) {
            return other.negate();
        }
        if (other.isZero()) {
            return this;
        }
        if (negative != other.negative) {
            return plusRaw(other.negate());
        }
        return minusRaw(other);
    }

    @Override
    public DecimalInteger multiply(final DecimalInteger other) {
        if (isZero() || other.isZero()) {
            return ZERO;
        }
        if (isUnitAbs()) {
            if (isPositive()) {
                return other;
            } else {
                return other.negate();
            }
        }
        if (other.isUnitAbs()) {
            if (other.isPositive()) {
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
    public DecimalInteger multiplyBase(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("multiplication times cannot be negative");
        } else if (0 == times) {
            return this;
        } else if (isZero()) {
            return ZERO;
        }
        final byte[] digits = new byte[this.digits.length + times];
        System.arraycopy(this.digits, 0, digits, 0, this.digits.length);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger multiplyBase() {
        return multiplyBase(1);
    }

    @Override
    public DecimalInteger divideByBase(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("division times cannot be negative");
        } else if (0 == times) {
            return this;
        } else if (isZero() || times >= this.digits.length) {
            return ZERO;
        }
        final byte[] digits = new byte[this.digits.length - times];
        System.arraycopy(this.digits, 0, digits, 0, digits.length);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger divideByBase() {
        return divideByBase(1);
    }

    @Override
    public DecimalInteger divideBy(final DecimalInteger other) {
        if (other.isZero()) {
            throw new ArithmeticException("cannot divide by zero");
        }
        if (isZero()) {
            return ZERO;
        }
        if (other.isUnitAbs()) {
            if (other.isPositive()) {
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
        if (other.isZero()) {
            throw new ArithmeticException("cannot divide by zero");
        }
        if (isZero() || other.isUnitAbs()) {
            return ZERO;
        }
        final byte[] digits = new byte[other.digits.length];
        Arithmetic.modulo(digits, this.digits, other.digits);
        return new DecimalInteger(digits, negative, true);
    }

    @Override
    public DecimalInteger[] divideByAndModulo(final DecimalInteger other) {
        if (other.isZero()) {
            throw new ArithmeticException("cannot divide by zero");
        }
        if (isZero()) {
            return new DecimalInteger[]{ZERO, ZERO};
        }
        if (other.isUnitAbs()) {
            if (other.isPositive()) {
                return new DecimalInteger[]{this, ZERO};
            } else {
                return new DecimalInteger[]{negate(), ZERO};
            }
        }
        final byte[] quotient_digits = new byte[digits.length];
        final byte[] remainder_digits = new byte[other.digits.length];
        Arithmetic.divideAndModulo(quotient_digits, remainder_digits, digits, other.digits);
        final DecimalInteger quotient = new DecimalInteger(quotient_digits, negative != other.negative, true);
        final DecimalInteger remainder = new DecimalInteger(remainder_digits, negative, true);
        return new DecimalInteger[]{quotient, remainder};
    }

    @Override
    public DecimalInteger gcd(final DecimalInteger other) {
        if (isZero()) {
            return other.abs();
        }
        if (other.isZero()) {
            return abs();
        }
        if (isUnitAbs() || other.isUnitAbs()) {
            return ONE;
        }
        final byte[] digits = new byte[Math.min(this.digits.length, other.digits.length)];
        Arithmetic.gcd(digits, this.digits, other.digits);
        return new DecimalInteger(digits, false, true);
    }

    @Override
    public DecimalInteger lcm(final DecimalInteger other) {
        return divideBy(gcd(other)).multiply(other);
    }

    @Override
    public DecimalInteger pow(final int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent cannot be negative");
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
}