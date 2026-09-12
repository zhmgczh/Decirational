package decirational;

import java.util.Objects;
import java.util.Arrays;

public final class TightInteger implements CustomInteger<TightInteger> {
    private final boolean negative;
    private final int[] integer;
    public static final TightInteger ZERO = new TightInteger();
    public static final TightInteger ONE = new TightInteger(1);
    private static final TightInteger[] numbers = new TightInteger[]{ZERO, ONE};

    public static TightInteger getDigit(int index) {
        if (index < 0 || index > 1) {
            throw new IllegalArgumentException("the index must be either 0 or 1");
        }
        return numbers[index];
    }

    public TightInteger(final TightInteger tight_integer) {
        negative = tight_integer.negative;
        integer = tight_integer.integer;
    }

    private TightInteger() {
        negative = false;
        integer = new int[1];
    }

    public TightInteger(final int[] integer, final boolean negative) {
        if (null == integer) {
            throw new NullPointerException("input int array cannot be null");
        }
        if (0 == integer.length) {
            throw new IllegalArgumentException("input int array cannot be empty");
        }
        int[] new_integer = Arithmetic.optimize(integer);
        if (new_integer == integer) {
            new_integer = Arrays.copyOf(new_integer, new_integer.length);
        }
        this.integer = new_integer;
        this.negative = Arithmetic.optimize(negative, this.integer);
    }

    private TightInteger(final int[] integer, final boolean negative, boolean unsafe) {
        this.integer = Arithmetic.optimize(integer);
        this.negative = Arithmetic.optimize(negative, this.integer);
    }

    public TightInteger(final byte number) {
        this((int) number);
    }

    public TightInteger(final short number) {
        this((int) number);
    }

    public TightInteger(final int number) {
        final long b = Arithmetic.reverseNegative(number);
        final int[] integer = new int[1];
        integer[0] = (int) b;
        this.integer = integer;
        negative = b != number;
    }

    public TightInteger(final long number) {
        this(Long.toString(number));
    }

    public TightInteger(final DecimalInteger decimal_integer) {
        this(decimal_integer.toTightInteger());
    }

    public TightInteger(final String number) {
        this(new DecimalInteger(number));
    }

    @Override
    public String toString() {
        return toDecimalInteger().toString();
    }

    public DecimalInteger toDecimalInteger() {
        int significant_estimate = (int) (integer.length * Arithmetic.tight_to_decimal_length_ratio + 1) + 1;
        // convertTightToDecimal writes DECIMAL_CHUNK_DIGITS digits per division, so the buffer must hold a whole
        // number of those chunks - rounded up from the safe estimate above, never down, so it's still large
        // enough for every significant digit.
        int chunks = (significant_estimate + Arithmetic.DECIMAL_CHUNK_DIGITS - 1) / Arithmetic.DECIMAL_CHUNK_DIGITS;
        byte[] digits = new byte[chunks * Arithmetic.DECIMAL_CHUNK_DIGITS];
        Arithmetic.convertTightToDecimal(digits, integer);
        return new DecimalInteger(digits, negative);
    }

    @Override
    public boolean isZero() {
        return 1 == integer.length && 0 == integer[0];
    }

    @Override
    public boolean isOne() {
        return !negative && 1 == integer.length && 1 == integer[0];
    }

    @Override
    public boolean isUnitAbs() {
        return 1 == integer.length && 1 == integer[0];
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
    public TightInteger negate() {
        return new TightInteger(integer, !negative, true);
    }

    @Override
    public TightInteger abs() {
        return new TightInteger(integer, false, true);
    }

    @Override
    public int compareTo(final TightInteger other) {
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
        for (int i = 0; i < integer.length; ++i) {
            if ((integer[i] & 0xffffffffL) < (other.integer[i] & 0xffffffffL)) {
                return negative ? 1 : -1;
            } else if ((integer[i] & 0xffffffffL) > (other.integer[i] & 0xffffffffL)) {
                return negative ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TightInteger other = (TightInteger) o;
        return negative == other.negative && Arrays.equals(integer, other.integer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(negative, Arrays.hashCode(integer));
    }

    private TightInteger plusRaw(TightInteger other) {
        final int[] integer = Arithmetic.expand(this.integer, Math.max(this.integer.length, other.integer.length) + 1);
        Arithmetic.add(integer, other.integer);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger plus(final TightInteger other) {
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

    private TightInteger minusRaw(TightInteger other) {
        final int compare_to = abs().compareTo(other.abs());
        final TightInteger a, b;
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
        final int[] integer = Arrays.copyOf(a.integer, a.integer.length);
        Arithmetic.subtract(integer, b.integer);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger minus(final TightInteger other) {
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
    public TightInteger multiply(final TightInteger other) {
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
        final int[] integer = new int[this.integer.length + other.integer.length];
        Arithmetic.multiply(integer, this.integer, other.integer);
        return new TightInteger(integer, negative != other.negative, true);
    }

    @Override
    public TightInteger multiplyBase(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("multiplication times cannot be negative");
        } else if (0 == times) {
            return this;
        } else if (isZero()) {
            return ZERO;
        }
        final int[] integer = new int[this.integer.length + times];
        System.arraycopy(this.integer, 0, integer, 0, this.integer.length);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger multiplyBase() {
        return multiplyBase(1);
    }

    @Override
    public TightInteger divideByBase(final int times) {
        if (times < 0) {
            throw new IllegalArgumentException("division times cannot be negative");
        } else if (0 == times) {
            return this;
        } else if (isZero() || times >= this.integer.length) {
            return ZERO;
        }
        final int[] integer = new int[this.integer.length - times];
        System.arraycopy(this.integer, 0, integer, 0, integer.length);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger divideByBase() {
        return divideByBase(1);
    }

    @Override
    public TightInteger divideBy(final TightInteger other) {
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
        final int[] integer = new int[this.integer.length];
        Arithmetic.divide(integer, this.integer, other.integer);
        return new TightInteger(integer, negative != other.negative, true);
    }

    @Override
    public TightInteger modulo(final TightInteger other) {
        if (other.isZero()) {
            throw new ArithmeticException("cannot divide by zero");
        }
        if (isZero() || other.isUnitAbs()) {
            return ZERO;
        }
        final int[] integer = new int[other.integer.length];
        Arithmetic.modulo(integer, this.integer, other.integer);
        return new TightInteger(integer, negative, true);
    }

    @Override
    public TightInteger[] divideByAndModulo(final TightInteger other) {
        if (other.isZero()) {
            throw new ArithmeticException("cannot divide by zero");
        }
        if (isZero()) {
            return new TightInteger[]{ZERO, ZERO};
        }
        if (other.isUnitAbs()) {
            if (other.isPositive()) {
                return new TightInteger[]{this, ZERO};
            } else {
                return new TightInteger[]{negate(), ZERO};
            }
        }
        final int[] quotient_integer = new int[integer.length];
        final int[] remainder_integer = new int[other.integer.length];
        Arithmetic.divideAndModulo(quotient_integer, remainder_integer, integer, other.integer);
        final TightInteger quotient = new TightInteger(quotient_integer, negative != other.negative, true);
        final TightInteger remainder = new TightInteger(remainder_integer, negative, true);
        return new TightInteger[]{quotient, remainder};
    }

    @Override
    public TightInteger gcd(final TightInteger other) {
        if (isZero()) {
            return other.abs();
        }
        if (other.isZero()) {
            return abs();
        }
        if (isUnitAbs() || other.isUnitAbs()) {
            return ONE;
        }
        final int[] integer = new int[Math.min(this.integer.length, other.integer.length)];
        Arithmetic.gcd(integer, this.integer, other.integer);
        return new TightInteger(integer, false, true);
    }

    @Override
    public TightInteger lcm(final TightInteger other) {
        return divideBy(gcd(other)).multiply(other);
    }

    @Override
    public TightInteger pow(final int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent cannot be negative");
        } else if (0 == exponent) {
            return ONE;
        } else if (1 == exponent) {
            return this;
        }
        TightInteger result = ONE;
        TightInteger base = this;
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