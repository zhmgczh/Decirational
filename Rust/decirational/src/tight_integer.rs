use crate::arithmetic::*;
use crate::custom_integer::{CustomInteger, DError, DResult};
use crate::decimal_integer::DecimalInteger;
use std::cmp::Ordering;
use std::fmt;

/// 32*log10(2): decimal digits needed per base-2^32 word, used to size
/// scratch buffers before base conversion (mirrors Java's
/// Arithmetic.tight_to_decimal_length_ratio).
const TIGHT_TO_DECIMAL_LENGTH_RATIO: f64 = 9.632_959_861_247_398;

/// An arbitrary-precision signed integer stored as base-2^32 words, most
/// significant first. The Rust counterpart of Java's TightInteger, selectable
/// via --integer=tight.
#[derive(Debug, Clone)]
pub struct TightInteger {
    negative: bool,
    words: Vec<u32>,
}

impl TightInteger {
    pub(crate) fn from_words_unsafe(words: Vec<u32>, negative: bool) -> Self {
        let w = optimize_words(&words);
        let negative = optimize_sign_words(negative, &w);
        TightInteger { negative, words: w }
    }

    pub fn zero() -> Self {
        Self::from_words_unsafe(vec![0], false)
    }
    pub fn one() -> Self {
        Self::from_words_unsafe(vec![1], false)
    }

    /// Returns the single-word TightInteger 0 or 1 for `index`.
    pub fn digit(index: u32) -> DResult<Self> {
        match index {
            0 => Ok(Self::zero()),
            1 => Ok(Self::one()),
            _ => Err(DError::new("the index must be either 0 or 1")),
        }
    }

    /// Builds a TightInteger from base-2^32 words (most significant first) and a sign.
    pub fn from_words(words: &[u32], negative: bool) -> DResult<Self> {
        if words.is_empty() {
            return Err(DError::new("input word slice cannot be empty"));
        }
        Ok(Self::from_words_unsafe(words.to_vec(), negative))
    }

    /// Builds a single-word TightInteger directly from a 32-bit integer,
    /// without going through decimal string parsing (mirrors Java's
    /// TightInteger(int) constructor, including its use of a widened 64-bit
    /// intermediate to negate i32::MIN without overflow).
    pub fn from_i32(n: i32) -> Self {
        let abs = reverse_abs_64(n as i64);
        Self::from_words_unsafe(vec![abs as u32], abs != n as i64)
    }

    /// Builds a TightInteger from a 64-bit integer via a decimal string
    /// round-trip, mirroring Java's TightInteger(long) constructor.
    pub fn from_i64(n: i64) -> Self {
        Self::parse(&n.to_string()).expect("formatting an i64 always yields a parseable integer")
    }

    /// Converts a DecimalInteger to a TightInteger.
    pub fn from_decimal(d: &DecimalInteger) -> Self {
        d.to_tight_integer()
    }

    /// Parses a (possibly signed) decimal integer literal.
    pub fn parse(number: &str) -> DResult<Self> {
        Ok(DecimalInteger::parse(number)?.to_tight_integer())
    }

    /// Converts this TightInteger to the base-10 representation.
    pub fn to_decimal_integer(&self) -> DecimalInteger {
        let decimal_length = (self.words.len() as f64 * TIGHT_TO_DECIMAL_LENGTH_RATIO + 1.0) as usize + 1;
        let mut digits = vec![0u8; decimal_length];
        convert_words_to_digits(&mut digits, &self.words);
        DecimalInteger::from_digits(&digits, self.negative).expect("converted digits are always valid")
    }

    fn plus_raw(&self, other: &Self) -> Self {
        let mut words = expand_words(&self.words, self.words.len().max(other.words.len()) + 1);
        add_words(&mut words, &other.words);
        Self::from_words_unsafe(words, self.negative)
    }

    fn minus_raw(&self, other: &Self) -> Self {
        let cmp = self.abs().cmp(&other.abs());
        let (a, b, negative) = match cmp {
            Ordering::Equal => return Self::zero(),
            Ordering::Greater => (self, other, self.negative),
            Ordering::Less => (other, self, !self.negative),
        };
        let mut words = a.words.clone();
        subtract_words(&mut words, &b.words);
        Self::from_words_unsafe(words, negative)
    }
}

impl fmt::Display for TightInteger {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.to_decimal_integer())
    }
}

impl PartialEq for TightInteger {
    fn eq(&self, other: &Self) -> bool {
        self.negative == other.negative && self.words == other.words
    }
}
impl Eq for TightInteger {}

impl Ord for TightInteger {
    fn cmp(&self, other: &Self) -> Ordering {
        if self.negative && !other.negative {
            return Ordering::Less;
        } else if !self.negative && other.negative {
            return Ordering::Greater;
        }
        let by_len = self.words.len().cmp(&other.words.len());
        let by_words = if by_len != Ordering::Equal { by_len } else { self.words.cmp(&other.words) };
        if self.negative { by_words.reverse() } else { by_words }
    }
}
impl PartialOrd for TightInteger {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl CustomInteger for TightInteger {
    fn from_i64(n: i64) -> Self { Self::from_i64(n) }

    fn is_zero(&self) -> bool { self.words.len() == 1 && self.words[0] == 0 }
    fn is_one(&self) -> bool { !self.negative && self.words.len() == 1 && self.words[0] == 1 }
    fn is_unit_abs(&self) -> bool { self.words.len() == 1 && self.words[0] == 1 }
    fn is_positive(&self) -> bool { !self.negative && !self.is_zero() }
    fn is_negative(&self) -> bool { self.negative }

    fn negate(&self) -> Self { Self::from_words_unsafe(self.words.clone(), !self.negative) }
    fn abs(&self) -> Self { Self::from_words_unsafe(self.words.clone(), false) }

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
        let mut words = vec![0u32; self.words.len() + other.words.len()];
        multiply_words(&mut words, &self.words, &other.words);
        Self::from_words_unsafe(words, self.negative != other.negative)
    }

    fn multiply_base(&self, times: i32) -> DResult<Self> {
        if times < 0 {
            return Err(DError::new("multiplication times cannot be negative"));
        } else if times == 0 {
            return Ok(self.clone());
        } else if self.is_zero() {
            return Ok(Self::zero());
        }
        let mut words = vec![0u32; self.words.len() + times as usize];
        words[..self.words.len()].copy_from_slice(&self.words);
        Ok(Self::from_words_unsafe(words, self.negative))
    }

    fn divide_by_base(&self, times: i32) -> DResult<Self> {
        if times < 0 {
            return Err(DError::new("division times cannot be negative"));
        } else if times == 0 {
            return Ok(self.clone());
        } else if self.is_zero() || times as usize >= self.words.len() {
            return Ok(Self::zero());
        }
        let words = self.words[..self.words.len() - times as usize].to_vec();
        Ok(Self::from_words_unsafe(words, self.negative))
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
        let mut words = vec![0u32; self.words.len()];
        divide_words(&mut words, &self.words, &other.words);
        Ok(Self::from_words_unsafe(words, self.negative != other.negative))
    }

    fn modulo(&self, other: &Self) -> DResult<Self> {
        if other.is_zero() {
            return Err(DError::new("cannot divide by zero"));
        }
        if self.is_zero() || other.is_unit_abs() {
            return Ok(Self::zero());
        }
        let mut words = vec![0u32; other.words.len()];
        modulo_words(&mut words, &self.words, &other.words);
        Ok(Self::from_words_unsafe(words, self.negative))
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
        let mut quotient = vec![0u32; self.words.len()];
        let mut remainder = vec![0u32; other.words.len()];
        divide_and_modulo_words(&mut quotient, &mut remainder, &self.words, &other.words);
        Ok((
            Self::from_words_unsafe(quotient, self.negative != other.negative),
            Self::from_words_unsafe(remainder, self.negative),
        ))
    }

    fn gcd(&self, other: &Self) -> Self {
        if self.is_zero() { return other.clone(); }
        if other.is_zero() { return self.clone(); }
        if self.is_unit_abs() || other.is_unit_abs() { return Self::one(); }
        let mut words = vec![0u32; self.words.len().min(other.words.len())];
        gcd_words(&mut words, &self.words, &other.words);
        Self::from_words_unsafe(words, false)
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
