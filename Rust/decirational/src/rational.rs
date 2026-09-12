use crate::arithmetic::{is_cyclic_begin, is_cyclic_end, is_decimal_point, is_digit, is_fraction_bar, is_minus, is_plus, CYCLIC_BEGIN, CYCLIC_END};
use crate::custom_integer::{CustomInteger, DError, DResult};
use crate::decimal_integer::strip_whitespace;
use crate::ops_macros::{forward_binop, forward_checked_binop, forward_unop};
use std::collections::HashMap;
use std::fmt;
use std::str::FromStr;

/// An exact fraction over a `CustomInteger` backend.
#[derive(Debug, Clone)]
pub struct Rational<T: CustomInteger> {
    numerator: T,
    denominator: T,
}

impl<T: CustomInteger> Rational<T> {
    pub fn numerator(&self) -> &T { &self.numerator }
    pub fn denominator(&self) -> &T { &self.denominator }

    fn get_one(&self) -> Self {
        let one = self.denominator.pow(0).expect("pow(0) never fails");
        Rational { numerator: one.clone(), denominator: one }
    }

    fn get_five(&self) -> T {
        T::from_i64(5)
    }

    fn get_ten(&self) -> T {
        T::from_i64(10)
    }

    fn reduced(numerator: T, denominator: T) -> Self {
        let gcd = numerator.gcd(&denominator);
        let (numerator, denominator) = if denominator.is_negative() {
            (numerator.negate(), denominator.negate())
        } else {
            (numerator, denominator)
        };
        Rational {
            numerator: numerator.divide_by(&gcd).expect("gcd is never zero for a nonzero denominator"),
            denominator: denominator.divide_by(&gcd).expect("gcd is never zero for a nonzero denominator"),
        }
    }

    fn sign_normalized(numerator: T, denominator: T) -> Self {
        if denominator.is_negative() {
            Rational { numerator: numerator.negate(), denominator: denominator.negate() }
        } else {
            Rational { numerator, denominator }
        }
    }

    /// Builds a reduced, sign-normalized fraction, rejecting a zero denominator.
    pub fn new(numerator: T, denominator: T) -> DResult<Self> {
        if denominator.is_zero() {
            return Err(DError::new("denominator cannot be zero"));
        }
        Ok(Self::reduced(numerator, denominator))
    }

    /// Builds the fraction integer/1.
    pub fn from_integer(integer: T) -> Self {
        let denominator = integer.pow(0).expect("pow(0) never fails");
        Rational { numerator: integer, denominator }
    }

    pub fn to_fraction_string(&self) -> String {
        format!("{}/{}", self.numerator, self.denominator)
    }

    pub fn to_mixed_string(&self) -> String {
        if self.denominator.is_one() {
            return self.numerator.to_string();
        }
        let (whole, remainder) = self.numerator.abs().divide_by_and_modulo(&self.denominator).expect("denominator is never zero");
        let mut s = String::new();
        if self.numerator.is_negative() {
            s.push('-');
        }
        if !whole.is_zero() {
            s.push_str(&whole.to_string());
            s.push(' ');
        }
        s.push_str(&remainder.to_string());
        s.push('/');
        s.push_str(&self.denominator.to_string());
        s
    }

    pub fn to_decimal_string(&self) -> String {
        let ten = self.get_ten();
        let (whole_integer, mut remainder) = self.numerator.abs().divide_by_and_modulo(&self.denominator).expect("denominator is never zero");
        let mut decimal = String::new();
        if self.numerator.is_negative() {
            decimal.push('-');
        }
        decimal.push_str(&whole_integer.to_string());
        if !remainder.is_zero() {
            decimal.push('.');
        }
        let mut seen: HashMap<String, usize> = HashMap::new();
        seen.insert(remainder.to_string(), decimal.len());
        let mut starting_cyclic: Option<usize> = None;
        while !remainder.is_zero() && starting_cyclic.is_none() {
            remainder = remainder.multiply(&ten);
            let (digit, next_remainder) = remainder.divide_by_and_modulo(&self.denominator).expect("denominator is never zero");
            remainder = next_remainder;
            decimal.push(digit.to_string().as_bytes()[0] as char);
            let key = remainder.to_string();
            if let Some(&pos) = seen.get(&key) {
                starting_cyclic = Some(pos);
            }
            seen.insert(key, decimal.len());
        }
        if let Some(pos) = starting_cyclic {
            decimal.insert(pos, CYCLIC_BEGIN as char);
            decimal.push(CYCLIC_END as char);
        }
        decimal
    }

    pub fn to_truncate_decimal_string(&self, round_to: i32) -> String {
        // Checked up front, like the i32::MIN guards in to_round_decimal_string
        // and to_ceil_decimal_string: i32::MIN has no positive counterpart
        // ("the minimum representable precision" isn't representable), so
        // there is nothing meaningful to truncate to and every port rejects
        // it. Checking it before negating anything (rather than negating
        // first and relying on how that happens to overflow) means the rest
        // of this function can just negate round_to normally.
        if round_to == i32::MIN {
            panic!("cannot round to the minimum representable precision");
        }
        let ten = self.get_ten();
        let (whole_integer, remainder0) = self.numerator.abs().divide_by_and_modulo(&self.denominator).expect("denominator is never zero");
        let whole_integer_str = whole_integer.to_string();
        let sign = if self.numerator.is_negative() { "-" } else { "" };
        if (whole_integer_str.len() as i32) <= -round_to {
            return format!("{}0", sign);
        } else if round_to < 0 {
            let shift_base = ten.pow(-round_to).expect("positive exponent never fails");
            let result = whole_integer.divide_by(&shift_base).expect("shift_base is never zero").multiply(&shift_base);
            return format!("{}{}", sign, result);
        } else if round_to == 0 {
            return format!("{}{}", sign, whole_integer_str);
        }
        let mut remainder = remainder0;
        let mut decimal = String::new();
        decimal.push_str(sign);
        decimal.push_str(&whole_integer_str);
        if !remainder.is_zero() {
            decimal.push('.');
        }
        let mut index = 0;
        while index < round_to && !remainder.is_zero() {
            remainder = remainder.multiply(&ten);
            let (digit, next_remainder) = remainder.divide_by_and_modulo(&self.denominator).expect("denominator is never zero");
            remainder = next_remainder;
            decimal.push(digit.to_string().as_bytes()[0] as char);
            index += 1;
        }
        decimal
    }

    pub fn to_round_decimal_string(&self, round_to: i32) -> String {
        if round_to == i32::MIN {
            panic!("cannot round to the minimum representable precision");
        }
        let five = Rational { numerator: self.get_five(), denominator: self.denominator.pow(0).expect("pow(0) never fails") };
        let ten = Rational { numerator: self.get_ten(), denominator: self.denominator.pow(0).expect("pow(0) never fails") };
        let shift_base = ten.pow(-round_to - 1).expect("exponent is in range");
        let delta = five.multiply(&shift_base);
        let temp = if self.is_negative() { self.minus(&delta) } else { self.plus(&delta) };
        temp.to_truncate_decimal_string(round_to)
    }

    pub fn to_ceil_decimal_string(&self, round_to: i32) -> String {
        if self.is_negative() {
            return format!("-{}", self.negate().to_floor_decimal_string(round_to));
        }
        if round_to == i32::MIN {
            panic!("cannot round to the minimum representable precision");
        }
        let ten = Rational { numerator: self.get_ten(), denominator: self.denominator.pow(0).expect("pow(0) never fails") };
        if self.multiply(&ten.pow(round_to).expect("exponent is in range")).is_integer() {
            return self.to_truncate_decimal_string(round_to);
        }
        let shift_base = ten.pow(-round_to).expect("exponent is in range");
        self.plus(&shift_base).to_truncate_decimal_string(round_to)
    }

    pub fn to_floor_decimal_string(&self, round_to: i32) -> String {
        if self.is_negative() {
            return format!("-{}", self.negate().to_ceil_decimal_string(round_to));
        }
        self.to_truncate_decimal_string(round_to)
    }

    pub fn is_integer(&self) -> bool { self.denominator.is_one() }
    pub fn is_zero(&self) -> bool { self.numerator.is_zero() }
    pub fn is_positive(&self) -> bool { self.numerator.is_positive() }
    pub fn is_negative(&self) -> bool { self.numerator.is_negative() }

    pub fn negate(&self) -> Self {
        Rational { numerator: self.numerator.negate(), denominator: self.denominator.clone() }
    }
    pub fn abs(&self) -> Self {
        Rational { numerator: self.numerator.abs(), denominator: self.denominator.clone() }
    }

    pub fn reciprocal(&self) -> DResult<Self> {
        if self.is_zero() {
            return Err(DError::new("cannot get the reciprocal of zero"));
        }
        Ok(Self::sign_normalized(self.denominator.clone(), self.numerator.clone()))
    }

    pub fn numerator_abs(&self) -> T { self.numerator.abs() }
    pub fn denominator_abs(&self) -> T { self.denominator.abs() }

    pub fn plus(&self, other: &Self) -> Self {
        let gcd = self.denominator.gcd(&other.denominator);
        let m1 = other.denominator.divide_by(&gcd).expect("gcd is never zero");
        let m2 = self.denominator.divide_by(&gcd).expect("gcd is never zero");
        let numerator = self.numerator.multiply(&m1).plus(&other.numerator.multiply(&m2));
        let denominator = gcd.multiply(&m1).multiply(&m2);
        Self::reduced(numerator, denominator)
    }

    pub fn minus(&self, other: &Self) -> Self {
        let gcd = self.denominator.gcd(&other.denominator);
        let m1 = other.denominator.divide_by(&gcd).expect("gcd is never zero");
        let m2 = self.denominator.divide_by(&gcd).expect("gcd is never zero");
        let numerator = self.numerator.multiply(&m1).minus(&other.numerator.multiply(&m2));
        let denominator = gcd.multiply(&m1).multiply(&m2);
        Self::reduced(numerator, denominator)
    }

    pub fn multiply(&self, other: &Self) -> Self {
        let gcd1 = self.denominator.gcd(&other.numerator);
        let gcd2 = other.denominator.gcd(&self.numerator);
        let denominator = self.denominator.divide_by(&gcd1).expect("gcd is never zero").multiply(&other.denominator.divide_by(&gcd2).expect("gcd is never zero"));
        let numerator = self.numerator.divide_by(&gcd2).expect("gcd is never zero").multiply(&other.numerator.divide_by(&gcd1).expect("gcd is never zero"));
        Rational { numerator, denominator }
    }

    pub fn divide_by(&self, other: &Self) -> DResult<Self> {
        if other.is_zero() {
            return Err(DError::new("cannot divide by zero"));
        }
        Ok(self.multiply(&Self::sign_normalized(other.denominator.clone(), other.numerator.clone())))
    }

    pub fn pow(&self, exponent: i32) -> DResult<Self> {
        if exponent < 0 {
            if self.is_zero() {
                return Err(DError::new("exponent cannot be negative for zero"));
            }
            if exponent == i32::MIN {
                let recip = Self::sign_normalized(self.denominator.clone(), self.numerator.clone());
                return Ok(recip.pow(i32::MAX)?.multiply(&recip));
            }
            return Self::sign_normalized(self.denominator.clone(), self.numerator.clone()).pow(-exponent);
        } else if exponent == 0 {
            return Ok(self.get_one());
        } else if exponent == 1 {
            return Ok(self.clone());
        }
        let mut result = self.get_one();
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

impl<T: CustomInteger> fmt::Display for Rational<T> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.denominator.is_one() {
            write!(f, "{}", self.numerator)
        } else {
            write!(f, "{}", self.to_fraction_string())
        }
    }
}

impl<T: CustomInteger> PartialEq for Rational<T> {
    fn eq(&self, other: &Self) -> bool {
        self.numerator == other.numerator && self.denominator == other.denominator
    }
}
impl<T: CustomInteger> Eq for Rational<T> {}

impl<T: CustomInteger> PartialOrd for Rational<T> {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        let gcd = self.denominator.gcd(&other.denominator);
        let left = self.numerator.multiply(&other.denominator.divide_by(&gcd).expect("gcd is never zero"));
        let right = other.numerator.multiply(&self.denominator.divide_by(&gcd).expect("gcd is never zero"));
        left.partial_cmp(&right)
    }
}

forward_binop!(Add, add, plus, Rational<T>, T: CustomInteger);
forward_binop!(Sub, sub, minus, Rational<T>, T: CustomInteger);
forward_binop!(Mul, mul, multiply, Rational<T>, T: CustomInteger);
forward_checked_binop!(Div, div, divide_by, Rational<T>, T: CustomInteger);
forward_unop!(Neg, neg, negate, Rational<T>, T: CustomInteger);

/// Parses the same fraction/decimal/repeating-decimal literal syntax as
/// [`parse_rational`], using `T`'s own `FromStr` as the digit-string
/// constructor it needs. Requires `T::Err = DError` since `parse_rational`
/// (like the rest of this crate) reports failures that way.
impl<T: CustomInteger + FromStr<Err = DError>> FromStr for Rational<T> {
    type Err = DError;
    fn from_str(s: &str) -> DResult<Self> {
        parse_rational(s, &|s| T::from_str(s))
    }
}

/// Parses a fraction ("3/4"), decimal ("0.5"), or repeating-decimal
/// ("0.{3}", "1.5{6}") literal into a reduced Rational<T>. `parse_int` builds
/// a T from a plain (unsigned, digits-only) decimal string - supplied
/// explicitly since a bare `T: CustomInteger` bound gives no way to require
/// one (see the `FromStr` impl below for a version that doesn't need it).
pub fn parse_rational<T: CustomInteger>(s: &str, parse_int: &dyn Fn(&str) -> DResult<T>) -> DResult<Rational<T>> {
    let s = strip_whitespace(s);
    if s.is_empty() {
        return Err(DError::new("input is empty"));
    }
    let bytes = s.as_bytes();
    let mut negative = false;
    let mut start = 0;
    if is_minus(bytes[0]) {
        negative = true;
        start = 1;
    } else if is_plus(bytes[0]) {
        start = 1;
    }
    if start >= bytes.len() || !is_digit(bytes[start]) {
        return Err(DError::new("the rational string does not have the right format"));
    }
    let (mut fraction_bar, mut decimal_point, mut cyclic_start, mut cyclic_end): (Option<usize>, Option<usize>, Option<usize>, Option<usize>) = (None, None, None, None);
    for i in (start + 1)..bytes.len() {
        let c = bytes[i];
        if is_fraction_bar(c) {
            if fraction_bar.is_some() || decimal_point.is_some() {
                return Err(DError::new("the rational string does not have the right format"));
            }
            fraction_bar = Some(i);
        } else if is_decimal_point(c) {
            if decimal_point.is_some() || fraction_bar.is_some() || i == bytes.len() - 1 {
                return Err(DError::new("the rational string does not have the right format"));
            }
            decimal_point = Some(i);
        } else if is_cyclic_begin(c) {
            if cyclic_start.is_some() || decimal_point.is_none() {
                return Err(DError::new("the rational string does not have the right format"));
            }
            cyclic_start = Some(i);
        } else if is_cyclic_end(c) {
            let cs = match cyclic_start {
                None => return Err(DError::new("the rational string does not have the right format")),
                Some(cs) => cs,
            };
            if cyclic_end.is_some() || i != bytes.len() - 1 || i - cs == 1 {
                return Err(DError::new("the rational string does not have the right format"));
            }
            cyclic_end = Some(i);
        } else if !is_digit(c) {
            return Err(DError::new("the rational string does not have the right format"));
        }
    }

    let wrap_err = |e: DError| DError::new(format!("cannot instantiate a rational from the given integer type: {}", e));

    if decimal_point.is_none() {
        let (numerator_str, denominator_str): (&str, String) = match fraction_bar {
            None => (&s[start..], "1".to_string()),
            Some(fb) if fb != bytes.len() - 1 => (&s[start..fb], s[fb + 1..].to_string()),
            _ => return Err(DError::new("the rational string does not have the right format")),
        };
        let mut numerator = parse_int(numerator_str).map_err(wrap_err)?;
        let denominator = parse_int(&denominator_str).map_err(wrap_err)?;
        if denominator.is_zero() {
            return Err(DError::new("denominator cannot be zero"));
        }
        if negative {
            numerator = numerator.negate();
        }
        return Ok(Rational::reduced(numerator, denominator));
    } else if cyclic_start.is_none() {
        let dp = decimal_point.unwrap();
        let numerator_str = format!("{}{}", &s[start..dp], &s[dp + 1..]);
        let denominator_str = format!("1{}", "0".repeat(bytes.len() - dp - 1));
        let mut numerator = parse_int(&numerator_str).map_err(wrap_err)?;
        let denominator = parse_int(&denominator_str).map_err(wrap_err)?;
        if negative {
            numerator = numerator.negate();
        }
        return Ok(Rational::reduced(numerator, denominator));
    } else if let Some(ce) = cyclic_end {
        let dp = decimal_point.unwrap();
        let cs = cyclic_start.unwrap();
        let finite_numerator_str = format!("{}{}", &s[start..dp], &s[dp + 1..cs]);
        let finite_denominator_str = format!("1{}", "0".repeat(cs - dp - 1));
        let cyclic_numerator_str = &s[cs + 1..ce];
        let cyclic_denominator_str = format!("{}{}", "9".repeat(ce - cs - 1), "0".repeat(cs - dp - 1));
        let finite_numerator = parse_int(&finite_numerator_str).map_err(wrap_err)?;
        let finite_denominator = parse_int(&finite_denominator_str).map_err(wrap_err)?;
        let cyclic_numerator = parse_int(cyclic_numerator_str).map_err(wrap_err)?;
        let cyclic_denominator = parse_int(&cyclic_denominator_str).map_err(wrap_err)?;
        let finite = Rational::reduced(finite_numerator, finite_denominator);
        let cyclic = Rational::reduced(cyclic_numerator, cyclic_denominator);
        let mut result = finite.plus(&cyclic);
        if negative {
            result = result.negate();
        }
        return Ok(result);
    }
    Err(DError::new("the rational string does not have the right format"))
}
