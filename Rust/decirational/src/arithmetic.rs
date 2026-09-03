//! A faithful port of the Java `Arithmetic` class: schoolbook add/subtract/
//! multiply, binary-search long division, Euclidean gcd, and base conversion
//! between decimal digits and base-2^32 words.
//!
//! Two structural changes from the Java source (results are unchanged):
//!   - Java threads explicit `(array, start, length)` triples everywhere
//!     because Java arrays cannot be cheaply sub-viewed. Rust slices ARE such
//!     a view, so every function here takes a slice directly; a Java call on
//!     the "whole array" is simply a call with the whole slice.
//!   - The base-2^32 "tight" digits use Rust's native u32/u64 instead of
//!     Java's signed int/long plus "&0xffffffffL" masking, which Java needs
//!     only because it has no unsigned integer type. The arithmetic
//!     performed is identical.

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

// ---- decimal digits (base 10, one decimal digit per byte, most significant first) ----

pub fn preceding_zeros_digits(digits: &[u8]) -> usize {
    digits.iter().position(|&d| d != 0).unwrap_or(digits.len())
}

pub fn is_zero_digits(digits: &[u8]) -> bool {
    preceding_zeros_digits(digits) == digits.len()
}

/// Trims leading zero digits, always keeping at least one digit.
pub fn optimize_digits(digits: &[u8]) -> Vec<u8> {
    let cut = preceding_zeros_digits(digits).min(digits.len() - 1);
    digits[cut..].to_vec()
}

/// Forces the sign to non-negative when the magnitude is zero.
pub fn optimize_sign_digits(negative: bool, digits: &[u8]) -> bool {
    if digits.len() == 1 && digits[0] == 0 { false } else { negative }
}

/// Left-pads digits with zeros to reach `length`.
pub fn expand_digits(digits: &[u8], length: usize) -> Vec<u8> {
    if length <= digits.len() {
        return digits.to_vec();
    }
    let mut result = vec![0u8; length];
    result[length - digits.len()..].copy_from_slice(digits);
    result
}

fn pass_carry_digit(digits: &mut [u8], sum: u8, i: usize) -> u8 {
    let carry = sum / 10;
    digits[i] = if carry == 0 { sum } else { sum % 10 };
    carry
}

/// digits += other, in place; digits must be pre-sized to hold the result.
pub fn add_digits(digits: &mut [u8], other: &[u8]) {
    let diff = digits.len() - other.len();
    let mut carry: u8 = 0;
    let mut other_index = other.len();
    for i in (diff..digits.len()).rev() {
        other_index -= 1;
        let sum = digits[i] + other[other_index] + carry;
        carry = pass_carry_digit(digits, sum, i);
    }
    for i in (0..diff).rev() {
        let sum = digits[i] + carry;
        carry = pass_carry_digit(digits, sum, i);
    }
}

fn pass_borrow_digit(digits: &mut [u8], difference: i16, i: usize) -> u8 {
    if difference < 0 {
        digits[i] = (difference + 10) as u8;
        1
    } else {
        digits[i] = difference as u8;
        0
    }
}

/// digits -= other, in place; assumes digits >= other in magnitude.
pub fn subtract_digits(digits: &mut [u8], other: &[u8]) {
    let diff = digits.len() - other.len();
    let mut borrow: u8 = 0;
    let mut other_index = other.len();
    for i in (diff..digits.len()).rev() {
        other_index -= 1;
        let d = digits[i] as i16 - other[other_index] as i16 - borrow as i16;
        borrow = pass_borrow_digit(digits, d, i);
    }
    for i in (0..diff).rev() {
        let d = digits[i] as i16 - borrow as i16;
        borrow = pass_borrow_digit(digits, d, i);
    }
}

/// digits = a*b; digits must be pre-zeroed and len(a)+len(b) long.
pub fn multiply_digits(digits: &mut [u8], a: &[u8], b: &[u8]) {
    for i in 1..=a.len() {
        let mut carry: u8 = 0;
        let mut digits_index = digits.len() - i;
        let a_value = a[a.len() - i];
        for b_index in (0..b.len()).rev() {
            let sum = digits[digits_index] + a_value * b[b_index] + carry;
            carry = pass_carry_digit(digits, sum, digits_index);
            digits_index -= 1;
        }
        if carry != 0 {
            digits[digits_index] += carry;
        }
    }
}

/// digits = a*b for a single digit a; digits must be pre-zeroed and len(b)+1 long.
/// (digits.len() == b.len()+1 always holds by contract, so digits_index
/// reaches exactly 0 - never underflows - right as the loop ends.)
pub fn multiply_digits_scalar(digits: &mut [u8], a: u8, b: &[u8]) {
    let mut carry: u8 = 0;
    let mut digits_index = digits.len() - 1;
    for b_index in (0..b.len()).rev() {
        let sum = digits[digits_index] + a * b[b_index] + carry;
        carry = pass_carry_digit(digits, sum, digits_index);
        digits_index = digits_index.wrapping_sub(1);
    }
    if carry != 0 {
        digits[digits_index] += carry;
    }
}

pub fn compare_digits(a: &[u8], b: &[u8]) -> i32 {
    let a_start = preceding_zeros_digits(a);
    let b_start = preceding_zeros_digits(b);
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
pub fn find_right_boundary_digits(dividend: &[u8], divisor: &[u8]) -> usize {
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

/// Finds, via binary search, the largest single digit d such that
/// d*divisor <= dividend, leaving that product in temp (len(divisor)+1 long).
pub fn multiplier_digits(temp: &mut [u8], dividend: &[u8], divisor: &[u8]) -> u8 {
    let (mut left, mut right): (u8, u8) = (0, 9);
    let mut mid = (left + right + 1) >> 1;
    while left < right {
        mid = (left + right + 1) >> 1;
        temp.fill(0);
        multiply_digits_scalar(temp, mid, divisor);
        match compare_digits(temp, dividend) {
            0 => { left = mid; break; }
            -1 => left = mid,
            _ => right = mid - 1,
        }
    }
    if mid != left {
        temp.fill(0);
        multiply_digits_scalar(temp, left, divisor);
    }
    left
}

/// Drops temp's leading digit when it is zero, so its length matches
/// whichever dividend window multiplier_digits was solving for.
fn aligned_temp_digits(temp: &[u8]) -> &[u8] {
    if temp[0] == 0 { &temp[1..] } else { temp }
}

pub fn divide_digits(quotient: &mut [u8], dividend: &[u8], divisor: &[u8]) {
    let divisor_sig = &divisor[preceding_zeros_digits(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![0u8; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let diff = quotient.len() - remaining.len();
    let mut left = preceding_zeros_digits(&remaining);
    let mut right = left + find_right_boundary_digits(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        let d = multiplier_digits(&mut temp, &remaining[left..=right], divisor_sig);
        quotient[diff + right] = d;
        subtract_digits(&mut remaining[left..=right], aligned_temp_digits(&temp));
        left += preceding_zeros_digits(&remaining[left..]);
        right = left + find_right_boundary_digits(&remaining[left..], divisor_sig);
    }
}

pub fn modulo_digits(remainder: &mut [u8], dividend: &[u8], divisor: &[u8]) {
    let divisor_sig = &divisor[preceding_zeros_digits(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![0u8; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let mut left = preceding_zeros_digits(&remaining);
    let mut right = left + find_right_boundary_digits(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        multiplier_digits(&mut temp, &remaining[left..=right], divisor_sig);
        subtract_digits(&mut remaining[left..=right], aligned_temp_digits(&temp));
        left += preceding_zeros_digits(&remaining[left..]);
        right = left + find_right_boundary_digits(&remaining[left..], divisor_sig);
    }
    let start = preceding_zeros_digits(&remaining);
    let valid_len = remaining.len() - start;
    let rlen = remainder.len();
    remainder[rlen - valid_len..].copy_from_slice(&remaining[start..]);
}

pub fn divide_and_modulo_digits(quotient: &mut [u8], remainder: &mut [u8], dividend: &[u8], divisor: &[u8]) {
    let divisor_sig = &divisor[preceding_zeros_digits(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![0u8; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let diff = quotient.len() - remaining.len();
    let mut left = preceding_zeros_digits(&remaining);
    let mut right = left + find_right_boundary_digits(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        let d = multiplier_digits(&mut temp, &remaining[left..=right], divisor_sig);
        quotient[diff + right] = d;
        subtract_digits(&mut remaining[left..=right], aligned_temp_digits(&temp));
        left += preceding_zeros_digits(&remaining[left..]);
        right = left + find_right_boundary_digits(&remaining[left..], divisor_sig);
    }
    let start = preceding_zeros_digits(&remaining);
    let valid_len = remaining.len() - start;
    let rlen = remainder.len();
    remainder[rlen - valid_len..].copy_from_slice(&remaining[start..]);
}

pub fn gcd_digits(result: &mut [u8], a: &[u8], b: &[u8]) {
    let cmp = compare_digits(a, b);
    if cmp == 0 {
        let target = result.len().min(a.len()).min(b.len());
        let rlen = result.len();
        result[rlen - target..].copy_from_slice(&a[a.len() - target..]);
        return;
    }
    let max_len = a.len().max(b.len());
    let (mut larger, mut smaller) = (vec![0u8; max_len], vec![0u8; max_len]);
    if cmp > 0 {
        larger[max_len - a.len()..].copy_from_slice(a);
        smaller[max_len - b.len()..].copy_from_slice(b);
    } else {
        larger[max_len - b.len()..].copy_from_slice(b);
        smaller[max_len - a.len()..].copy_from_slice(a);
    }
    let mut temp = vec![0u8; max_len];
    let mut larger_left = preceding_zeros_digits(&larger);
    let mut smaller_left = preceding_zeros_digits(&smaller);
    while !is_zero_digits(&smaller[smaller_left..]) {
        temp[smaller_left..].fill(0);
        modulo_digits(&mut temp[smaller_left..], &larger[larger_left..], &smaller[smaller_left..]);
        let old_smaller_left = smaller_left;
        larger_left = smaller_left;
        std::mem::swap(&mut larger, &mut smaller);
        std::mem::swap(&mut smaller, &mut temp);
        smaller_left = old_smaller_left + preceding_zeros_digits(&smaller[old_smaller_left..]);
    }
    let target = larger.len() - larger_left;
    let rlen = result.len();
    result[rlen - target..].copy_from_slice(&larger[larger_left..]);
}

// ---- tight words (base 2^32, one word per u32, most significant first) ----

pub fn preceding_zeros_words(words: &[u32]) -> usize {
    words.iter().position(|&w| w != 0).unwrap_or(words.len())
}

pub fn is_zero_words(words: &[u32]) -> bool {
    preceding_zeros_words(words) == words.len()
}

pub fn optimize_words(words: &[u32]) -> Vec<u32> {
    let cut = preceding_zeros_words(words).min(words.len() - 1);
    words[cut..].to_vec()
}

pub fn optimize_sign_words(negative: bool, words: &[u32]) -> bool {
    if words.len() == 1 && words[0] == 0 { false } else { negative }
}

pub fn expand_words(words: &[u32], length: usize) -> Vec<u32> {
    if length <= words.len() {
        return words.to_vec();
    }
    let mut result = vec![0u32; length];
    result[length - words.len()..].copy_from_slice(words);
    result
}

/// The absolute value of a widened 64-bit number, used so that i32::MIN
/// doesn't overflow when negated (mirrors Java's Arithmetic.reverse_negative).
pub fn reverse_abs_64(number: i64) -> i64 {
    if number < 0 { -number } else { number }
}

fn pass_carry_word(words: &mut [u32], sum: u64, i: usize) -> u32 {
    words[i] = (sum & 0xffffffff) as u32;
    (sum >> 32) as u32
}

pub fn add_words(words: &mut [u32], other: &[u32]) {
    let diff = words.len() - other.len();
    let mut carry: u32 = 0;
    let mut other_index = other.len();
    for i in (diff..words.len()).rev() {
        other_index -= 1;
        let sum = words[i] as u64 + other[other_index] as u64 + carry as u64;
        carry = pass_carry_word(words, sum, i);
    }
    for i in (0..diff).rev() {
        let sum = words[i] as u64 + carry as u64;
        carry = pass_carry_word(words, sum, i);
    }
}

fn pass_borrow_word(words: &mut [u32], difference: i64, i: usize) -> u32 {
    words[i] = difference as u32;
    if difference < 0 { 1 } else { 0 }
}

pub fn subtract_words(words: &mut [u32], other: &[u32]) {
    let diff = words.len() - other.len();
    let mut borrow: u32 = 0;
    let mut other_index = other.len();
    for i in (diff..words.len()).rev() {
        other_index -= 1;
        let d = words[i] as i64 - other[other_index] as i64 - borrow as i64;
        borrow = pass_borrow_word(words, d, i);
    }
    for i in (0..diff).rev() {
        let d = words[i] as i64 - borrow as i64;
        borrow = pass_borrow_word(words, d, i);
    }
}

pub fn multiply_words(words: &mut [u32], a: &[u32], b: &[u32]) {
    for i in 1..=a.len() {
        let mut carry: u32 = 0;
        let mut words_index = words.len() - i;
        let a_value = a[a.len() - i] as u64;
        for b_index in (0..b.len()).rev() {
            let sum = words[words_index] as u64 + a_value * b[b_index] as u64 + carry as u64;
            carry = pass_carry_word(words, sum, words_index);
            words_index -= 1;
        }
        if carry != 0 {
            words[words_index] += carry;
        }
    }
}

/// (words.len() == b.len()+1 always holds by contract, so words_index
/// reaches exactly 0 - never underflows - right as the loop ends.)
pub fn multiply_words_scalar(words: &mut [u32], a: u32, b: &[u32]) {
    let mut carry: u32 = 0;
    let mut words_index = words.len() - 1;
    for b_index in (0..b.len()).rev() {
        let sum = words[words_index] as u64 + a as u64 * b[b_index] as u64 + carry as u64;
        carry = pass_carry_word(words, sum, words_index);
        words_index = words_index.wrapping_sub(1);
    }
    if carry != 0 {
        words[words_index] += carry;
    }
}

pub fn compare_words(a: &[u32], b: &[u32]) -> i32 {
    let a_start = preceding_zeros_words(a);
    let b_start = preceding_zeros_words(b);
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

pub fn find_right_boundary_words(dividend: &[u32], divisor: &[u32]) -> usize {
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

pub fn multiplier_words(temp: &mut [u32], dividend: &[u32], divisor: &[u32]) -> u32 {
    let (mut left, mut right): (u32, u32) = (0, 0xffffffff);
    let mut mid = (((left as u64) + (right as u64) + 1) >> 1) as u32;
    while left < right {
        mid = (((left as u64) + (right as u64) + 1) >> 1) as u32;
        temp.fill(0);
        multiply_words_scalar(temp, mid, divisor);
        match compare_words(temp, dividend) {
            0 => { left = mid; break; }
            -1 => left = mid,
            _ => right = mid - 1,
        }
    }
    if mid != left {
        temp.fill(0);
        multiply_words_scalar(temp, left, divisor);
    }
    left
}

fn aligned_temp_words(temp: &[u32]) -> &[u32] {
    if temp[0] == 0 { &temp[1..] } else { temp }
}

pub fn divide_words(quotient: &mut [u32], dividend: &[u32], divisor: &[u32]) {
    let divisor_sig = &divisor[preceding_zeros_words(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![0u32; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let diff = quotient.len() - remaining.len();
    let mut left = preceding_zeros_words(&remaining);
    let mut right = left + find_right_boundary_words(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        let d = multiplier_words(&mut temp, &remaining[left..=right], divisor_sig);
        quotient[diff + right] = d;
        subtract_words(&mut remaining[left..=right], aligned_temp_words(&temp));
        left += preceding_zeros_words(&remaining[left..]);
        right = left + find_right_boundary_words(&remaining[left..], divisor_sig);
    }
}

pub fn modulo_words(remainder: &mut [u32], dividend: &[u32], divisor: &[u32]) {
    let divisor_sig = &divisor[preceding_zeros_words(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![0u32; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let mut left = preceding_zeros_words(&remaining);
    let mut right = left + find_right_boundary_words(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        multiplier_words(&mut temp, &remaining[left..=right], divisor_sig);
        subtract_words(&mut remaining[left..=right], aligned_temp_words(&temp));
        left += preceding_zeros_words(&remaining[left..]);
        right = left + find_right_boundary_words(&remaining[left..], divisor_sig);
    }
    let start = preceding_zeros_words(&remaining);
    let valid_len = remaining.len() - start;
    let rlen = remainder.len();
    remainder[rlen - valid_len..].copy_from_slice(&remaining[start..]);
}

pub fn divide_and_modulo_words(quotient: &mut [u32], remainder: &mut [u32], dividend: &[u32], divisor: &[u32]) {
    let divisor_sig = &divisor[preceding_zeros_words(divisor)..];
    assert!(!divisor_sig.is_empty(), "divide by 0");
    let mut temp = vec![0u32; divisor_sig.len() + 1];
    let mut remaining = dividend.to_vec();
    let diff = quotient.len() - remaining.len();
    let mut left = preceding_zeros_words(&remaining);
    let mut right = left + find_right_boundary_words(&remaining[left..], divisor_sig);
    while right < remaining.len() {
        let d = multiplier_words(&mut temp, &remaining[left..=right], divisor_sig);
        quotient[diff + right] = d;
        subtract_words(&mut remaining[left..=right], aligned_temp_words(&temp));
        left += preceding_zeros_words(&remaining[left..]);
        right = left + find_right_boundary_words(&remaining[left..], divisor_sig);
    }
    let start = preceding_zeros_words(&remaining);
    let valid_len = remaining.len() - start;
    let rlen = remainder.len();
    remainder[rlen - valid_len..].copy_from_slice(&remaining[start..]);
}

pub fn gcd_words(result: &mut [u32], a: &[u32], b: &[u32]) {
    let cmp = compare_words(a, b);
    if cmp == 0 {
        let target = result.len().min(a.len()).min(b.len());
        let rlen = result.len();
        result[rlen - target..].copy_from_slice(&a[a.len() - target..]);
        return;
    }
    let max_len = a.len().max(b.len());
    let (mut larger, mut smaller) = (vec![0u32; max_len], vec![0u32; max_len]);
    if cmp > 0 {
        larger[max_len - a.len()..].copy_from_slice(a);
        smaller[max_len - b.len()..].copy_from_slice(b);
    } else {
        larger[max_len - b.len()..].copy_from_slice(b);
        smaller[max_len - a.len()..].copy_from_slice(a);
    }
    let mut temp = vec![0u32; max_len];
    let mut larger_left = preceding_zeros_words(&larger);
    let mut smaller_left = preceding_zeros_words(&smaller);
    while !is_zero_words(&smaller[smaller_left..]) {
        temp[smaller_left..].fill(0);
        modulo_words(&mut temp[smaller_left..], &larger[larger_left..], &smaller[smaller_left..]);
        let old_smaller_left = smaller_left;
        larger_left = smaller_left;
        std::mem::swap(&mut larger, &mut smaller);
        std::mem::swap(&mut smaller, &mut temp);
        smaller_left = old_smaller_left + preceding_zeros_words(&smaller[old_smaller_left..]);
    }
    let target = larger.len() - larger_left;
    let rlen = result.len();
    result[rlen - target..].copy_from_slice(&larger[larger_left..]);
}

// ---- base conversion between decimal digits and base-2^32 words ----

const TIGHT_BASE_AS_WORD: [u32; 1] = [10];
const TIGHT_BASE_AS_DIGITS: [u8; 10] = [4, 2, 9, 4, 9, 6, 7, 2, 9, 6]; // 4294967296 in decimal digits

fn decimal_remainder_to_word(remainder: &[u8]) -> u32 {
    let mut value: u64 = 0;
    for &d in remainder {
        value = value * 10 + d as u64;
    }
    value as u32
}

/// Writes the decimal digit expansion of `words` (base 2^32) into `digits`.
pub fn convert_words_to_digits(digits: &mut [u8], words: &[u32]) {
    let mut quotient = words.to_vec();
    let mut temp = vec![0u32; quotient.len()];
    let mut remainder = [0u32; 1];
    let mut digits_index = digits.len();
    let mut left = preceding_zeros_words(&quotient);
    while !is_zero_words(&quotient[left..]) {
        temp[left..].fill(0);
        remainder[0] = 0;
        divide_and_modulo_words(&mut temp[left..], &mut remainder, &quotient[left..], &TIGHT_BASE_AS_WORD);
        digits_index -= 1;
        digits[digits_index] = remainder[0] as u8;
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
