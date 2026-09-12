use std::fmt;

/// A simple string-based error covering every fallible operation in this
/// crate (bad input, division by zero, out-of-range arguments).
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

/// An arbitrary-precision signed integer. `DecimalInteger` and
/// `TightInteger` both implement it, and `Rational<T>` is generic over it.
pub trait CustomInteger: Sized + Clone + PartialEq + Eq + PartialOrd + Ord + fmt::Display {
    /// An associated function (no `self` needed) so callers with just a type
    /// bound (`T: CustomInteger`) can build a small constant, e.g.
    /// `T::from_i64(10)`, without already holding a `T`.
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
