use crate::arithmetic::*;
use crate::{parse_rational, CustomInteger, DecimalInteger, Lexer, Parser, Rational, RoundingMode, TightInteger, Token};

fn d(s: &str) -> DecimalInteger {
    DecimalInteger::parse(s).unwrap_or_else(|e| panic!("parse({:?}): {}", s, e))
}
fn t(s: &str) -> TightInteger {
    TightInteger::parse(s).unwrap_or_else(|e| panic!("parse({:?}): {}", s, e))
}
fn r(n: i64, den: i64) -> Rational<DecimalInteger> {
    Rational::new(DecimalInteger::from_i64(n), DecimalInteger::from_i64(den)).unwrap()
}
fn r_int(n: i64) -> Rational<DecimalInteger> {
    Rational::from_integer(DecimalInteger::from_i64(n))
}

fn assert_panics<F: FnOnce() + std::panic::UnwindSafe>(desc: &str, f: F) {
    let result = std::panic::catch_unwind(f);
    assert!(result.is_err(), "{}: expected a panic", desc);
}

// ===================== arithmetic.rs =====================

#[test]
fn arithmetic_digits_helpers() {
    assert_eq!(optimize_digits(vec![0, 0, 1, 2]), vec![1, 2]);
    assert_eq!(optimize_digits(vec![0, 0, 0]), vec![0]);
    assert_eq!(optimize_digits(vec![5]), vec![5]);

    assert!(!optimize_sign_digits(true, &[0]));
    assert!(optimize_sign_digits(true, &[5]));

    assert_eq!(expand_digits(&[1, 2], 4), vec![0, 0, 1, 2]);
    assert_eq!(expand_digits(&[1, 2], 2), vec![1, 2]);
    assert_eq!(expand_digits(&[1, 2], 1), vec![1, 2]);

    assert_eq!(compare_digits(&[1, 2, 3], &[1, 2, 3]), 0);
    assert!(compare_digits(&[1, 2, 3], &[1, 2, 4]) < 0);
    assert!(compare_digits(&[2], &[1, 9]) < 0, "shorter true magnitude beats longer");
    assert_eq!(compare_digits(&[0, 0, 5], &[5]), 0, "ignores leading zero padding");
    assert!(compare_digits(&[9], &[1]) > 0);
}

#[test]
fn arithmetic_words_helpers() {
    assert_eq!(optimize_words(vec![0, 0, 1, 2]), vec![1, 2]);
    assert_eq!(optimize_words(vec![0, 0, 0]), vec![0]);
    assert!(!optimize_sign_words(true, &[0]));

    assert_eq!(compare_words(&[1, 2], &[1, 2]), 0);
    assert!(compare_words(&[1], &[2]) < 0);
    assert_eq!(compare_words(&[0, 7], &[7]), 0);
    // Words are unsigned: u32::MAX must compare greater than 1.
    assert!(compare_words(&[u32::MAX], &[1]) > 0, "words compare as unsigned magnitude");
}

// ===================== DecimalInteger =====================

#[test]
fn decimal_integer_construction() {
    let cases = [("123", "123"), ("-123", "-123"), ("+123", "123"), ("000123", "123"), ("0", "0"), ("-0", "0"),
        ("123456789012345678901234567890", "123456789012345678901234567890")];
    for (input, want) in cases {
        assert_eq!(d(input).to_string(), want, "parse({:?})", input);
    }
    assert_eq!(DecimalInteger::from_i64(0).to_string(), "0");
    assert_eq!(DecimalInteger::from_i64(42).to_string(), "42");
    assert_eq!(DecimalInteger::from_i64(-42).to_string(), "-42");
    assert_eq!(DecimalInteger::zero(), d("0"));
    assert_eq!(DecimalInteger::one(), d("1"));
    assert_eq!(DecimalInteger::digit(7).unwrap(), d("7"));

    for bad in ["", "-", "12a", "1.5"] {
        assert!(DecimalInteger::parse(bad).is_err(), "expected error parsing {:?}", bad);
    }
    assert!(DecimalInteger::digit(10).is_err());
    assert!(DecimalInteger::from_digits(&[], false).is_err());
    assert!(DecimalInteger::from_digits(&[1, 10, 2], false).is_err(), "out-of-range digit rejected");
}

#[test]
fn decimal_integer_predicates() {
    assert!(d("0").is_zero());
    assert!(!d("1").is_zero());
    assert!(d("1").is_one());
    assert!(!d("-1").is_one());
    assert!(!d("2").is_one());
    assert!(d("1").is_unit_abs());
    assert!(d("-1").is_unit_abs());
    assert!(!d("2").is_unit_abs());
    assert!(d("5").is_positive());
    assert!(!d("0").is_positive());
    assert!(!d("-5").is_positive());
    assert!(d("-5").is_negative());
    assert!(!d("0").is_negative());
    assert!(!d("5").is_negative());
}

#[test]
fn decimal_integer_negate_abs() {
    assert_eq!(d("5").negate().to_string(), "-5");
    assert_eq!(d("-5").negate().to_string(), "5");
    assert_eq!(d("0").negate().to_string(), "0");
    assert_eq!(d("-5").abs().to_string(), "5");
    assert_eq!(d("5").abs().to_string(), "5");
}

#[test]
fn decimal_integer_compare() {
    assert!(d("5") > d("3"));
    assert!(d("3") < d("5"));
    assert_eq!(d("5"), d("5"));
    assert!(d("-5") < d("3"));
    assert!(d("-5") < d("-3"));
    assert!(d("-3") > d("-5"));
    assert!(d("100") > d("99"));
    assert!(d("-100") < d("-99"));
}

#[test]
fn decimal_integer_equals() {
    assert_eq!(d("007"), DecimalInteger::from_i64(7));
    assert_ne!(d("5"), d("-5"));
}

#[test]
fn decimal_integer_plus_minus() {
    let plus_cases = [("123", "456", "579"), ("-5", "3", "-2"), ("5", "-3", "2"), ("-5", "-3", "-8"),
        ("0", "7", "7"), ("7", "0", "7"), ("5", "-5", "0")];
    for (a, b, want) in plus_cases {
        assert_eq!(d(a).plus(&d(b)).to_string(), want, "{}+{}", a, b);
    }
    let minus_cases = [("5", "3", "2"), ("3", "5", "-2"), ("-5", "-3", "-2"), ("5", "-3", "8"), ("0", "7", "-7"), ("7", "0", "7")];
    for (a, b, want) in minus_cases {
        assert_eq!(d(a).minus(&d(b)).to_string(), want, "{}-{}", a, b);
    }
}

#[test]
fn decimal_integer_multiply() {
    let cases = [("12", "12", "144"), ("-3", "4", "-12"), ("-3", "-4", "12"), ("0", "999", "0"),
        ("1", "999", "999"), ("-1", "999", "-999"), ("999", "-1", "-999")];
    for (a, b, want) in cases {
        assert_eq!(d(a).multiply(&d(b)).to_string(), want, "{}*{}", a, b);
    }
}

#[test]
fn decimal_integer_multiply_divide_base() {
    assert_eq!(d("12").multiply_base(2).unwrap().to_string(), "1200");
    assert_eq!(d("12").multiply_base(0).unwrap().to_string(), "12");
    assert_eq!(d("-12").multiply_base(2).unwrap().to_string(), "-1200");
    assert_eq!(d("1234").divide_by_base(2).unwrap().to_string(), "12");
    assert_eq!(d("1234").divide_by_base(0).unwrap().to_string(), "1234");
    assert_eq!(d("1234").divide_by_base(10).unwrap().to_string(), "0");
    assert!(d("5").multiply_base(-1).is_err());
    assert!(d("5").divide_by_base(-1).is_err());
}

#[test]
fn decimal_integer_divide_modulo() {
    let div_cases = [("144", "12", "12"), ("-144", "12", "-12"), ("7", "2", "3"), ("-7", "2", "-3")];
    for (a, b, want) in div_cases {
        assert_eq!(d(a).divide_by(&d(b)).unwrap().to_string(), want, "{}/{}", a, b);
    }
    assert_eq!(d("7").modulo(&d("2")).unwrap().to_string(), "1");
    assert_eq!(d("-7").modulo(&d("2")).unwrap().to_string(), "-1");
    assert!(d("5").divide_by(&DecimalInteger::zero()).is_err());
    assert!(d("5").modulo(&DecimalInteger::zero()).is_err());
}

fn verify_decimal_division_identity(a_str: &str, b_str: &str) {
    let (a, b) = (d(a_str), d(b_str));
    let (q, rem) = a.divide_by_and_modulo(&b).unwrap();
    assert_eq!(q.multiply(&b).plus(&rem), a, "({})/({})", a_str, b_str);
    assert_eq!(q, a.divide_by(&b).unwrap());
    assert_eq!(rem, a.modulo(&b).unwrap());
}

#[test]
fn decimal_integer_division_identity() {
    for (a, b) in [("17", "5"), ("-17", "5"), ("17", "-5"), ("-17", "-5"), ("100", "7"), ("0", "5"), ("999999999999999999999", "37"), ("1", "1")] {
        verify_decimal_division_identity(a, b);
    }
}

#[test]
fn decimal_integer_gcd_lcm() {
    let cases = [("48", "18", "6"), ("17", "5", "1"), ("100", "75", "25"), ("0", "5", "5"), ("5", "0", "5"), ("-12", "18", "6"), ("0", "-5", "5"), ("-5", "0", "5")];
    for (a, b, want) in cases {
        assert_eq!(d(a).gcd(&d(b)).to_string(), want, "gcd({},{})", a, b);
    }
    assert_eq!(d("21").lcm(&d("6")).unwrap().to_string(), "42");
    assert_eq!(d("0").lcm(&d("5")).unwrap().to_string(), "0");
}

#[test]
fn decimal_integer_pow() {
    let cases = [("2", 10, "1024"), ("5", 0, "1"), ("0", 0, "1"), ("5", 1, "5"), ("-2", 3, "-8"), ("-2", 2, "4")];
    for (base, exp, want) in cases {
        assert_eq!(d(base).pow(exp).unwrap().to_string(), want, "{}^{}", base, exp);
    }
    assert!(d("2").pow(-1).is_err());
}

#[test]
fn decimal_integer_operators_and_fromstr() {
    use std::str::FromStr;
    let a = d("17");
    let b = d("5");
    assert_eq!((a.clone() + b.clone()).to_string(), "22", "T + T");
    assert_eq!((a.clone() + &b).to_string(), "22", "T + &T");
    assert_eq!((&a + b.clone()).to_string(), "22", "&T + T");
    assert_eq!((&a + &b).to_string(), "22", "&T + &T");
    assert_eq!((&a - &b).to_string(), "12", "&T - &T");
    assert_eq!((&a * &b).to_string(), "85", "&T * &T");
    assert_eq!((&a / &b).to_string(), "3", "&T / &T truncates toward zero");
    assert_eq!((&a % &b).to_string(), "2", "&T % &T");
    assert_eq!((-a.clone()).to_string(), "-17", "-T");
    assert_eq!((-&a).to_string(), "-17", "-&T");
    assert_panics("division by zero via /", || { let _ = &a / &DecimalInteger::from_i64(0); });
    assert_panics("modulo by zero via %", || { let _ = &a % &DecimalInteger::from_i64(0); });
    assert_eq!(DecimalInteger::from_str("42").unwrap(), d("42"), "FromStr matches parse");
    assert!(DecimalInteger::from_str("not a number").is_err());
}

// ===================== TightInteger =====================

#[test]
fn tight_integer_construction() {
    for (input, want) in [("123", "123"), ("-123", "-123"), ("0", "0"), ("-0", "0")] {
        assert_eq!(t(input).to_string(), want);
    }
    assert_eq!(TightInteger::from_i32(0).to_string(), "0");
    assert_eq!(TightInteger::from_i32(42).to_string(), "42");
    assert_eq!(TightInteger::from_i32(-42).to_string(), "-42");
    assert_eq!(TightInteger::from_i32(i32::MIN).to_string(), "-2147483648", "handles i32::MIN without overflow");
    assert_eq!(TightInteger::zero(), t("0"));
    assert_eq!(TightInteger::one(), t("1"));
    assert_eq!(TightInteger::digit(1).unwrap(), t("1"));
    assert!(TightInteger::digit(2).is_err());
    assert!(TightInteger::from_words(&[], false).is_err());
}

#[test]
fn tight_integer_predicates_and_sign() {
    assert!(t("0").is_zero());
    assert!(t("1").is_one());
    assert!(!t("-1").is_one());
    assert!(t("-1").is_unit_abs());
    assert!(t("5").is_positive());
    assert!(t("-5").is_negative());
    assert_eq!(t("5").negate().to_string(), "-5");
    assert_eq!(t("-5").abs().to_string(), "5");
    assert_eq!(t("0").negate().to_string(), "0");
}

#[test]
fn tight_integer_compare_equals() {
    assert!(t("5") > t("3"));
    assert!(t("-5") < t("-3"));
    assert_eq!(t("5"), t("5"));
    assert_eq!(t("007"), TightInteger::from_i32(7));
    assert_ne!(t("5"), t("-5"));
}

#[test]
fn tight_integer_arithmetic_basics() {
    assert_eq!(t("123").plus(&t("456")).to_string(), "579");
    assert_eq!(t("5").minus(&t("3")).to_string(), "2");
    assert_eq!(t("12").multiply(&t("12")).to_string(), "144");
    assert_eq!(t("-1").multiply(&t("999")).to_string(), "-999");
    assert_eq!(t("144").divide_by(&t("12")).unwrap().to_string(), "12");
    assert_eq!(t("-7").divide_by(&t("2")).unwrap().to_string(), "-3");
    assert_eq!(t("-7").modulo(&t("2")).unwrap().to_string(), "-1");
    assert!(t("5").divide_by(&TightInteger::zero()).is_err());
    assert!(t("5").modulo(&TightInteger::zero()).is_err());
    assert!(t("5").multiply_base(-1).is_err());
    assert!(t("5").divide_by_base(-1).is_err());
}

#[test]
fn tight_integer_multiply_divide_base_identity() {
    let round_trip = t("12").multiply_base(2).unwrap().divide_by_base(2).unwrap();
    assert_eq!(round_trip, t("12"));
    assert_eq!(t("12").multiply_base(0).unwrap().to_string(), "12");
}

fn verify_tight_division_identity(a_str: &str, b_str: &str) {
    let (a, b) = (t(a_str), t(b_str));
    let (q, rem) = a.divide_by_and_modulo(&b).unwrap();
    assert_eq!(q.multiply(&b).plus(&rem), a, "({})/({})", a_str, b_str);
    assert_eq!(q, a.divide_by(&b).unwrap());
    assert_eq!(rem, a.modulo(&b).unwrap());
}

#[test]
fn tight_integer_division_identity() {
    for (a, b) in [("17", "5"), ("-17", "5"), ("123456789012345678901234567890", "987654321"), ("0", "5")] {
        verify_tight_division_identity(a, b);
    }
}

#[test]
fn tight_integer_gcd_pow() {
    assert_eq!(t("48").gcd(&t("18")).to_string(), "6");
    assert_eq!(t("0").gcd(&t("-5")).to_string(), "5");
    assert_eq!(t("-5").gcd(&t("0")).to_string(), "5");
    assert_eq!(t("21").lcm(&t("6")).unwrap().to_string(), "42");
    assert_eq!(t("2").pow(10).unwrap().to_string(), "1024");
    assert_eq!(t("5").pow(0).unwrap().to_string(), "1");
    assert!(t("2").pow(-1).is_err());
}

#[test]
fn tight_integer_operators_and_fromstr() {
    use std::str::FromStr;
    let a = t("17");
    let b = t("5");
    assert_eq!((a.clone() + b.clone()).to_string(), "22", "T + T");
    assert_eq!((&a + &b).to_string(), "22", "&T + &T");
    assert_eq!((&a - &b).to_string(), "12", "&T - &T");
    assert_eq!((&a * &b).to_string(), "85", "&T * &T");
    assert_eq!((&a / &b).to_string(), "3", "&T / &T truncates toward zero");
    assert_eq!((&a % &b).to_string(), "2", "&T % &T");
    assert_eq!((-a.clone()).to_string(), "-17", "-T");
    assert_panics("division by zero via /", || { let _ = &a / &TightInteger::from_i64(0); });
    assert_eq!(TightInteger::from_str("42").unwrap(), t("42"), "FromStr matches parse");
    assert!(TightInteger::from_str("not a number").is_err());
}

fn verify_tight_round_trip(value: &str) {
    let tight = t(value);
    assert_eq!(tight.to_string(), value, "TightInteger round-trip for {}", value);
    assert_eq!(tight.to_decimal_integer().to_string(), value);
    assert_eq!(d(value).to_tight_integer().to_decimal_integer().to_string(), value);
}

#[test]
fn tight_integer_round_trips() {
    let values = ["0", "1", "-1", "9", "10", "99", "100", "2147483647", "2147483648", "4294967295", "4294967296",
        "-2147483648", "123456789012345678901234567890", "-999999999999999999999999999999999999999",
        // Exercise convert_words_to_digits's 9-decimal-digit chunking directly:
        // exactly one chunk, exactly two chunks, and one digit into a third
        // chunk, each with a leading digit that must not become a spurious
        // leading zero once the top chunk's unused high digits are trimmed.
        "999999999", "-999999999", "100000000", "123456789123456789",
        "-123456789123456789", "1000000000000000001"];
    for v in values {
        verify_tight_round_trip(v);
    }
}

fn cross_check(a_str: &str, b_str: &str) {
    let (da, db) = (d(a_str), d(b_str));
    let (ta, tb) = (t(a_str), t(b_str));
    assert_eq!(ta.plus(&tb).to_string(), da.plus(&db).to_string(), "plus({},{})", a_str, b_str);
    assert_eq!(ta.minus(&tb).to_string(), da.minus(&db).to_string(), "minus({},{})", a_str, b_str);
    assert_eq!(ta.multiply(&tb).to_string(), da.multiply(&db).to_string(), "multiply({},{})", a_str, b_str);
    if !db.is_zero() {
        assert_eq!(ta.divide_by(&tb).unwrap().to_string(), da.divide_by(&db).unwrap().to_string(), "divide({},{})", a_str, b_str);
        assert_eq!(ta.modulo(&tb).unwrap().to_string(), da.modulo(&db).unwrap().to_string(), "modulo({},{})", a_str, b_str);
    }
    assert_eq!(ta.gcd(&tb).to_string(), da.gcd(&db).to_string(), "gcd({},{})", a_str, b_str);
    assert_eq!(ta.pow(3).unwrap().to_string(), da.pow(3).unwrap().to_string(), "pow3({})", a_str);
}

#[test]
fn tight_vs_decimal_cross_check() {
    cross_check("123456789012345678901234567890", "987654321098765432109876543210");
    cross_check("1000000000000000000000000000000", "3");
    cross_check("-123456789", "456");
    cross_check("0", "999999999999999999999");
    cross_check("4294967296", "4294967295");
    cross_check("4294967295", "4294967295"); // both words at u32::MAX: exercises the widest carry
    cross_check("-999999999999999999999999999999999999999", "-1");
}

// ===================== Rational =====================

#[test]
fn rational_construction() {
    assert_eq!(r(4, 8).to_string(), "1/2");
    assert_eq!(r(1, -2).to_string(), "-1/2");
    assert_eq!(r(-1, -2).to_string(), "1/2");
    assert_eq!(r(5, 1).to_string(), "5");
    assert!(Rational::new(DecimalInteger::from_i64(1), DecimalInteger::zero()).is_err());

    assert!(r(4, 4).is_integer());
    assert!(!r(3, 4).is_integer());
    assert!(r(0, 5).is_zero());
    assert!(r(3, 4).is_positive());
    assert!(r(-3, 4).is_negative());
}

#[test]
fn rational_reciprocal_abs_negate() {
    assert_eq!(r(3, 4).reciprocal().unwrap().to_string(), "4/3");
    assert_eq!(r(-3, 4).reciprocal().unwrap().to_string(), "-4/3");
    assert!(r(0, 5).reciprocal().is_err());
    assert_eq!(r(-3, 4).abs().to_string(), "3/4");
    assert_eq!(r(3, 4).negate().to_string(), "-3/4");
}

#[test]
fn rational_string_parsing() {
    let cases = [("3/4", "3/4"), ("-3/4", "-3/4"), ("6/8", "3/4"), ("5", "5"),
        ("0.5", "1/2"), ("-0.5", "-1/2"), ("0.25", "1/4"), ("0.{3}", "1/3")];
    for (input, want) in cases {
        let parsed: Rational<DecimalInteger> = parse_rational(input).unwrap();
        assert_eq!(parsed.to_string(), want, "parse({:?})", input);
    }
    let mixed: Rational<DecimalInteger> = parse_rational("1.5{6}").unwrap();
    assert_eq!(mixed.to_fraction_string(), "47/30");

    for bad in ["", ".", "1.2.3", "1/2/3", "1/2.5", "5.", "0.{}", "1{3}", "1.2}"] {
        assert!(parse_rational::<DecimalInteger>(bad).is_err(), "expected error parsing {:?}", bad);
    }
}

#[test]
fn rational_arithmetic() {
    assert_eq!(r(1, 2).plus(&r(1, 3)).to_string(), "5/6");
    assert_eq!(r(1, 2).minus(&r(1, 3)).to_string(), "1/6");
    assert_eq!(r(1, 2).multiply(&r(1, 3)).to_string(), "1/6");
    assert_eq!(r(1, 2).divide_by(&r(1, 3)).unwrap().to_string(), "3/2");
    assert!(r(1, 2).divide_by(&r(0, 5)).is_err());
    assert_eq!(r(1, 3).plus(&r(2, 3)).to_string(), "1");
}

#[test]
fn rational_operators_and_fromstr() {
    use std::str::FromStr;
    let a = r(1, 2);
    let b = r(1, 3);
    assert_eq!((a.clone() + b.clone()).to_string(), "5/6", "T + T");
    assert_eq!((&a + &b).to_string(), "5/6", "&T + &T");
    assert_eq!((&a - &b).to_string(), "1/6", "&T - &T");
    assert_eq!((&a * &b).to_string(), "1/6", "&T * &T");
    assert_eq!((&a / &b).to_string(), "3/2", "&T / &T");
    assert_eq!((-a.clone()).to_string(), "-1/2", "-T");
    assert_panics("division by zero via /", || { let _ = &a / &r(0, 5); });
    let parsed: Rational<DecimalInteger> = Rational::from_str("1/3").unwrap();
    assert_eq!(parsed, r(1, 3), "FromStr matches parse_rational");
    let parsed_decimal: Rational<DecimalInteger> = "0.5".parse().unwrap();
    assert_eq!(parsed_decimal.to_string(), "1/2", "FromStr parses decimal literals too");
    assert!(Rational::<DecimalInteger>::from_str("not a rational").is_err());
}

#[test]
fn rational_pow() {
    assert_eq!(r_int(2).pow(-3).unwrap().to_string(), "1/8");
    assert_eq!(r_int(2).pow(3).unwrap().to_string(), "8");
    assert_eq!(r_int(9).pow(0).unwrap().to_string(), "1");
    assert_eq!(r_int(1).pow(i32::MIN).unwrap().to_string(), "1");
    assert_eq!(r_int(-1).pow(i32::MIN).unwrap().to_string(), "1", "even exponent");
    assert_eq!(r_int(-1).pow(i32::MIN + 1).unwrap().to_string(), "-1", "odd exponent");
    assert!(r(0, 5).pow(-1).is_err());
}

#[test]
fn rational_compare_equals() {
    assert!(r(1, 2) < r(2, 3));
    assert!(r(2, 3) > r(1, 2));
    assert_eq!(r(1, 2).partial_cmp(&r(2, 4)), Some(std::cmp::Ordering::Equal));
    assert_eq!(r(1, 2), r(2, 4));
    assert_ne!(r(1, 2), r(1, 3));
}

#[test]
fn rational_decimal_string() {
    let cases = [(1, 2, "0.5"), (5, 4, "1.25"), (1, 3, "0.{3}"), (1, 6, "0.1{6}"),
        (1, 7, "0.{142857}"), (22, 7, "3.{142857}"), (-1, 3, "-0.{3}"), (0, 1, "0")];
    for (n, den, want) in cases {
        assert_eq!(r(n, den).to_decimal_string(), want, "{}/{}", n, den);
    }
}

#[test]
fn rational_truncate_round_ceil_floor() {
    assert_eq!(r(10, 3).to_truncate_decimal_string(2), "3.33");
    assert_eq!(r(-10, 3).to_truncate_decimal_string(2), "-3.33");
    assert_eq!(r_int(1234).to_truncate_decimal_string(-2), "1200");
    assert_eq!(r_int(50).to_truncate_decimal_string(-3), "0");

    assert_eq!(r(1, 3).to_round_decimal_string(2), "0.33");
    assert_eq!(r(2, 3).to_round_decimal_string(2), "0.67");
    assert_eq!(r(1, 2).to_round_decimal_string(0), "1", "half-up");
    assert_eq!(r(-1, 2).to_round_decimal_string(0), "-1");

    assert_eq!(r(1, 3).to_ceil_decimal_string(0), "1");
    assert_eq!(r(1, 3).to_floor_decimal_string(0), "0");
    assert_eq!(r(-1, 3).to_floor_decimal_string(0), "-1");
    assert_eq!(r(5, 2).to_ceil_decimal_string(0), "3");
    assert_eq!(r(-5, 2).to_ceil_decimal_string(0), "-2");
    assert_eq!(r(-5, 2).to_floor_decimal_string(0), "-3");
    assert_eq!(r_int(0).to_ceil_decimal_string(0), "0");
    assert_eq!(r_int(0).to_floor_decimal_string(0), "0");
    // KNOWN BUG, preserved intentionally: ceil() of a negative non-integer
    // whose floor()-of-negation is exactly zero prints "-0" instead of "0".
    // Documented rather than silently treated as correct.
    assert_eq!(r(-1, 3).to_ceil_decimal_string(0), "-0", "known -0 bug");
}

// truncate/round/ceil/floor must agree on digit count: truncate/ceil/floor used to stop
// early and drop trailing zeros whenever the exact decimal terminated before reaching the
// requested precision (e.g. an integer's remainder is zero from the start), while round
// always padded because its old "add 5 * 10^(-round_to-1) then truncate" trick made the
// remainder non-zero almost by construction. Each case below is already exact at the given
// precision, so all four formats must produce the identical, fully padded string.
#[test]
fn rational_decimal_formats_agree_on_digit_count() {
    let cases = [
        (2, 1, 3, "2.000"),   // whole number: remainder is zero from the start
        (7, 4, 2, "1.75"),    // terminates exactly at the requested precision
        (7, 4, 4, "1.7500"),  // terminates before the requested precision
        (-2, 1, 2, "-2.00"),
    ];
    for (n, den, precision, want) in cases {
        let v = r(n, den);
        assert_eq!(v.to_truncate_decimal_string(precision), want, "{}/{} truncate({})", n, den, precision);
        assert_eq!(v.to_round_decimal_string(precision), want, "{}/{} round({})", n, den, precision);
        assert_eq!(v.to_ceil_decimal_string(precision), want, "{}/{} ceil({})", n, den, precision);
        assert_eq!(v.to_floor_decimal_string(precision), want, "{}/{} floor({})", n, den, precision);
    }
}

// HALF_EVEN agrees with half-up except on an exact tie (a discarded fraction of precisely
// 1/2), where it rounds to whichever neighbor has an even last digit.
#[test]
fn rational_round_half_even() {
    let cases = [
        (1, 8, 2, "0.13", "0.12"),    // 0.125: last kept digit 2 is even -> stays down
        (-1, 8, 2, "-0.13", "-0.12"),
        (3, 8, 2, "0.38", "0.38"),    // 0.375: 37 is odd -> half-up and half-even agree on 38
        (1, 2, 0, "1", "0"),          // 0.5: 0 is even -> stays down under half-even
        (3, 2, 0, "2", "2"),          // 1.5: 2 is even -> rounds up either way
        (5, 2, 0, "3", "2"),          // 2.5: 2 is even -> stays down under half-even
    ];
    for (n, den, precision, half_up, half_even) in cases {
        let v = r(n, den);
        assert_eq!(v.to_round_decimal_string_mode(precision, RoundingMode::HalfUp), half_up, "{}/{} round({}, half-up)", n, den, precision);
        assert_eq!(v.to_round_decimal_string_mode(precision, RoundingMode::HalfEven), half_even, "{}/{} round({}, half-even)", n, den, precision);
        // to_round_decimal_string must keep defaulting to half-up.
        assert_eq!(v.to_round_decimal_string(precision), half_up, "{}/{} round({}) default", n, den, precision);
    }
}

#[test]
fn rational_mixed_string() {
    let cases = [(7, 4, "1 3/4"), (-7, 4, "-1 3/4"), (3, 4, "3/4"), (-3, 4, "-3/4"),
        (5, 1, "5"), (-5, 1, "-5"), (0, 1, "0"), (5, 2, "2 1/2")];
    for (n, den, want) in cases {
        assert_eq!(r(n, den).to_mixed_string(), want, "mixed({}/{})", n, den);
    }
}

// ===================== Lexer =====================

fn test_lexer() -> Lexer<TightInteger> {
    Lexer::new()
}

#[test]
fn lexer_empty_input() {
    let lexer = test_lexer();
    assert_eq!(lexer.get_tokens("").unwrap().len(), 0);
    assert_eq!(lexer.get_tokens("   ").unwrap().len(), 0);
}

#[test]
fn lexer_whitespace_stripped() {
    let lexer = test_lexer();
    assert_eq!(lexer.get_tokens("1+2").unwrap().len(), lexer.get_tokens(" 1 + 2 ").unwrap().len());
}

#[test]
fn lexer_basic_tokenization() {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens("12+3*4").unwrap();
    assert_eq!(tokens.len(), 5);
    assert!(matches!(tokens[0], Token::Integer(_)));
    assert_eq!(tokens[1], Token::Plus);
    assert_eq!(tokens[3], Token::Multiply);
}

#[test]
fn lexer_integer_boundary() {
    // Both sides of the i32 fast path (see Lexer::parse_operand) must land
    // on the same Token::Integer variant with the correct value - unlike
    // Java, there's no separate "large integer" token type to check against.
    let lexer = test_lexer();
    let int_value = |s: &str| match &lexer.get_tokens(s).unwrap()[0] {
        Token::Integer(v) => v.to_string(),
        other => panic!("expected Token::Integer, got {:?}", other),
    };
    assert_eq!(int_value("2147483647"), "2147483647", "fits an i32");
    assert_eq!(int_value("2147483648"), "2147483648", "one past i32::MAX");
    assert_eq!(int_value("99999999999999999999"), "99999999999999999999", "far beyond i32");
}

#[test]
fn lexer_decimal_and_cyclic_literals() {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens("3.14").unwrap();
    assert_eq!(tokens.len(), 1);
    assert!(matches!(tokens[0], Token::Rational(_)));
    let tokens = lexer.get_tokens("0.{3}").unwrap();
    assert_eq!(tokens.len(), 1);
    assert!(matches!(tokens[0], Token::Rational(_)));
}

#[test]
fn lexer_fraction_bar_splits_tokens() {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens("1/2").unwrap();
    assert_eq!(tokens.len(), 3, "1/2 splits into operand, DIVISION, operand");
    assert_eq!(tokens[1], Token::Divide);
}

#[test]
fn lexer_structural_tokens() {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens("([|1|])").unwrap();
    assert_eq!(tokens.len(), 7);
    assert_eq!(tokens[0], Token::LeftParen);
    assert_eq!(tokens[1], Token::LeftFloor);
    assert_eq!(tokens[2], Token::AbsoluteBar);
    assert!(matches!(tokens[3], Token::Integer(_)));
    assert_eq!(tokens[4], Token::AbsoluteBar);
    assert_eq!(tokens[5], Token::RightFloor);
    assert_eq!(tokens[6], Token::RightParen);
}

#[test]
fn lexer_repeated_operator_chars() {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens("++").unwrap();
    assert_eq!(tokens, vec![Token::Plus, Token::Plus], "++ is two separate PLUS tokens");
}

#[test]
fn lexer_operand_followed_by_paren() {
    let lexer = test_lexer();
    assert_eq!(lexer.get_tokens("12(3)").unwrap().len(), 4);
}

#[test]
fn lexer_illegal_character() {
    let lexer = test_lexer();
    assert!(lexer.get_tokens("2#3").is_err());
    assert!(lexer.get_tokens("5!").is_err());
}

// ===================== Parser =====================

fn eval(expr: &str) -> String {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens(expr).unwrap_or_else(|e| panic!("get_tokens({:?}): {}", expr, e));
    let mut parser = Parser::<TightInteger>::new();
    let result = parser.parse(tokens).unwrap_or_else(|e| panic!("parse({:?}): {}", expr, e));
    result.to_decimal_string()
}

#[test]
fn parser_precedence_and_associativity() {
    let cases = [
        ("2+3*4", "14"), ("(2+3)*4", "20"), ("10%3", "1"), ("2^10", "1024"), ("2^-2", "0.25"),
        ("2^3^2", "512"), ("-2^2", "-4"), ("(-2)^2", "4"), ("((1+2)*3)^2", "81"),
    ];
    for (expr, want) in cases {
        assert_eq!(eval(expr), want, "eval({:?})", expr);
    }
}

#[test]
fn parser_unary_operators() {
    let cases = [("-3+5", "2"), ("+3-1", "2"), ("5++3", "8"), ("5--3", "8"), ("--5", "5"), ("5-+3", "2")];
    for (expr, want) in cases {
        assert_eq!(eval(expr), want, "eval({:?})", expr);
    }
}

#[test]
fn parser_floor() {
    let cases = [("[3.7]", "3"), ("[-3.7]", "-4"), ("[5]", "5"), ("[-5]", "-5"), ("[0]", "0")];
    for (expr, want) in cases {
        assert_eq!(eval(expr), want, "eval({:?})", expr);
    }
}

#[test]
fn parser_absolute_value() {
    let cases = [("|-5|", "5"), ("|5|", "5"), ("|3-10|", "7"), ("|3-10|*2", "14")];
    for (expr, want) in cases {
        assert_eq!(eval(expr), want, "eval({:?})", expr);
    }
}

#[test]
fn parser_nesting_floor_absolute() {
    assert_eq!(eval("|[-7.5]|"), "8");
    assert_eq!(eval("[|-7.5|]"), "7");
}

#[test]
fn parser_big_integers() {
    assert_eq!(eval("123456789012345678901234567890*2"), "246913578024691357802469135780");
}

#[test]
fn parser_rational_literals() {
    assert_eq!(eval("0.{3}"), "0.{3}");
    assert_eq!(eval("100/7"), "14.{285714}");
    assert_eq!(eval("1/3+1/6"), "0.5");
}

#[test]
fn parser_modulo() {
    assert_eq!(eval("-10%3"), "-1");
    let lexer = test_lexer();
    let mut parser = Parser::<TightInteger>::new();
    let tokens = lexer.get_tokens("5%2.5").unwrap();
    assert!(parser.parse(tokens).is_err(), "modulo of a non-integer should be rejected");
}

#[test]
fn parser_integer_division() {
    let cases = [
        ("7//2", "3"), ("-7//2", "-3"), ("7//-2", "-3"), ("-7//-2", "3"),
        ("100//7", "14"),
        ("100//3//2", "16"), // left-associative
        ("2+3//2", "3"),     // // binds tighter than +
        ("2*3//2", "3"),     // same precedence as *, left to right
        ("(1+2)//2", "1"),
    ];
    for (expr, want) in cases {
        assert_eq!(eval(expr), want, "eval({:?})", expr);
    }
    // (a//b)*b + (a%b) == a, matching divide_by_and_modulo's quotient/remainder pair.
    assert_eq!(eval("(-7//2)*2+(-7%2)"), "-7");

    let lexer = test_lexer();
    let mut parser = Parser::<TightInteger>::new();
    let tokens = lexer.get_tokens("7.5//2").unwrap();
    assert!(parser.parse(tokens).is_err(), "integer division of a non-integer should be rejected");
    let mut parser = Parser::<TightInteger>::new();
    let tokens = lexer.get_tokens("5//0").unwrap();
    assert!(parser.parse(tokens).is_err(), "integer division by zero should be rejected");
}

#[test]
fn lexer_integer_division_slash_pairing() {
    let lexer = test_lexer();
    let tokens = lexer.get_tokens("///").unwrap();
    assert_eq!(tokens, vec![Token::IntegerDivide, Token::Divide], "odd run of '/' greedily pairs into // then a lone /");
    let tokens = lexer.get_tokens("////").unwrap();
    assert_eq!(tokens, vec![Token::IntegerDivide, Token::IntegerDivide], "even run of '/' greedily pairs into // tokens");
}

#[test]
fn parser_error_cases() {
    let lexer = test_lexer();
    let exprs = ["1/0", "5/(2-2)", "2^2.5", "2^99999999999999999999", "2+", "(1+2", "()", "2(3)", "[5", "|5", "2#3"];
    for expr in exprs {
        let mut parser = Parser::<TightInteger>::new();
        let outcome = lexer.get_tokens(expr).and_then(|tokens| parser.parse(tokens));
        assert!(outcome.is_err(), "expected an error evaluating {:?}", expr);
    }
    let mut parser = Parser::<TightInteger>::new();
    assert!(parser.parse(vec![]).is_err(), "empty token list should be rejected");
}

// Expression-depth limit: a deeply nested/chained expression must be
// rejected as a normal Err instead of crashing the whole process with an
// uncatchable "fatal runtime error: stack overflow" (not a panic -
// catch_unwind cannot intercept it). Regression test for a real DoS: any of
// these three independent recursion paths (bracket/floor/absolute nesting,
// chained unary +/-, chained right-associative ^) previously took down the
// whole interpreter - an abort, not even a panic - on a single malicious
// input line.
#[test]
fn parser_expression_depth_limit_rejects_deep_input() {
    let lexer = test_lexer();
    let deep_exprs = [
        format!("{}1{}", "(".repeat(10_000), ")".repeat(10_000)),
        format!("{}5", "-".repeat(10_000)),
        // Base 1 (not 2): 1^1^1^...^1 never produces an exponent outside
        // int range, so this exercises the depth guard itself rather than
        // coincidentally failing "exponent out of range" first.
        vec!["1"; 10_000].join("^"),
    ];
    for expr in &deep_exprs {
        let mut parser = Parser::<TightInteger>::new();
        let outcome = lexer.get_tokens(expr).and_then(|tokens| parser.parse(tokens));
        match outcome {
            Err(e) => assert!(e.to_string().contains("nested too deeply"), "expected a 'nested too deeply' error, got: {}", e),
            Ok(_) => panic!("expected an error for a 10,000-deep expression"),
        }
    }
}

#[test]
fn parser_expression_depth_limit_allows_moderate_nesting() {
    let expr = format!("{}1{}", "(".repeat(500), ")".repeat(500));
    assert_eq!(eval(&expr), "1", "nesting well under the depth limit should still work");
}

// ===================== round_to boundary panics (Rational only) =====================

#[test]
fn rational_round_to_min_int_panics() {
    // to_truncate/round/ceil/floor_decimal_string special-case i32::MIN,
    // since it has no positive counterpart to negate `round_to` into - "the
    // minimum representable precision" isn't representable, so it is
    // rejected outright rather than producing some arbitrary result. This is
    // the one place this type panics rather than returning Result: it's a
    // caller-contract violation (an absurd precision argument), not a
    // data-dependent failure the REPL needs to recover from mid-expression.
    // All three round_to-taking formatters panic for i32::MIN regardless of
    // the number's magnitude.
    assert_panics("truncate at i32::MIN panics", || {
        let _ = r_int(5).to_truncate_decimal_string(i32::MIN);
    });
    assert_panics("round at i32::MIN panics", || {
        let _ = r_int(5).to_round_decimal_string(i32::MIN);
    });
    assert_panics("ceil at i32::MIN panics", || {
        let _ = r_int(5).to_ceil_decimal_string(i32::MIN);
    });
}
