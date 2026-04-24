public interface CustomInteger<T extends CustomInteger<T>> extends Comparable<T> {
    T get_zero();

    T get_one();

    boolean is_zero();

    boolean is_unit_abs();

    boolean is_positive();

    boolean is_negative();

    T negate();

    T abs();

    T plus(final T other);

    T minus(final T other);

    T multiply(final T other);

    T divide_by(final T other);

    T modulo(final T other);

    T gcd(final T other);

    T lcm(final T other);

    T pow(final int exponent);

    T[] divide_by_and_modulo(final T other);
}