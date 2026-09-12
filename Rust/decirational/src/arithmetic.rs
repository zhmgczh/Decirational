//! Schoolbook add/subtract/multiply, binary-search long division, Euclidean
//! gcd, and base conversion between decimal digits and base-2^32 words.
//!
//! Each algorithm is written once, generic over the [`Limb`] trait, and
//! monomorphized for `u8` (decimal digits, base 10) and `u32` (base-2^32
//! words) by the public `_digits`/`_words` functions below - kept as the
//! stable entry points every call site in the crate uses.

pub const CYCLIC_BEGIN: u8 = b'{';
pub const CYCLIC_END: u8 = b'}';

pub fn is_digit(c: u8) -> bool { c.is_ascii_digit() }
pub fn is_plus(c: u8) -> bool { c == b'+' }
pub fn is_minus(c: u8) -> bool { c == b'-' }
pub fn is_fraction_bar(c: u8) -> bool { c == b'/' }
pub fn is_decimal_point(c: u8) -> bool { c == b'.' }
pub fn is_cyclic_begin(c: u8) -> bool { c == CYCLIC_BEGIN }
pub fn is_cyclic_end(c: u8) -> bool { c == CYCLIC_END }

pub fn char_to_digit(c: u8) -> u8 { c - b'0' }
pub fn digit_to_char(d: u8) -> u8 { d + b'0' }

// ---- the shared algorithm, generic over the limb type ----

/// A single "limb" of a big-integer magnitude: a decimal digit (`u8`, base
/// 10) or a base-2^32 word (`u32`, base 2^32). All arithmetic below widens
/// through `u64` (comfortably large enough for both: the largest possible
/// `u32` product plus two `u32` addends still fits `u64`), so the same
/// schoolbook algorithms work unchanged for either limb type.
trait Limb: Copy + Ord {
    /// The base of this limb - `10` for a decimal digit, `2^32` for a word.
    /// Every limb value is in `0..BASE`.
    const BASE: u64;
    const ZERO: Self;
    /// The largest valid single-limb value, `BASE - 1`.
    const MAX: Self;

    fn to_u64(self) -> u64;
    /// Narrows `value` (which must be `< BASE`) back down to `Self`.
    fn from_u64(value: u64) -> Self;
}

impl Limb for u8 {
    const BASE: u64 = 10;
    const ZERO: u8 = 0;
    const MAX: u8 = 9;
    fn to_u64(self) -> u64 { self as u64 }
    fn from_u64(value: u64) -> u8 { value as u8 }
}

impl Limb for u32 {
    const BASE: u64 = 1u64 << 32;
    const ZERO: u32 = 0;
    const MAX: u32 = u32::MAX;
    fn to_u64(self) -> u64 { self as u64 }
    fn from_u64(value: u64) -> u32 { value as u32 }
}

fn preceding_zeros<L: Limb>(limbs: &[L]) -> usize {
    limbs.iter().position(|&d| d != L::ZERO).unwrap_or(limbs.len())
}

fn is_zero<L: Limb>(limbs: &[L]) -> bool {
    preceding_zeros(limbs) == limbs.len()
}

/// Trims leading zero limbs, always keeping at least one. Every call site
/// already owns `limbs` (it's the `Vec` a `DecimalInteger`/`TightInteger`
/// is about to be built from), so this takes it by value and trims in
/// place - no allocation at all in the (overwhelmingly common) case where
/// there is nothing to trim, and no separate allocation from the caller's
/// own `Vec` when there is.
fn optimize<L: Limb>(mut limbs: Vec<L>) -> Vec<L> {
    let cut = preceding_zeros(&limbs).min(limbs.len() - 1);
    if cut > 0 {
        limbs.drain(..cut);
    }
    limbs
}

/// Forces the sign to non-negative when the magnitude is zero.
fn optimize_sign<L: Limb>(negative: bool, limbs: &[L]) -> bool {
    if limbs.len() == 1 && limbs[0] == L::ZERO { false } else { negative }
}

/// Left-pads with zero limbs to reach `length`.
fn expand<L: Limb>(limbs: &[L], length: usize) -> Vec<L> {
    if length <= limbs.len() {
        return limbs.to_vec();
    }
    let mut result = vec![L::ZERO; length];
    result[length - limbs.len()..].copy_from_slice(limbs);
    result
}

fn pass_carry<L: Limb>(limbs: &mut [L], sum: u64, i: usize) -> u64 {
    let carry = sum / L::BASE;
    limbs[i] = L::from_u64(if carry == 0 { sum } else { sum % L::BASE });
    carry
}

/// limbs += other, in place; limbs must be pre-sized to hold the result.
fn add<L: Limb>(limbs: &mut [L], other: &[L]) {
    let diff = limbs.len() - other.len();
    let mut carry: u64 = 0;
    let mut other_index = other.len();
    for i in (diff..limbs.len()).rev() {
        other_index -= 1;
        let sum = limbs[i].to_u64() + other[other_index].to_u64() + carry;
        carry = pass_carry::<L>(limbs, sum, i);
    }
    for i in (0..diff).rev() {
        let sum = limbs[i].to_u64() + carry;
        carry = pass_carry::<L>(limbs, sum, i);
    }
}

fn pass_borrow<L: Limb>(limbs: &mut [L], difference: i64, i: usize) -> u64 {
    if difference < 0 {
        limbs[i] = L::from_u64((difference + L::BASE as i64) as u64);
        1
    } else {
        limbs[i] = L::from_u64(difference as u64);
        0
    }
}

/// limbs -= other, in place; assumes limbs >= other in magnitude.
fn subtract<L: Limb>(limbs: &mut [L], other: &[L]) {
    let diff = limbs.len() - other.len();
    let mut borrow: u64 = 0;
    let mut other_index = other.len();
    for i in (diff..limbs.len()).rev() {
        other_index -= 1;
        let d = limbs[i].to_u64() as i64 - other[other_index].to_u64() as i64 - borrow as i64;
        borrow = pass_borrow::<L>(limbs, d, i);
    }
    for i in (0..diff).rev() {
        let d = limbs[i].to_u64() as i64 - borrow as i64;
        borrow = pass_borrow::<L>(limbs, d, i);
    }
}

/// limbs = a*b; limbs must be pre-zeroed and len(a)+len(b) long.
fn multiply<L: Limb>(limbs: &mut [L], a: &[L], b: &[L]) {
    for i in 1..=a.len() {
        let mut carry: u64 = 0;
        let mut limbs_index = limbs.len() - i;
        let a_value = a[a.len() - i].to_u64();
        for b_index in (0..b.len()).rev() {
            let sum = limbs[limbs_index].to_u64() + a_value * b[b_index].to_u64() + carry;
            carry = pass_carry::<L>(limbs, sum, limbs_index);
            limbs_index -= 1;
        }
        if carry != 0 {
            limbs[limbs_index] = L::from_u64(limbs[limbs_index].to_u64() + carry);
        }
    }
}

/// limbs = a*b for a single limb a; limbs must be pre-zeroed and len(b)+1 long.
/// (limbs.len() == b.len()+1 always holds by contract, so limbs_index
/// reaches exactly 0 - never underflows - right as the loop ends.)
fn multiply_scalar<L: Limb>(limbs: &mut [L], a: L, b: &[L]) {
    let mut carry: u64 = 0;
    let mut limbs_index = limbs.len() - 1;
    let a_value = a.to_u64();
    for b_index in (0..b.len()).rev() {
        let sum = limbs[limbs_index].to_u64() + a_value * b[b_index].to_u64() + carry;
        carry = pass_carry::<L>(limbs, sum, limbs_index);
        limbs_index = limbs_index.wrapping_sub(1);
    }
    if carry != 0 {
        limbs[limbs_index] = L::from_u64(limbs[limbs_index].to_u64() + carry);
    }
}

fn compare<L: Limb>(a: &[L], b: &[L]) -> i32 {
    let a_start = preceding_zeros(a);
    let b_start = preceding_zeros(b);
    let a_len = a.len() - a_start;
    let b_len = b.len() - b_start;
    if a_len != b_len {
        return if a_len > b_len { 1 } else { -1 };
    }
    for i in 0..a_len {
        let (av, bv) = (a[a_start + i], b[b_start + i]);
        if av != bv {
            return if av > bv { 1 } else { -1 };
        }
    }
    0
}

/// Returns, relative to the start of `dividend`, the last index of the
/// shortest leading window of `dividend` that is >= `divisor`.
fn find_right_boundary<L: Limb>(dividend: &[L], divisor: &[L]) -> usize {
    let bound = divisor.len() - 1;
    for i in 0..divisor.len() {
        if i >= dividend.len() {
            return bound + 1;
        }
        if dividend[i] != divisor[i] {
            return if dividend[i] > divisor[i] { bound } else { bound + 1 };
        }
    }
    bound
}

/// Finds, via binary search, the largest single limb d such that
/// d*divisor <= dividend, leaving that product in temp (len(divisor)+1 long).
fn multiplier<L: Limb>(temp: &mut [L], dividend: &[L], divisor: &[L]) -> L {
    let (mut left, mut right): (u64, u64) = (0, L::MAX.to_u64());
    let mut mid = (left + right + 1) >> 1;
    while left < right {
        mid = (left + right + 1) >> 1;
        temp.fill(L::ZERO);
        multiply_scalar(temp, L::from_u64(mid), divisor);
        match compare(temp, dividend) {
            0 => { left = mid; break; }
            -1 => left = mid,
            _ => right = mid - 1,
        }
    }
    if mid != left {
        temp.fill(L::ZERO);
        multiply_scalar(temp, L::from_u64(left), divisor);
    }
    L::from_u64(left)
}

/// Drops temp's leading limb when it is zero, so its length matches
/// whichever dividend window multiplier was solving for.
fn aligned_temp<L: Limb>(temp: &[L]) -> &[L] {
    if temp[0] == L::ZERO { &temp[1..] } else { temp }
}

fn divide<L: Limb>(quotient: &mut [L], dividend: &[L], divisor: &[L]) {
    let divisor_sig = &divisor[preceding_zeros(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![L::ZERO; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let diff = quotient.len() - remaining.len();
    let mut left = preceding_zeros(&remaining);
    let mut right = left + find_right_boundary(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        let d = multiplier(&mut temp, &remaining[left..=right], divisor_sig);
        quotient[diff + right] = d;
        subtract(&mut remaining[left..=right], aligned_temp(&temp));
        left += preceding_zeros(&remaining[left..]);
        right = left + find_right_boundary(&remaining[left..], divisor_sig);
    }
}

fn modulo<L: Limb>(remainder: &mut [L], dividend: &[L], divisor: &[L]) {
    let divisor_sig = &divisor[preceding_zeros(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![L::ZERO; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let mut left = preceding_zeros(&remaining);
    let mut right = left + find_right_boundary(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        multiplier(&mut temp, &remaining[left..=right], divisor_sig);
        subtract(&mut remaining[left..=right], aligned_temp(&temp));
        left += preceding_zeros(&remaining[left..]);
        right = left + find_right_boundary(&remaining[left..], divisor_sig);
    }
    let start = preceding_zeros(&remaining);
    let valid_len = remaining.len() - start;
    let rlen = remainder.len();
    remainder[rlen - valid_len..].copy_from_slice(&remaining[start..]);
}

fn divide_and_modulo<L: Limb>(quotient: &mut [L], remainder: &mut [L], dividend: &[L], divisor: &[L]) {
    let divisor_sig = &divisor[preceding_zeros(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![L::ZERO; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let diff = quotient.len() - remaining.len();
    let mut left = preceding_zeros(&remaining);
    let mut right = left + find_right_boundary(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        let d = multiplier(&mut temp, &remaining[left..=right], divisor_sig);
        quotient[diff + right] = d;
        subtract(&mut remaining[left..=right], aligned_temp(&temp));
        left += preceding_zeros(&remaining[left..]);
        right = left + find_right_boundary(&remaining[left..], divisor_sig);
    }
    let start = preceding_zeros(&remaining);
    let valid_len = remaining.len() - start;
    let rlen = remainder.len();
    remainder[rlen - valid_len..].copy_from_slice(&remaining[start..]);
}

fn gcd<L: Limb>(result: &mut [L], a: &[L], b: &[L]) {
    let cmp = compare(a, b);
    if cmp == 0 {
        let target = result.len().min(a.len()).min(b.len());
        let rlen = result.len();
        result[rlen - target..].copy_from_slice(&a[a.len() - target..]);
        return;
    }
    let max_len = a.len().max(b.len());
    let (mut larger, mut smaller) = (vec![L::ZERO; max_len], vec![L::ZERO; max_len]);
    if cmp > 0 {
        larger[max_len - a.len()..].copy_from_slice(a);
        smaller[max_len - b.len()..].copy_from_slice(b);
    } else {
        larger[max_len - b.len()..].copy_from_slice(b);
        smaller[max_len - a.len()..].copy_from_slice(a);
    }
    let mut temp = vec![L::ZERO; max_len];
    let mut larger_left = preceding_zeros(&larger);
    let mut smaller_left = preceding_zeros(&smaller);
    while !is_zero(&smaller[smaller_left..]) {
        temp[smaller_left..].fill(L::ZERO);
        modulo(&mut temp[smaller_left..], &larger[larger_left..], &smaller[smaller_left..]);
        let old_smaller_left = smaller_left;
        larger_left = smaller_left;
        std::mem::swap(&mut larger, &mut smaller);
        std::mem::swap(&mut smaller, &mut temp);
        smaller_left = old_smaller_left + preceding_zeros(&smaller[old_smaller_left..]);
    }
    let target = larger.len() - larger_left;
    let rlen = result.len();
    result[rlen - target..].copy_from_slice(&larger[larger_left..]);
}

// ---- decimal digits (base 10, one decimal digit per byte, most significant first) ----

pub fn preceding_zeros_digits(digits: &[u8]) -> usize { preceding_zeros(digits) }
pub fn is_zero_digits(digits: &[u8]) -> bool { is_zero(digits) }
pub fn optimize_digits(digits: Vec<u8>) -> Vec<u8> { optimize(digits) }
pub fn optimize_sign_digits(negative: bool, digits: &[u8]) -> bool { optimize_sign(negative, digits) }
pub fn expand_digits(digits: &[u8], length: usize) -> Vec<u8> { expand(digits, length) }
pub fn add_digits(digits: &mut [u8], other: &[u8]) { add(digits, other) }
pub fn subtract_digits(digits: &mut [u8], other: &[u8]) { subtract(digits, other) }
pub fn multiply_digits(digits: &mut [u8], a: &[u8], b: &[u8]) { multiply(digits, a, b) }
// Only exercised directly by tests.rs below (production code goes through
// the generic `divide`/`modulo`/`gcd`, which call the private generic
// versions of these, not these type-specific names) - kept `pub` so tests
// can probe this exact entry point, same as before the generic merge.
#[cfg_attr(not(test), allow(dead_code))]
pub fn multiply_digits_scalar(digits: &mut [u8], a: u8, b: &[u8]) { multiply_scalar(digits, a, b) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn compare_digits(a: &[u8], b: &[u8]) -> i32 { compare(a, b) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn find_right_boundary_digits(dividend: &[u8], divisor: &[u8]) -> usize { find_right_boundary(dividend, divisor) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn multiplier_digits(temp: &mut [u8], dividend: &[u8], divisor: &[u8]) -> u8 { multiplier(temp, dividend, divisor) }
pub fn divide_digits(quotient: &mut [u8], dividend: &[u8], divisor: &[u8]) { divide(quotient, dividend, divisor) }
pub fn modulo_digits(remainder: &mut [u8], dividend: &[u8], divisor: &[u8]) { modulo(remainder, dividend, divisor) }
pub fn divide_and_modulo_digits(quotient: &mut [u8], remainder: &mut [u8], dividend: &[u8], divisor: &[u8]) { divide_and_modulo(quotient, remainder, dividend, divisor) }
pub fn gcd_digits(result: &mut [u8], a: &[u8], b: &[u8]) { gcd(result, a, b) }

// ---- tight words (base 2^32, one word per u32, most significant first) ----

pub fn preceding_zeros_words(words: &[u32]) -> usize { preceding_zeros(words) }
pub fn is_zero_words(words: &[u32]) -> bool { is_zero(words) }
pub fn optimize_words(words: Vec<u32>) -> Vec<u32> { optimize(words) }
pub fn optimize_sign_words(negative: bool, words: &[u32]) -> bool { optimize_sign(negative, words) }
pub fn expand_words(words: &[u32], length: usize) -> Vec<u32> { expand(words, length) }

/// The absolute value of a widened 64-bit number, used so that i32::MIN
/// doesn't overflow when negated.
pub fn reverse_abs_64(number: i64) -> i64 {
    if number < 0 { -number } else { number }
}

pub fn add_words(words: &mut [u32], other: &[u32]) { add(words, other) }
pub fn subtract_words(words: &mut [u32], other: &[u32]) { subtract(words, other) }
pub fn multiply_words(words: &mut [u32], a: &[u32], b: &[u32]) { multiply(words, a, b) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn multiply_words_scalar(words: &mut [u32], a: u32, b: &[u32]) { multiply_scalar(words, a, b) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn compare_words(a: &[u32], b: &[u32]) -> i32 { compare(a, b) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn find_right_boundary_words(dividend: &[u32], divisor: &[u32]) -> usize { find_right_boundary(dividend, divisor) }
#[cfg_attr(not(test), allow(dead_code))]
pub fn multiplier_words(temp: &mut [u32], dividend: &[u32], divisor: &[u32]) -> u32 { multiplier(temp, dividend, divisor) }
pub fn divide_words(quotient: &mut [u32], dividend: &[u32], divisor: &[u32]) { divide(quotient, dividend, divisor) }
pub fn modulo_words(remainder: &mut [u32], dividend: &[u32], divisor: &[u32]) { modulo(remainder, dividend, divisor) }
pub fn divide_and_modulo_words(quotient: &mut [u32], remainder: &mut [u32], dividend: &[u32], divisor: &[u32]) { divide_and_modulo(quotient, remainder, dividend, divisor) }
pub fn gcd_words(result: &mut [u32], a: &[u32], b: &[u32]) { gcd(result, a, b) }

// ---- base conversion between decimal digits and base-2^32 words ----

const TIGHT_BASE_AS_DIGITS: [u8; 10] = [4, 2, 9, 4, 9, 6, 7, 2, 9, 6]; // 4294967296 in decimal digits

/// Decimal digits extracted per division in [`convert_words_to_digits`]: the
/// most that fit in a `u32` word (`10^9 < 2^32 <= 10^10`).
pub const DECIMAL_CHUNK_DIGITS: usize = 9;
const DECIMAL_CHUNK_BASE_AS_WORD: [u32; 1] = [1_000_000_000];

fn decimal_remainder_to_word(remainder: &[u8]) -> u32 {
    let mut value: u64 = 0;
    for &d in remainder {
        value = value * 10 + d as u64;
    }
    value as u32
}

/// Writes the decimal digit expansion of `words` (base 2^32) into `digits`.
/// `digits.len()` must be a multiple of [`DECIMAL_CHUNK_DIGITS`] large
/// enough to hold every significant digit (leading zero slots are fine -
/// callers strip those the same way they always have).
///
/// Divides by 10^9 rather than by 10: each division here is a full
/// `words.len()`-ish-word long division (binary-search based, unrelated to
/// how large the single-word divisor itself is - see `multiplier` above),
/// so extracting `DECIMAL_CHUNK_DIGITS` decimal digits per division instead
/// of one cuts the number of those divisions - and everything scaling with
/// that count (each division's own internal allocation included) - by
/// roughly `DECIMAL_CHUNK_DIGITS`-fold.
pub fn convert_words_to_digits(digits: &mut [u8], words: &[u32]) {
    let mut quotient = words.to_vec();
    let mut temp = vec![0u32; quotient.len()];
    let mut remainder = [0u32; 1];
    let mut digits_index = digits.len();
    let mut left = preceding_zeros_words(&quotient);
    while !is_zero_words(&quotient[left..]) {
        temp[left..].fill(0);
        remainder[0] = 0;
        divide_and_modulo_words(&mut temp[left..], &mut remainder, &quotient[left..], &DECIMAL_CHUNK_BASE_AS_WORD);
        let mut chunk = remainder[0];
        for _ in 0..DECIMAL_CHUNK_DIGITS {
            digits_index -= 1;
            digits[digits_index] = (chunk % 10) as u8;
            chunk /= 10;
        }
        left += preceding_zeros_words(&temp[left..]);
        let len = quotient.len() - left;
        quotient[left..].copy_from_slice(&temp[left..left + len]);
    }
}

/// Writes the base-2^32 word expansion of `digits` (decimal) into `words`.
pub fn convert_digits_to_words(words: &mut [u32], digits: &[u8]) {
    let mut quotient = digits.to_vec();
    let mut temp = vec![0u8; quotient.len()];
    let mut remainder = vec![0u8; TIGHT_BASE_AS_DIGITS.len()];
    let mut words_index = words.len();
    let mut left = preceding_zeros_digits(&quotient);
    while !is_zero_digits(&quotient[left..]) {
        temp[left..].fill(0);
        remainder.fill(0);
        divide_and_modulo_digits(&mut temp[left..], &mut remainder, &quotient[left..], &TIGHT_BASE_AS_DIGITS);
        words_index -= 1;
        words[words_index] = decimal_remainder_to_word(&remainder);
        left += preceding_zeros_digits(&temp[left..]);
        let len = quotient.len() - left;
        quotient[left..].copy_from_slice(&temp[left..left + len]);
    }
}
