use crate::arithmetic::*;
use crate::custom_integer::{CustomInteger, DError, DResult};
use crate::ops_macros::{forward_binop, forward_checked_binop, forward_unop};
use crate::tight_integer::TightInteger;
use std::cmp::Ordering;
use std::fmt;
use std::str::FromStr;

/// log(10) / (32*log(2)): base-2^32 words needed per decimal digit, used to
/// size scratch buffers before base conversion.
const DECIMAL_TO_TIGHT_LENGTH_RATIO: f64 = 0.103_812_888_090_313_15;

/// An arbitrary-precision signed integer stored as base-10 digits, most
/// significant first. The default `CustomInteger` backend.
#[derive(Debug, Clone)]
pub struct DecimalInteger {
    negative: bool,
    digits: Vec<u8>,
}

fn unsafe_new(digits: Vec<u8>, negative: bool) -> DecimalInteger {
    let d = optimize_digits(&digits);
    let negative = optimize_sign_digits(negative, &d);
    DecimalInteger { negative, digits: d }
}

impl DecimalInteger {
    pub fn zero() -> Self {
        unsafe_new(vec![0], false)
    }
    pub fn one() -> Self {
        unsafe_new(vec![1], false)
    }

    /// Returns the single-digit DecimalInteger 0-9 for `index`.
    pub fn digit(index: u32) -> DResult<Self> {
        if index > 9 {
            return Err(DError::new("the index must be between 0 and 9"));
        }
        Ok(unsafe_new(vec![index as u8], false))
    }

    /// Builds a DecimalInteger from base-10 digits (each 0-9, most
    /// significant first) and a sign, validating the digits.
    pub fn from_digits(digits: &[u8], negative: bool) -> DResult<Self> {
        if digits.is_empty() {
            return Err(DError::new("input digit slice cannot be empty"));
        }
        if digits.iter().any(|&d| d > 9) {
            return Err(DError::new("input slice is not a valid decimal integer"));
        }
        Ok(unsafe_new(digits.to_vec(), negative))
    }

    /// Builds a DecimalInteger from a machine integer.
    pub fn from_i64(n: i64) -> Self {
        Self::parse(&n.to_string()).expect("formatting an i64 always yields a parseable integer")
    }

    pub fn from_i32(n: i32) -> Self {
        Self::from_i64(n as i64)
    }

    /// Converts a TightInteger to a DecimalInteger.
    pub fn from_tight(t: &TightInteger) -> Self {
        t.to_decimal_integer()
    }

    /// Parses a (possibly signed) decimal integer literal.
    pub fn parse(number: &str) -> DResult<Self> {
        let number = strip_whitespace(number);
        if number.is_empty() {
            return Err(DError::new("input is empty"));
        }
        let bytes = number.as_bytes();
        let mut start = 0;
        let mut negative = false;
        if is_minus(bytes[0]) {
            negative = true;
            start = 1;
        } else if is_plus(bytes[0]) {
            start = 1;
        }
        if start == bytes.len() {
            return Err(DError::new(format!("input {:?} has no digits", number)));
        }
        for &c in &bytes[start..] {
            if !is_digit(c) {
                return Err(DError::new(format!(
                    "input {:?} is invalid and cannot be parsed as a decimal integer",
                    number
                )));
            }
        }
        let digits: Vec<u8> = bytes[start..].iter().map(|&c| char_to_digit(c)).collect();
        Ok(unsafe_new(digits, negative))
    }

    /// Converts this DecimalInteger to the base-2^32 representation.
    pub fn to_tight_integer(&self) -> TightInteger {
        let tight_length = (self.digits.len() as f64 * DECIMAL_TO_TIGHT_LENGTH_RATIO + 1.0) as usize + 1;
        let mut words = vec![0u32; tight_length];
        convert_digits_to_words(&mut words, &self.digits);
        TightInteger::from_words_unsafe(words, self.negative)
    }

    fn plus_raw(&self, other: &Self) -> Self {
        // expand_digits always allocates here since the target length (max+1)
        // is strictly greater than self.digits.len().
        let mut digits = expand_digits(&self.digits, self.digits.len().max(other.digits.len()) + 1);
        add_digits(&mut digits, &other.digits);
        unsafe_new(digits, self.negative)
    }

    fn minus_raw(&self, other: &Self) -> Self {
        let cmp = self.abs().cmp(&other.abs());
        let (a, b, negative) = match cmp {
            Ordering::Equal => return Self::zero(),
            Ordering::Greater => (self, other, self.negative),
            Ordering::Less => (other, self, !self.negative),
        };
        let mut digits = a.digits.clone();
        subtract_digits(&mut digits, &b.digits);
        unsafe_new(digits, negative)
    }
}

pub(crate) fn strip_whitespace(s: &str) -> String {
    s.chars().filter(|c| !c.is_whitespace()).collect()
}

impl fmt::Display for DecimalInteger {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.negative {
            write!(f, "-")?;
        }
        for &d in &self.digits {
            write!(f, "{}", digit_to_char(d) as char)?;
        }
        Ok(())
    }
}

impl PartialEq for DecimalInteger {
    fn eq(&self, other: &Self) -> bool {
        self.negative == other.negative && self.digits == other.digits
    }
}
impl Eq for DecimalInteger {}

impl Ord for DecimalInteger {
    fn cmp(&self, other: &Self) -> Ordering {
        if self.negative && !other.negative {
            return Ordering::Less;
        } else if !self.negative && other.negative {
            return Ordering::Greater;
        }
        let by_len = self.digits.len().cmp(&other.digits.len());
        let by_digits = if by_len != Ordering::Equal { by_len } else { self.digits.cmp(&other.digits) };
        if self.negative { by_digits.reverse() } else { by_digits }
    }
}
impl PartialOrd for DecimalInteger {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl CustomInteger for DecimalInteger {
    fn from_i64(n: i64) -> Self { Self::from_i64(n) }
    fn from_i32(n: i32) -> Self { Self::from_i32(n) }
    fn parse(s: &str) -> DResult<Self> { Self::parse(s) }

    fn is_zero(&self) -> bool { self.digits.len() == 1 && self.digits[0] == 0 }
    fn is_one(&self) -> bool { !self.negative && self.digits.len() == 1 && self.digits[0] == 1 }
    fn is_unit_abs(&self) -> bool { self.digits.len() == 1 && self.digits[0] == 1 }
    fn is_positive(&self) -> bool { !self.negative && !self.is_zero() }
    fn is_negative(&self) -> bool { self.negative }

    fn negate(&self) -> Self { unsafe_new(self.digits.clone(), !self.negative) }
    fn abs(&self) -> Self { unsafe_new(self.digits.clone(), false) }

    fn plus(&self, other: &Self) -> Self {
        if self.is_zero() { return other.clone(); }
        if other.is_zero() { return self.clone(); }
        if self.negative != other.negative { return self.minus_raw(&other.negate()); }
        self.plus_raw(other)
    }

    fn minus(&self, other: &Self) -> Self {
        if self.is_zero() { return other.negate(); }
        if other.is_zero() { return self.clone(); }
        if self.negative != other.negative { return self.plus_raw(&other.negate()); }
        self.minus_raw(other)
    }

    fn multiply(&self, other: &Self) -> Self {
        if self.is_zero() || other.is_zero() { return Self::zero(); }
        if self.is_unit_abs() { return if self.is_positive() { other.clone() } else { other.negate() }; }
        if other.is_unit_abs() { return if other.is_positive() { self.clone() } else { self.negate() }; }
        let mut digits = vec![0u8; self.digits.len() + other.digits.len()];
        multiply_digits(&mut digits, &self.digits, &other.digits);
        unsafe_new(digits, self.negative != other.negative)
    }

    fn multiply_base(&self, times: i32) -> DResult<Self> {
        if times < 0 {
            return Err(DError::new("multiplication times cannot be negative"));
        } else if times == 0 {
            return Ok(self.clone());
        } else if self.is_zero() {
            return Ok(Self::zero());
        }
        let mut digits = vec![0u8; self.digits.len() + times as usize];
        digits[..self.digits.len()].copy_from_slice(&self.digits);
        Ok(unsafe_new(digits, self.negative))
    }

    fn divide_by_base(&self, times: i32) -> DResult<Self> {
        if times < 0 {
            return Err(DError::new("division times cannot be negative"));
        } else if times == 0 {
            return Ok(self.clone());
        } else if self.is_zero() || times as usize >= self.digits.len() {
            return Ok(Self::zero());
        }
        let digits = self.digits[..self.digits.len() - times as usize].to_vec();
        Ok(unsafe_new(digits, self.negative))
    }

    fn divide_by(&self, other: &Self) -> DResult<Self> {
        if other.is_zero() {
            return Err(DError::new("cannot divide by zero"));
        }
        if self.is_zero() {
            return Ok(Self::zero());
        }
        if other.is_unit_abs() {
            return Ok(if other.is_positive() { self.clone() } else { self.negate() });
        }
        let mut digits = vec![0u8; self.digits.len()];
        divide_digits(&mut digits, &self.digits, &other.digits);
        Ok(unsafe_new(digits, self.negative != other.negative))
    }

    fn modulo(&self, other: &Self) -> DResult<Self> {
        if other.is_zero() {
            return Err(DError::new("cannot divide by zero"));
        }
        if self.is_zero() || other.is_unit_abs() {
            return Ok(Self::zero());
        }
        let mut digits = vec![0u8; other.digits.len()];
        modulo_digits(&mut digits, &self.digits, &other.digits);
        Ok(unsafe_new(digits, self.negative))
    }

    fn divide_by_and_modulo(&self, other: &Self) -> DResult<(Self, Self)> {
        if other.is_zero() {
            return Err(DError::new("cannot divide by zero"));
        }
        if self.is_zero() {
            return Ok((Self::zero(), Self::zero()));
        }
        if other.is_unit_abs() {
            let q = if other.is_positive() { self.clone() } else { self.negate() };
            return Ok((q, Self::zero()));
        }
        let mut quotient = vec![0u8; self.digits.len()];
        let mut remainder = vec![0u8; other.digits.len()];
        divide_and_modulo_digits(&mut quotient, &mut remainder, &self.digits, &other.digits);
        Ok((
            unsafe_new(quotient, self.negative != other.negative),
            unsafe_new(remainder, self.negative),
        ))
    }

    fn gcd(&self, other: &Self) -> Self {
        if self.is_zero() { return other.abs(); }
        if other.is_zero() { return self.abs(); }
        if self.is_unit_abs() || other.is_unit_abs() { return Self::one(); }
        let mut digits = vec![0u8; self.digits.len().min(other.digits.len())];
        gcd_digits(&mut digits, &self.digits, &other.digits);
        unsafe_new(digits, false)
    }

    fn pow(&self, exponent: i32) -> DResult<Self> {
        if exponent < 0 {
            return Err(DError::new("exponent cannot be negative"));
        } else if exponent == 0 {
            return Ok(Self::one());
        } else if exponent == 1 {
            return Ok(self.clone());
        }
        let mut result = Self::one();
        let mut base = self.clone();
        let mut power = exponent;
        loop {
            if power & 1 == 1 {
                result = result.multiply(&base);
            }
            power >>= 1;
            if power > 0 {
                base = base.multiply(&base);
            } else {
                break;
            }
        }
        Ok(result)
    }
}

forward_binop!(Add, add, plus, DecimalInteger);
forward_binop!(Sub, sub, minus, DecimalInteger);
forward_binop!(Mul, mul, multiply, DecimalInteger);
forward_checked_binop!(Div, div, divide_by, DecimalInteger);
forward_checked_binop!(Rem, rem, modulo, DecimalInteger);
forward_unop!(Neg, neg, negate, DecimalInteger);

impl FromStr for DecimalInteger {
    type Err = DError;
    fn from_str(s: &str) -> DResult<Self> {
        Self::parse(s)
    }
}
