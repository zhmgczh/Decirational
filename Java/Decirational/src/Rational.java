public class Rational<T extends CustomInteger<T>> {
    private final T numerator;
    private final T denominator;

    public T get_numerator() {
        return numerator;
    }

    public T get_denominator() {
        return denominator;
    }

    public Rational<T> get_zero() {
        return new Rational<>(numerator.get_zero(), denominator.get_one());
    }

    public Rational<T> get_one() {
        return new Rational<>(numerator.get_one(), denominator.get_one());
    }

    public Rational(T numerator, T denominator) {
        T gcd = numerator.gcd(denominator);
        if (denominator.is_negative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator.divide_by(gcd);
        this.denominator = denominator.divide_by(gcd);
    }

    public Rational(final T integer) {
        this.numerator = integer;
        this.denominator = integer.get_one();
    }

    public Rational<T> plus(final Rational<T> other) {
        T denominator = this.denominator.multiply(other.denominator);
        T numerator = this.numerator.multiply(other.denominator).plus(other.numerator.multiply(this.denominator));
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> minus(final Rational<T> other) {
        T denominator = this.denominator.multiply(other.denominator);
        T numerator = this.numerator.multiply(other.denominator).minus(other.numerator.multiply(this.denominator));
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> multiply(final Rational<T> other) {
        T denominator = this.denominator.multiply(other.denominator);
        T numerator = this.numerator.multiply(other.denominator);
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> divide_by(final Rational<T> other) {
        T denominator = this.denominator.multiply(other.numerator);
        T numerator = this.numerator.multiply(other.denominator);
        return new Rational<>(numerator, denominator);
    }

    public Rational<T> pow(final int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("Exponent cannot be negative!");
        } else if (0 == exponent) {
            return get_one();
        } else if (1 == exponent) {
            return this;
        }
        Rational<T> result = get_one();
        Rational<T> base = this;
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