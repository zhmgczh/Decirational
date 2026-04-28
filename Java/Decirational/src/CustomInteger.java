public interface CustomInteger<T extends CustomInteger<T>> extends Comparable<T> {
    boolean is_zero();

    boolean is_one();

    boolean is_unit_abs();

    boolean is_positive();

    boolean is_negative();

    T negate();

    T abs();

    T plus(final T other);

    T minus(final T other);

    T multiply(final T other);

    T multiply_base(final int times);

    T multiply_base();

    T divide_by_base(final int times);

    T divide_by_base();

    T divide_by(final T other);

    T modulo(final T other);

    T gcd(final T other);

    T lcm(final T other);

    T pow(final int exponent);

    T[] divide_by_and_modulo(final T other);
}