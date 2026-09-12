package decirational

// CustomInteger is an arbitrary-precision signed integer. DecimalInteger and
// TightInteger both implement it, and Rational[T] is generic over it.
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
	// times/exponent are int32, not Go's native int: using the wider native
	// int here once let a value that should be rejected as out of range
	// (e.g. an exponent that fits int64 but not int32) instead be silently
	// accepted and attempted, hanging on an astronomically large computation.
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
