use std::fmt;

/// A simple string-based error, used throughout for anything Java expresses
/// as an unchecked exception (NumberFormatException, ArithmeticException,
/// IllegalArgumentException). Unlike the Go port (which uses panic/recover,
/// a normalized Go idiom for "let this propagate through many call layers"),
/// idiomatic Rust favors `Result` end to end, so every fallible operation
/// here returns one.
#[derive(Debug, Clone)]
pub struct DError(pub String);

impl fmt::Display for DError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl std::error::Error for DError {}

impl DError {
    pub fn new(msg: impl Into<String>) -> Self {
        DError(msg.into())
    }
}

pub type DResult<T> = Result<T, DError>;

/// CustomInteger is the Rust counterpart of the Java CustomInteger<T>
/// interface: an arbitrary-precision signed integer. DecimalInteger and
/// TightInteger both implement it, and Rational<T> is generic over it,
/// exactly as in the Java version (`T extends CustomInteger<T>` there,
/// `T: CustomInteger` here - Rust's `Self` plays the role Java's `T` plays).
pub trait CustomInteger: Sized + Clone + PartialEq + Eq + PartialOrd + Ord + fmt::Display {
    /// Builds a T for a small machine integer directly, with no existing T
    /// needed to call it on. Java's generics (erased, no static dispatch
    /// through a type parameter) and Go's (interface constraints are pure
    /// method sets, no way to require a constructor either) cannot express
    /// this at all - both are limited to instance methods, which is why
    /// Rational<T>'s base-10 helpers used to build constants like 5 and 10
    /// by hand out of repeated `plus`/`multiply` starting from `pow(0)`, the
    /// only value obtainable without a T already in hand. A Rust trait can
    /// declare an associated function with no `self` parameter, so callers
    /// with just a type bound (`T: CustomInteger`) can call `T::from_i64(10)`
    /// directly.
    fn from_i64(n: i64) -> Self;

    fn is_zero(&self) -> bool;
    fn is_one(&self) -> bool;
    fn is_unit_abs(&self) -> bool;
    fn is_positive(&self) -> bool;
    fn is_negative(&self) -> bool;

    fn negate(&self) -> Self;
    fn abs(&self) -> Self;

    fn plus(&self, other: &Self) -> Self;
    fn minus(&self, other: &Self) -> Self;
    fn multiply(&self, other: &Self) -> Self;
    fn multiply_base(&self, times: i32) -> DResult<Self>;
    fn divide_by_base(&self, times: i32) -> DResult<Self>;
    fn divide_by(&self, other: &Self) -> DResult<Self>;
    fn modulo(&self, other: &Self) -> DResult<Self>;
    fn gcd(&self, other: &Self) -> Self;
    fn pow(&self, exponent: i32) -> DResult<Self>;
    fn divide_by_and_modulo(&self, other: &Self) -> DResult<(Self, Self)>;

    /// times=1 is never a negative-times error, so this is infallible.
    fn multiply_base_once(&self) -> Self {
        self.multiply_base(1).expect("times=1 is always valid")
    }
    /// times=1 is never a negative-times error, so this is infallible.
    fn divide_by_base_once(&self) -> Self {
        self.divide_by_base(1).expect("times=1 is always valid")
    }
    fn lcm(&self, other: &Self) -> DResult<Self> {
        Ok(self.divide_by(&self.gcd(other))?.multiply(other))
    }
}
