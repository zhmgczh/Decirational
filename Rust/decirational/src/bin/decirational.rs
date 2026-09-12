//! A REPL that reads one arithmetic expression per line from standard input
//! and prints its value.

use decirational::{CustomInteger, DResult, DecimalInteger, Lexer, Parser, Rational, TightInteger};
use std::io::{self, BufRead, Write};
use std::panic::{self, AssertUnwindSafe};
use std::process::exit;

const USAGE: &str = "Usage: decirational [--integer=decimal|tight] [--format=<format>] [--precision=N]

  --integer=decimal   use DecimalInteger for large integers (default)
  --integer=tight     use TightInteger for large integers

  --format=default    Rational's default string form (default): fraction, or plain integer when the denominator is 1
  --format=fraction   original (improper) fraction: always numerator/denominator
  --format=mixed      mixed number: whole part and proper fraction, e.g. 1 3/4
  --format=decimal    decimal expansion, with repeating digits in {}
  --format=truncate   decimal expansion truncated to --precision digits after the point
  --format=round      decimal expansion rounded to --precision digits after the point
  --format=ceil       decimal expansion rounded up (toward +infinity) to --precision digits after the point
  --format=floor      decimal expansion rounded down (toward -infinity) to --precision digits after the point

  --precision=N       digits after the decimal point for truncate/round/ceil/floor (default 0);
                       ignored by default/fraction/mixed/decimal. N may be negative to round to
                       tens, hundreds, etc. before the point.

Reads one arithmetic expression per line from standard input and prints its value.";

fn fail(message: &str) -> ! {
    eprintln!("{}", message);
    eprintln!("{}", USAGE);
    exit(1);
}

fn main() {
    let mut integer_type = "decimal".to_string();
    let mut format = "default".to_string();
    let mut precision: i32 = 0;

    for arg in std::env::args().skip(1) {
        if arg == "--help" || arg == "-h" {
            println!("{}", USAGE);
            return;
        } else if let Some(v) = arg.strip_prefix("--integer=") {
            integer_type = v.to_string();
        } else if let Some(v) = arg.strip_prefix("--format=") {
            format = v.to_string();
        } else if let Some(v) = arg.strip_prefix("--precision=") {
            match v.parse::<i32>() {
                Ok(n) => precision = n,
                Err(_) => fail(&format!("Invalid --precision value: {}", v)),
            }
        } else {
            fail(&format!("Unknown argument: {}", arg));
        }
    }

    let result = match integer_type.as_str() {
        "decimal" => run::<DecimalInteger>(DecimalInteger::from_i32, DecimalInteger::parse, &format, precision),
        "tight" => run::<TightInteger>(TightInteger::from_i32, TightInteger::parse, &format, precision),
        other => Err(format!("unknown integer type: {} (expected 'decimal' or 'tight')", other)),
    };
    if let Err(message) = result {
        fail(&message);
    }
}

type Formatter<T> = Box<dyn Fn(&Rational<T>) -> String>;

fn make_formatter<T: CustomInteger>(format: &str, precision: i32) -> Result<Formatter<T>, String> {
    match format {
        "default" => Ok(Box::new(|r: &Rational<T>| r.to_string())),
        "fraction" => Ok(Box::new(|r: &Rational<T>| r.to_fraction_string())),
        "mixed" => Ok(Box::new(|r: &Rational<T>| r.to_mixed_string())),
        "decimal" => Ok(Box::new(|r: &Rational<T>| r.to_decimal_string())),
        "truncate" => Ok(Box::new(move |r: &Rational<T>| r.to_truncate_decimal_string(precision))),
        "round" => Ok(Box::new(move |r: &Rational<T>| r.to_round_decimal_string(precision))),
        "ceil" => Ok(Box::new(move |r: &Rational<T>| r.to_ceil_decimal_string(precision))),
        "floor" => Ok(Box::new(move |r: &Rational<T>| r.to_floor_decimal_string(precision))),
        other => Err(format!(
            "unknown format: {} (expected default, fraction, mixed, decimal, truncate, round, ceil, or floor)",
            other
        )),
    }
}

fn run<T: CustomInteger + 'static>(
    from_i32: impl Fn(i32) -> T + 'static,
    parse_int: impl Fn(&str) -> DResult<T> + 'static,
    format: &str,
    precision: i32,
) -> Result<(), String> {
    let formatter = make_formatter::<T>(format, precision)?;
    let lexer = Lexer::new(from_i32, parse_int);
    let mut parser = Parser::<T>::new();
    let stdin = io::stdin();
    let stdout = io::stdout();
    let mut out = stdout.lock();
    for line in stdin.lock().lines() {
        let line = line.expect("failed to read from standard input");
        if line.trim().is_empty() {
            continue;
        }
        // Covers the same scope as Java's per-line try/catch: tokenizing,
        // parsing, AND formatting. `Parser::parse` already turns a panic
        // during tokenizing/parsing into a `Result::Err` on its own, but
        // formatting a successfully-parsed result (e.g. an absurd
        // --precision) can still panic, and that call sits outside
        // `Parser::parse` - so it needs its own catch_unwind too, or such a
        // panic would crash the whole REPL instead of reporting one bad
        // line and continuing.
        let output = match lexer.get_tokens(&line).and_then(|tokens| parser.parse(tokens)) {
            Ok(result) => match catch_formatter_panic(|| formatter(&result)) {
                Ok(s) => s,
                Err(message) => format!("Error: {}", message),
            },
            Err(e) => format!("Error: {}", e),
        };
        writeln!(out, "{}", output).expect("failed to write to standard output");
    }
    Ok(())
}

thread_local! {
    static LAST_PANIC_MESSAGE: std::cell::RefCell<Option<String>> = const { std::cell::RefCell::new(None) };
}

/// Runs `f`, converting a panic into `Err(message)` instead of letting it
/// unwind out to `main` and kill the REPL.
///
/// The message is captured via a panic hook's `Display` (rather than
/// `downcast_ref::<&str>()`/`downcast_ref::<String>()` on the
/// `catch_unwind` payload - the same technique `capi.rs` uses, and for the
/// same reason: in this codebase that downcast reliably fails to match even
/// a plain `panic!("literal")` payload from `to_round_decimal_string`
/// (confirmed by hand while wiring up the C API's error reporting; the root
/// cause was never pinned down, so the more robust hook-based capture -
/// which handles any payload shape - is used everywhere instead of trusting
/// the downcast to work). Unlike `capi.rs`'s hook, this one does NOT chain
/// to the previously-installed (default) hook, so nothing is printed to
/// stderr for a panic recovered this way: every such panic here is a
/// calculator-level error (e.g. an absurd --precision) that gets reported
/// as a normal "Error: ..." line and the REPL moves on to the next line,
/// exactly like Java's caught exceptions and Go's recovered panics - neither
/// of which print anything to stderr for their equivalent cases either.
fn catch_formatter_panic<F: FnOnce() -> String>(f: F) -> Result<String, String> {
    static INIT: std::sync::Once = std::sync::Once::new();
    INIT.call_once(|| {
        panic::set_hook(Box::new(|info| {
            let full = info.to_string();
            let message = match full.split_once(":\n") {
                Some((prefix, rest)) if prefix.starts_with("panicked at") => rest.to_string(),
                _ => full,
            };
            LAST_PANIC_MESSAGE.with(|slot| *slot.borrow_mut() = Some(message));
        }));
    });
    LAST_PANIC_MESSAGE.with(|slot| *slot.borrow_mut() = None);
    panic::catch_unwind(AssertUnwindSafe(f)).map_err(|_| LAST_PANIC_MESSAGE.with(|slot| slot.borrow_mut().take()).unwrap_or_else(|| "unknown panic".to_string()))
}
