//! A raw C ABI for this crate, built as a `cdylib`/`staticlib` in addition to
//! the normal Rust `lib`. See [`capi/decirational.h`](../capi/decirational.h)
//! for the C-facing declarations these functions implement.
//!
//! Design notes for anyone linking against this from C:
//!   - Every function that can fail returns a null pointer, or a negative
//!     sentinel (`-1` for a normally-0/1 boolean query, `INT32_MIN` for
//!     `decirational_rational_compare`, which otherwise returns -1/0/1).
//!     Call `decirational_last_error()` (valid until the next
//!     `decirational_*` call on the same thread) for a human-readable reason.
//!   - Every fallible Rust operation - `Result::Err` *and* panics (e.g. an
//!     absurd `--precision`-equivalent) - is caught here and converted to
//!     that same error convention. A panic unwinding across an `extern "C"`
//!     boundary is undefined behavior, so nothing is allowed to escape
//!     uncaught.
//!   - Strings returned to the caller (`char *`) are heap-allocated by Rust
//!     and MUST be freed with `decirational_string_free`, never with C's
//!     `free()` directly (the allocator that made them has to be the one
//!     that frees them).
//!   - `DecirationalRational` handles are opaque and MUST be freed with
//!     `decirational_rational_free`. They are backend-tagged internally
//!     (decimal vs. tight, chosen at construction time); combining two
//!     handles of different backends in one call is an error, not UB.

use crate::{CustomInteger, DResult, DecimalInteger, Lexer, Parser, Rational, TightInteger};
use std::cell::RefCell;
use std::cmp::Ordering;
use std::ffi::{c_char, c_int, c_longlong, CStr, CString};
use std::panic::{self, AssertUnwindSafe};
use std::ptr;

// ---- integer_backend values ----
const BACKEND_DECIMAL: c_int = 0;
const BACKEND_TIGHT: c_int = 1;

// ---- format values, matching the CLI's --format flag ----
const FORMAT_DEFAULT: c_int = 0;
const FORMAT_FRACTION: c_int = 1;
const FORMAT_MIXED: c_int = 2;
const FORMAT_DECIMAL: c_int = 3;
const FORMAT_TRUNCATE: c_int = 4;
const FORMAT_ROUND: c_int = 5;
const FORMAT_CEIL: c_int = 6;
const FORMAT_FLOOR: c_int = 7;

thread_local! {
    static LAST_ERROR: RefCell<Option<CString>> = const { RefCell::new(None) };
    static LAST_PANIC_MESSAGE: RefCell<Option<String>> = const { RefCell::new(None) };
}

/// Installs (once, process-wide) a panic hook that stashes the panicking
/// thread's message where `guard()` can retrieve it, then chains to
/// whatever hook was previously installed (by default, Rust's own hook that
/// prints to stderr - so panics stay visible during development, exactly as
/// for the `decirational` binary). Capturing via the hook's `Display`
/// (rather than `downcast_ref::<&str>`/`downcast_ref::<String>` on the
/// `catch_unwind` payload) is the standard, robust technique other
/// panic-catching FFI crates use, since it handles any payload shape
/// uniformly instead of only the two special-cased built-in ones.
fn install_panic_hook() {
    static INIT: std::sync::Once = std::sync::Once::new();
    INIT.call_once(|| {
        let previous_hook = panic::take_hook();
        panic::set_hook(Box::new(move |info| {
            LAST_PANIC_MESSAGE.with(|slot| *slot.borrow_mut() = Some(strip_panic_location_prefix(&info.to_string())));
            previous_hook(info);
        }));
    });
}

/// `PanicHookInfo::to_string()` renders as "panicked at <location>:\n<message>"
/// (Rust 1.82 doesn't yet stabilize a way to get just the message - that
/// landed as `PanicHookInfo::message()` in a later release). Strip that
/// prefix so callers get a clean message instead of an embedded newline and
/// a source location; fall back to the untouched text if it doesn't match
/// that shape (e.g. a custom hook further up the chain already changed it).
fn strip_panic_location_prefix(full: &str) -> String {
    match full.split_once(":\n") {
        Some((prefix, rest)) if prefix.starts_with("panicked at") => rest.to_string(),
        _ => full.to_string(),
    }
}

fn set_last_error(message: impl Into<String>) {
    // A NUL byte can never legally appear in our own error text, but guard
    // against it anyway rather than let CString::new fail silently drop it.
    let sanitized = message.into().replace('\0', "");
    LAST_ERROR.with(|slot| *slot.borrow_mut() = Some(CString::new(sanitized).unwrap_or_default()));
}

fn clear_last_error() {
    LAST_ERROR.with(|slot| *slot.borrow_mut() = None);
}

/// Returns the most recent error message set by a `decirational_*` call on
/// this thread, or NULL if the most recent call succeeded. The returned
/// pointer is only valid until the next `decirational_*` call on this
/// thread - copy it if you need it to outlive that.
#[no_mangle]
pub extern "C" fn decirational_last_error() -> *const c_char {
    LAST_ERROR.with(|slot| match slot.borrow().as_ref() {
        Some(message) => message.as_ptr(),
        None => ptr::null(),
    })
}

/// Frees a string previously returned by any `decirational_*` function.
/// Passing NULL is a no-op. Do not call C's `free()` on these pointers.
#[no_mangle]
pub extern "C" fn decirational_string_free(s: *mut c_char) {
    if s.is_null() {
        return;
    }
    unsafe {
        drop(CString::from_raw(s));
    }
}

/// A static, non-owned version string; do not free.
#[no_mangle]
pub extern "C" fn decirational_version() -> *const c_char {
    const VERSION: &[u8] = b"0.1.0\0";
    CStr::from_bytes_with_nul(VERSION).unwrap().as_ptr()
}

/// Runs `f`, catching both a returned `Err` and a Rust panic, and recording
/// either as the thread's last error. This is the single choke point every
/// exported function funnels through, so no panic can ever unwind across
/// the `extern "C"` boundary (which would be undefined behavior).
fn guard<F, R>(f: F) -> Option<R>
where
    F: FnOnce() -> Result<R, String>,
{
    install_panic_hook();
    clear_last_error();
    LAST_PANIC_MESSAGE.with(|slot| *slot.borrow_mut() = None);
    match panic::catch_unwind(AssertUnwindSafe(f)) {
        Ok(Ok(value)) => Some(value),
        Ok(Err(message)) => {
            set_last_error(message);
            None
        }
        Err(_payload) => {
            let message = LAST_PANIC_MESSAGE.with(|slot| slot.borrow_mut().take()).unwrap_or_else(|| "unknown panic".to_string());
            set_last_error(message);
            None
        }
    }
}


unsafe fn cstr_to_str<'a>(s: *const c_char) -> Result<&'a str, String> {
    if s.is_null() {
        return Err("null string pointer".to_string());
    }
    CStr::from_ptr(s).to_str().map_err(|_| "input is not valid UTF-8".to_string())
}

fn string_to_ptr(result: Option<String>) -> *mut c_char {
    match result {
        Some(s) => match CString::new(s) {
            Ok(cs) => cs.into_raw(),
            Err(_) => {
                set_last_error("result contained an embedded NUL byte");
                ptr::null_mut()
            }
        },
        None => ptr::null_mut(),
    }
}

fn format_rational<T: CustomInteger>(r: &Rational<T>, format: c_int, precision: i32) -> Result<String, String> {
    Ok(match format {
        FORMAT_DEFAULT => r.to_string(),
        FORMAT_FRACTION => r.to_fraction_string(),
        FORMAT_MIXED => r.to_mixed_string(),
        FORMAT_DECIMAL => r.to_decimal_string(),
        FORMAT_TRUNCATE => r.to_truncate_decimal_string(precision),
        FORMAT_ROUND => r.to_round_decimal_string(precision),
        FORMAT_CEIL => r.to_ceil_decimal_string(precision),
        FORMAT_FLOOR => r.to_floor_decimal_string(precision),
        other => return Err(format!("unknown format: {}", other)),
    })
}

// ---- the opaque Rational handle ----

enum RationalHandle {
    Decimal(Rational<DecimalInteger>),
    Tight(Rational<TightInteger>),
}

/// An opaque handle to a `Rational` value, backed by either `DecimalInteger`
/// or `TightInteger` depending on how it was constructed. Always allocated
/// by this library; free with `decirational_rational_free`.
pub struct DecirationalRational(RationalHandle);

fn box_handle(h: RationalHandle) -> *mut DecirationalRational {
    Box::into_raw(Box::new(DecirationalRational(h)))
}

/// Evaluates a full expression string (the same grammar and `--format`/
/// `--precision` semantics as the `decirational` CLI: `+ - * / // % ^`,
/// `()`, `[]` floor, `||` absolute value, decimal and repeating-decimal
/// literals) and returns the formatted result as a newly allocated string,
/// or NULL on error. Free the result with `decirational_string_free`.
#[no_mangle]
pub extern "C" fn decirational_eval(expression: *const c_char, integer_backend: c_int, format: c_int, precision: c_int) -> *mut c_char {
    let result = guard(move || {
        let expr = unsafe { cstr_to_str(expression) }?;
        match integer_backend {
            BACKEND_DECIMAL => {
                let lexer = Lexer::new(DecimalInteger::from_i32, DecimalInteger::parse);
                let mut parser = Parser::<DecimalInteger>::new();
                let tokens = lexer.get_tokens(expr).map_err(|e| e.to_string())?;
                let value = parser.parse(tokens).map_err(|e| e.to_string())?;
                format_rational(&value, format, precision)
            }
            BACKEND_TIGHT => {
                let lexer = Lexer::new(TightInteger::from_i32, TightInteger::parse);
                let mut parser = Parser::<TightInteger>::new();
                let tokens = lexer.get_tokens(expr).map_err(|e| e.to_string())?;
                let value = parser.parse(tokens).map_err(|e| e.to_string())?;
                format_rational(&value, format, precision)
            }
            other => Err(format!("unknown integer_backend: {}", other)),
        }
    });
    string_to_ptr(result)
}

/// Parses a fraction/decimal/repeating-decimal literal (e.g. "3/4", "0.5",
/// "0.{3}", "-7") into a new handle, or returns NULL on error.
#[no_mangle]
pub extern "C" fn decirational_rational_parse(literal: *const c_char, integer_backend: c_int) -> *mut DecirationalRational {
    let result = guard(move || {
        let s = unsafe { cstr_to_str(literal) }?;
        match integer_backend {
            BACKEND_DECIMAL => Ok(RationalHandle::Decimal(
                crate::parse_rational(s, &DecimalInteger::parse).map_err(|e| e.to_string())?,
            )),
            BACKEND_TIGHT => Ok(RationalHandle::Tight(
                crate::parse_rational(s, &TightInteger::parse).map_err(|e| e.to_string())?,
            )),
            other => Err(format!("unknown integer_backend: {}", other)),
        }
    });
    result.map(box_handle).unwrap_or(ptr::null_mut())
}

/// Builds the fraction `value`/1 in the given backend.
#[no_mangle]
pub extern "C" fn decirational_rational_from_i64(value: c_longlong, integer_backend: c_int) -> *mut DecirationalRational {
    let result = guard(move || match integer_backend {
        BACKEND_DECIMAL => Ok(RationalHandle::Decimal(Rational::from_integer(DecimalInteger::from_i64(value)))),
        BACKEND_TIGHT => Ok(RationalHandle::Tight(Rational::from_integer(TightInteger::from_i64(value)))),
        other => Err(format!("unknown integer_backend: {}", other)),
    });
    result.map(box_handle).unwrap_or(ptr::null_mut())
}

/// Frees a handle previously returned by any `decirational_rational_*`
/// function. Passing NULL is a no-op.
#[no_mangle]
pub extern "C" fn decirational_rational_free(r: *mut DecirationalRational) {
    if r.is_null() {
        return;
    }
    unsafe {
        drop(Box::from_raw(r));
    }
}

/// Returns a new, independent handle with the same value.
#[no_mangle]
pub extern "C" fn decirational_rational_clone(r: *const DecirationalRational) -> *mut DecirationalRational {
    let result = guard(move || unsafe {
        if r.is_null() {
            return Err("null Rational handle".to_string());
        }
        Ok(match &(*r).0 {
            RationalHandle::Decimal(x) => RationalHandle::Decimal(x.clone()),
            RationalHandle::Tight(x) => RationalHandle::Tight(x.clone()),
        })
    });
    result.map(box_handle).unwrap_or(ptr::null_mut())
}

fn binary_op(
    a: *const DecirationalRational,
    b: *const DecirationalRational,
    decimal_op: impl FnOnce(&Rational<DecimalInteger>, &Rational<DecimalInteger>) -> DResult<Rational<DecimalInteger>>,
    tight_op: impl FnOnce(&Rational<TightInteger>, &Rational<TightInteger>) -> DResult<Rational<TightInteger>>,
) -> *mut DecirationalRational {
    let result = guard(move || unsafe {
        if a.is_null() || b.is_null() {
            return Err("null Rational handle".to_string());
        }
        match (&(*a).0, &(*b).0) {
            (RationalHandle::Decimal(x), RationalHandle::Decimal(y)) => {
                Ok(RationalHandle::Decimal(decimal_op(x, y).map_err(|e| e.to_string())?))
            }
            (RationalHandle::Tight(x), RationalHandle::Tight(y)) => {
                Ok(RationalHandle::Tight(tight_op(x, y).map_err(|e| e.to_string())?))
            }
            _ => Err("cannot combine a decimal-backed and a tight-backed Rational".to_string()),
        }
    });
    result.map(box_handle).unwrap_or(ptr::null_mut())
}

fn unary_op(
    r: *const DecirationalRational,
    decimal_op: impl FnOnce(&Rational<DecimalInteger>) -> DResult<Rational<DecimalInteger>>,
    tight_op: impl FnOnce(&Rational<TightInteger>) -> DResult<Rational<TightInteger>>,
) -> *mut DecirationalRational {
    let result = guard(move || unsafe {
        if r.is_null() {
            return Err("null Rational handle".to_string());
        }
        match &(*r).0 {
            RationalHandle::Decimal(x) => Ok(RationalHandle::Decimal(decimal_op(x).map_err(|e| e.to_string())?)),
            RationalHandle::Tight(x) => Ok(RationalHandle::Tight(tight_op(x).map_err(|e| e.to_string())?)),
        }
    });
    result.map(box_handle).unwrap_or(ptr::null_mut())
}

/// a + b. Both handles must share the same backend.
#[no_mangle]
pub extern "C" fn decirational_rational_add(a: *const DecirationalRational, b: *const DecirationalRational) -> *mut DecirationalRational {
    binary_op(a, b, |x, y| Ok(x.plus(y)), |x, y| Ok(x.plus(y)))
}

/// a - b. Both handles must share the same backend.
#[no_mangle]
pub extern "C" fn decirational_rational_sub(a: *const DecirationalRational, b: *const DecirationalRational) -> *mut DecirationalRational {
    binary_op(a, b, |x, y| Ok(x.minus(y)), |x, y| Ok(x.minus(y)))
}

/// a * b. Both handles must share the same backend.
#[no_mangle]
pub extern "C" fn decirational_rational_mul(a: *const DecirationalRational, b: *const DecirationalRational) -> *mut DecirationalRational {
    binary_op(a, b, |x, y| Ok(x.multiply(y)), |x, y| Ok(x.multiply(y)))
}

/// a / b (exact division). NULL (with an error) if b is zero. Both handles
/// must share the same backend.
#[no_mangle]
pub extern "C" fn decirational_rational_div(a: *const DecirationalRational, b: *const DecirationalRational) -> *mut DecirationalRational {
    binary_op(a, b, |x, y| x.divide_by(y), |x, y| x.divide_by(y))
}

/// -r.
#[no_mangle]
pub extern "C" fn decirational_rational_negate(r: *const DecirationalRational) -> *mut DecirationalRational {
    unary_op(r, |x| Ok(x.negate()), |x| Ok(x.negate()))
}

/// |r|.
#[no_mangle]
pub extern "C" fn decirational_rational_abs(r: *const DecirationalRational) -> *mut DecirationalRational {
    unary_op(r, |x| Ok(x.abs()), |x| Ok(x.abs()))
}

/// 1/r. NULL (with an error) if r is zero.
#[no_mangle]
pub extern "C" fn decirational_rational_reciprocal(r: *const DecirationalRational) -> *mut DecirationalRational {
    unary_op(r, |x| x.reciprocal(), |x| x.reciprocal())
}

/// r raised to `exponent` (may be negative for a nonzero r).
#[no_mangle]
pub extern "C" fn decirational_rational_pow(r: *const DecirationalRational, exponent: c_int) -> *mut DecirationalRational {
    unary_op(r, |x| x.pow(exponent), |x| x.pow(exponent))
}

fn ordering_to_int(o: Ordering) -> c_int {
    match o {
        Ordering::Less => -1,
        Ordering::Equal => 0,
        Ordering::Greater => 1,
    }
}

/// -1/0/1 as a < / == / > b. Both handles must share the same backend.
/// Returns `INT32_MIN` on error (null handle, or mismatched backends) -
/// check `decirational_last_error()` to distinguish that from a genuine
/// comparison result (comparisons never naturally produce `INT32_MIN`).
#[no_mangle]
pub extern "C" fn decirational_rational_compare(a: *const DecirationalRational, b: *const DecirationalRational) -> c_int {
    let result = guard(move || unsafe {
        if a.is_null() || b.is_null() {
            return Err("null Rational handle".to_string());
        }
        match (&(*a).0, &(*b).0) {
            (RationalHandle::Decimal(x), RationalHandle::Decimal(y)) => Ok(ordering_to_int(x.partial_cmp(y).unwrap())),
            (RationalHandle::Tight(x), RationalHandle::Tight(y)) => Ok(ordering_to_int(x.partial_cmp(y).unwrap())),
            _ => Err("cannot compare a decimal-backed and a tight-backed Rational".to_string()),
        }
    });
    result.unwrap_or(c_int::MIN)
}

fn bool_query(
    r: *const DecirationalRational,
    decimal_q: impl FnOnce(&Rational<DecimalInteger>) -> bool,
    tight_q: impl FnOnce(&Rational<TightInteger>) -> bool,
) -> c_int {
    let result = guard(move || unsafe {
        if r.is_null() {
            return Err("null Rational handle".to_string());
        }
        Ok(match &(*r).0 {
            RationalHandle::Decimal(x) => decimal_q(x),
            RationalHandle::Tight(x) => tight_q(x),
        })
    });
    match result {
        Some(true) => 1,
        Some(false) => 0,
        None => -1,
    }
}

/// 1/0/-1 (true/false/error - check `decirational_last_error()`).
#[no_mangle]
pub extern "C" fn decirational_rational_is_zero(r: *const DecirationalRational) -> c_int {
    bool_query(r, |x| x.is_zero(), |x| x.is_zero())
}

/// 1/0/-1 (true/false/error - check `decirational_last_error()`).
#[no_mangle]
pub extern "C" fn decirational_rational_is_negative(r: *const DecirationalRational) -> c_int {
    bool_query(r, |x| x.is_negative(), |x| x.is_negative())
}

/// 1/0/-1 (true/false/error - check `decirational_last_error()`).
#[no_mangle]
pub extern "C" fn decirational_rational_is_integer(r: *const DecirationalRational) -> c_int {
    bool_query(r, |x| x.is_integer(), |x| x.is_integer())
}

/// Renders `r` per `format`/`precision` (the same values as `--format`/
/// `--precision` on the CLI; see the FORMAT_* constants in decirational.h).
/// Returns a newly allocated string, or NULL on error. Free with
/// `decirational_string_free`.
#[no_mangle]
pub extern "C" fn decirational_rational_to_string(r: *const DecirationalRational, format: c_int, precision: c_int) -> *mut c_char {
    let result = guard(move || unsafe {
        if r.is_null() {
            return Err("null Rational handle".to_string());
        }
        match &(*r).0 {
            RationalHandle::Decimal(x) => format_rational(x, format, precision),
            RationalHandle::Tight(x) => format_rational(x, format, precision),
        }
    });
    string_to_ptr(result)
}

/// The (always-reduced) numerator, as a decimal string.
#[no_mangle]
pub extern "C" fn decirational_rational_numerator_string(r: *const DecirationalRational) -> *mut c_char {
    let result = guard(move || unsafe {
        if r.is_null() {
            return Err("null Rational handle".to_string());
        }
        Ok(match &(*r).0 {
            RationalHandle::Decimal(x) => x.numerator().to_string(),
            RationalHandle::Tight(x) => x.numerator().to_string(),
        })
    });
    string_to_ptr(result)
}

/// The (always-reduced, always-positive) denominator, as a decimal string.
#[no_mangle]
pub extern "C" fn decirational_rational_denominator_string(r: *const DecirationalRational) -> *mut c_char {
    let result = guard(move || unsafe {
        if r.is_null() {
            return Err("null Rational handle".to_string());
        }
        Ok(match &(*r).0 {
            RationalHandle::Decimal(x) => x.denominator().to_string(),
            RationalHandle::Tight(x) => x.denominator().to_string(),
        })
    });
    string_to_ptr(result)
}

// A from-Rust smoke test of the extern "C" functions themselves, so `cargo
// test` alone gives some coverage of this module without requiring a C
// toolchain. The authoritative, thorough verification of the actual FFI
// boundary lives in capi/test_decirational.c (a real C program, built and
// run via run_capi_test.sh) - that's the one that can catch ABI-shape
// mistakes a same-crate Rust call never would.
#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CString;

    unsafe fn to_string(s: *const c_char) -> String {
        CStr::from_ptr(s).to_str().unwrap().to_string()
    }

    #[test]
    fn eval_round_trip() {
        let expr = CString::new("100/7").unwrap();
        let r = decirational_eval(expr.as_ptr(), BACKEND_DECIMAL, FORMAT_DECIMAL, 0);
        assert!(!r.is_null());
        assert_eq!(unsafe { to_string(r) }, "14.{285714}");
        decirational_string_free(r);
    }

    #[test]
    fn eval_error_sets_last_error() {
        let expr = CString::new("1/0").unwrap();
        let r = decirational_eval(expr.as_ptr(), BACKEND_DECIMAL, FORMAT_DEFAULT, 0);
        assert!(r.is_null());
        let err = decirational_last_error();
        assert!(!err.is_null());
        assert_eq!(unsafe { to_string(err) }, "cannot divide by zero");
    }

    #[test]
    fn rational_construct_arithmetic_format_free() {
        let a = CString::new("1/3").unwrap();
        let b = CString::new("1/6").unwrap();
        let ra = decirational_rational_parse(a.as_ptr(), BACKEND_DECIMAL);
        let rb = decirational_rational_parse(b.as_ptr(), BACKEND_DECIMAL);
        assert!(!ra.is_null() && !rb.is_null());
        let sum = decirational_rational_add(ra, rb);
        assert!(!sum.is_null());
        let s = decirational_rational_to_string(sum, FORMAT_DEFAULT, 0);
        assert_eq!(unsafe { to_string(s) }, "1/2");
        decirational_string_free(s);
        decirational_rational_free(sum);
        decirational_rational_free(ra);
        decirational_rational_free(rb);
    }

    #[test]
    fn panic_is_caught_not_propagated() {
        let five = decirational_rational_from_i64(5, BACKEND_DECIMAL);
        let r = decirational_rational_to_string(five, FORMAT_ROUND, i32::MIN);
        assert!(r.is_null(), "a Rust panic must surface as a null return, never unwind across the FFI boundary");
        let err = decirational_last_error();
        assert_eq!(unsafe { to_string(err) }, "cannot round to the minimum representable precision");
        decirational_rational_free(five);
    }

    #[test]
    fn mismatched_backends_are_a_normal_error() {
        let decimal_one = decirational_rational_from_i64(1, BACKEND_DECIMAL);
        let tight_one = decirational_rational_from_i64(1, BACKEND_TIGHT);
        assert!(decirational_rational_add(decimal_one, tight_one).is_null());
        assert_eq!(decirational_rational_compare(decimal_one, tight_one), c_int::MIN);
        decirational_rational_free(decimal_one);
        decirational_rational_free(tight_one);
    }

    #[test]
    fn null_handles_are_errors_not_crashes() {
        assert!(decirational_rational_add(ptr::null(), ptr::null()).is_null());
        assert_eq!(decirational_rational_is_zero(ptr::null()), -1);
        assert!(decirational_rational_to_string(ptr::null(), FORMAT_DEFAULT, 0).is_null());
    }
}
