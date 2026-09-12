package decirational;

public interface CustomInteger<T extends CustomInteger<T>> extends Comparable<T> {
    boolean isZero();

    boolean isOne();

    boolean isUnitAbs();

    boolean isPositive();

    boolean isNegative();

    T negate();

    T abs();

    T plus(final T other);

    T minus(final T other);

    T multiply(final T other);

    T multiplyBase(final int times);

    T multiplyBase();

    T divideByBase(final int times);

    T divideByBase();

    T divideBy(final T other);

    T modulo(final T other);

    T gcd(final T other);

    T lcm(final T other);

    T pow(final int exponent);

    T[] divideByAndModulo(final T other);
}