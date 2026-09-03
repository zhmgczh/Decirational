package decirational

// CustomInteger is the Go counterpart of the Java CustomInteger<T> interface:
// an arbitrary-precision signed integer. DecimalInteger and TightInteger both
// implement it, and Rational[T] is generic over it, exactly as in the Java
// version (T extends CustomInteger<T> there, T CustomInteger[T] here).
type CustomInteger[T any] interface {
	IsZero() bool
	IsOne() bool
	IsUnitAbs() bool
	IsPositive() bool
	IsNegative() bool

	Negate() T
	Abs() T

	Plus(other T) T
	Minus(other T) T
	Multiply(other T) T
	// times/exponent are int32 (not Go's native int) to match Java's `int`
	// and Rust's `i32` exactly, including their overflow width at the
	// boundary (e.g. math.MinInt32) - using Go's wider native int here once
	// let a value Java/Rust correctly reject as out of range (e.g. an
	// exponent that fits int64 but not int32) instead be silently accepted
	// and attempted, hanging on an astronomically large computation.
	MultiplyBase(times int32) T
	MultiplyBaseOnce() T
	DivideByBase(times int32) T
	DivideByBaseOnce() T
	DivideBy(other T) T
	Modulo(other T) T
	Gcd(other T) T
	Lcm(other T) T
	Pow(exponent int32) T
	DivideByAndModulo(other T) [2]T

	Compare(other T) int
	Equals(other T) bool
	String() string
}
