package decirational

import "testing"

func r(t *testing.T, n, den int64) Rational[DecimalInteger] {
	t.Helper()
	rat, err := NewRational(NewDecimalIntegerFromInt64(n), NewDecimalIntegerFromInt64(den))
	if err != nil {
		t.Fatalf("NewRational(%d,%d): %v", n, den, err)
	}
	return rat
}

func rInt(n int64) Rational[DecimalInteger] {
	return NewRationalFromInteger(NewDecimalIntegerFromInt64(n))
}

func TestRationalConstruction(t *testing.T) {
	if got := r(t, 4, 8).String(); got != "1/2" {
		t.Errorf("reduces to lowest terms: %s", got)
	}
	if got := r(t, 1, -2).String(); got != "-1/2" {
		t.Errorf("negative denominator normalizes sign: %s", got)
	}
	if got := r(t, -1, -2).String(); got != "1/2" {
		t.Errorf("double negative cancels: %s", got)
	}
	if got := r(t, 5, 1).String(); got != "5" {
		t.Errorf("denominator of one prints as plain integer: %s", got)
	}
	if _, err := NewRational(NewDecimalIntegerFromInt64(1), DecimalZero); err == nil {
		t.Error("zero denominator should be rejected")
	}

	if !r(t, 4, 4).IsInteger() {
		t.Error("4/4 reduces to an integer")
	}
	if r(t, 3, 4).IsInteger() {
		t.Error("3/4 is not an integer")
	}
	if !r(t, 0, 5).IsZero() {
		t.Error("0/5 is zero")
	}
	if !r(t, 3, 4).IsPositive() {
		t.Error("3/4 is positive")
	}
	if !r(t, -3, 4).IsNegative() {
		t.Error("-3/4 is negative")
	}
}

func TestRationalReciprocalAbsNegate(t *testing.T) {
	if recip := r(t, 3, 4).Reciprocal(); recip.String() != "4/3" {
		t.Errorf("reciprocal of 3/4: %v", recip)
	}
	if recipNeg := r(t, -3, 4).Reciprocal(); recipNeg.String() != "-4/3" {
		t.Errorf("reciprocal preserves sign: %v", recipNeg)
	}
	expectPanic(t, "reciprocal of zero should panic", func() { r(t, 0, 5).Reciprocal() })
	if got := r(t, -3, 4).Abs().String(); got != "3/4" {
		t.Errorf("abs of negative fraction: %s", got)
	}
	if got := r(t, 3, 4).Negate().String(); got != "-3/4" {
		t.Errorf("negate of positive fraction: %s", got)
	}
}

func mustParseRat(t *testing.T, s string) Rational[DecimalInteger] {
	t.Helper()
	rat, err := ParseRational[DecimalInteger](s, ParseDecimalInteger)
	if err != nil {
		t.Fatalf("ParseRational(%q): %v", s, err)
	}
	return rat
}

func TestRationalStringParsing(t *testing.T) {
	cases := []struct{ input, want string }{
		{"3/4", "3/4"}, {"-3/4", "-3/4"}, {"6/8", "3/4"}, {"5", "5"},
		{"0.5", "1/2"}, {"-0.5", "-1/2"}, {"0.25", "1/4"},
		{"0.{3}", "1/3"},
	}
	for _, c := range cases {
		if got := mustParseRat(t, c.input).String(); got != c.want {
			t.Errorf("parse(%q) = %q, want %q", c.input, got, c.want)
		}
	}
	if got := mustParseRat(t, "1.5{6}").ToFractionString(); got != "47/30" {
		t.Errorf("parse mixed finite+cyclic decimal: %s", got)
	}

	for _, bad := range []string{"", ".", "1.2.3", "1/2/3", "1/2.5", "5.", "0.{}", "1{3}", "1.2}"} {
		if _, err := ParseRational[DecimalInteger](bad, ParseDecimalInteger); err == nil {
			t.Errorf("expected error parsing %q", bad)
		}
	}
}

func TestRationalArithmetic(t *testing.T) {
	if got := r(t, 1, 2).Plus(r(t, 1, 3)).String(); got != "5/6" {
		t.Errorf("1/2+1/3 = %s", got)
	}
	if got := r(t, 1, 2).Minus(r(t, 1, 3)).String(); got != "1/6" {
		t.Errorf("1/2-1/3 = %s", got)
	}
	if got := r(t, 1, 2).Multiply(r(t, 1, 3)).String(); got != "1/6" {
		t.Errorf("1/2*1/3 = %s", got)
	}
	if div := r(t, 1, 2).DivideBy(r(t, 1, 3)); div.String() != "3/2" {
		t.Errorf("(1/2)/(1/3) = %v", div)
	}
	expectPanic(t, "divide by zero rational should panic", func() { r(t, 1, 2).DivideBy(r(t, 0, 5)) })
	if got := r(t, 1, 3).Plus(r(t, 2, 3)).String(); got != "1" {
		t.Errorf("1/3+2/3=1: %s", got)
	}
}

func TestRationalPow(t *testing.T) {
	if got := rInt(2).Pow(-3).String(); got != "1/8" {
		t.Errorf("2^-3 = %s", got)
	}
	if got := rInt(2).Pow(3).String(); got != "8" {
		t.Errorf("2^3 = %s", got)
	}
	if got := rInt(9).Pow(0).String(); got != "1" {
		t.Errorf("x^0=1: %s", got)
	}
	if got := rInt(1).Pow(-2147483648).String(); got != "1" {
		t.Errorf("1^MIN_VALUE=1: %s", got)
	}
	if got := rInt(-1).Pow(-2147483648).String(); got != "1" {
		t.Errorf("(-1)^MIN_VALUE=1 (even exponent): %s", got)
	}
	if got := rInt(-1).Pow(-2147483648 + 1).String(); got != "-1" {
		t.Errorf("(-1)^(MIN_VALUE+1)=-1 (odd exponent): %s", got)
	}
	expectPanic(t, "zero to negative power rejected", func() { r(t, 0, 5).Pow(-1) })
}

func TestRationalCompareEquals(t *testing.T) {
	if r(t, 1, 2).Compare(r(t, 2, 3)) >= 0 {
		t.Error("1/2 < 2/3")
	}
	if r(t, 2, 3).Compare(r(t, 1, 2)) <= 0 {
		t.Error("2/3 > 1/2")
	}
	if r(t, 1, 2).Compare(r(t, 2, 4)) != 0 {
		t.Error("1/2 == 2/4")
	}
	if !r(t, 1, 2).Equals(r(t, 2, 4)) {
		t.Error("equal fractions from different literals are equal")
	}
	if r(t, 1, 2).Equals(r(t, 1, 3)) {
		t.Error("different fractions are not equal")
	}
}

func TestRationalDecimalString(t *testing.T) {
	cases := []struct {
		n, d int64
		want string
	}{
		{1, 2, "0.5"}, {5, 4, "1.25"}, {1, 3, "0.{3}"}, {1, 6, "0.1{6}"},
		{1, 7, "0.{142857}"}, {22, 7, "3.{142857}"}, {-1, 3, "-0.{3}"}, {0, 1, "0"},
	}
	for _, c := range cases {
		if got := r(t, c.n, c.d).ToDecimalString(); got != c.want {
			t.Errorf("%d/%d -> %s, want %s", c.n, c.d, got, c.want)
		}
	}
}

func TestRationalTruncateRoundCeilFloor(t *testing.T) {
	if got := r(t, 10, 3).ToTruncateDecimalString(2); got != "3.33" {
		t.Errorf("10/3 truncate(2) = %s", got)
	}
	if got := r(t, -10, 3).ToTruncateDecimalString(2); got != "-3.33" {
		t.Errorf("-10/3 truncate(2) = %s", got)
	}
	if got := rInt(1234).ToTruncateDecimalString(-2); got != "1200" {
		t.Errorf("1234 truncate(-2) = %s", got)
	}
	if got := rInt(50).ToTruncateDecimalString(-3); got != "0" {
		t.Errorf("50 truncate(-3) rounds down to 0 magnitude: %s", got)
	}

	if got := r(t, 1, 3).ToRoundDecimalString(2); got != "0.33" {
		t.Errorf("1/3 round(2) = %s", got)
	}
	if got := r(t, 2, 3).ToRoundDecimalString(2); got != "0.67" {
		t.Errorf("2/3 round(2) = %s", got)
	}
	if got := r(t, 1, 2).ToRoundDecimalString(0); got != "1" {
		t.Errorf("1/2 rounds up to 1 (half-up): %s", got)
	}
	if got := r(t, -1, 2).ToRoundDecimalString(0); got != "-1" {
		t.Errorf("-1/2 rounds to -1: %s", got)
	}

	if got := r(t, 1, 3).ToCeilDecimalString(0); got != "1" {
		t.Errorf("ceil(1/3)=1: %s", got)
	}
	if got := r(t, 1, 3).ToFloorDecimalString(0); got != "0" {
		t.Errorf("floor(1/3)=0: %s", got)
	}
	if got := r(t, -1, 3).ToFloorDecimalString(0); got != "-1" {
		t.Errorf("floor(-1/3)=-1: %s", got)
	}
	if got := r(t, 5, 2).ToCeilDecimalString(0); got != "3" {
		t.Errorf("ceil(5/2)=3: %s", got)
	}
	if got := r(t, -5, 2).ToCeilDecimalString(0); got != "-2" {
		t.Errorf("ceil(-5/2)=-2: %s", got)
	}
	if got := r(t, -5, 2).ToFloorDecimalString(0); got != "-3" {
		t.Errorf("floor(-5/2)=-3: %s", got)
	}
	if got := rInt(0).ToCeilDecimalString(0); got != "0" {
		t.Errorf("ceil(0)=0: %s", got)
	}
	if got := rInt(0).ToFloorDecimalString(0); got != "0" {
		t.Errorf("floor(0)=0: %s", got)
	}
	// KNOWN BUG (present in the Java original and preserved here): ceil() of a
	// negative non-integer whose floor()-of-negation is exactly zero prints
	// "-0" instead of "0", because ToCeilDecimalString prepends '-'
	// unconditionally for negative inputs, even when the magnitude truncates
	// to "0". Documented rather than silently asserted as correct.
	if got := r(t, -1, 3).ToCeilDecimalString(0); got != "-0" {
		t.Errorf("BUG marker changed: ceil(-1/3) = %s, expected the known -0 bug", got)
	}
}

func TestRationalMixedString(t *testing.T) {
	cases := []struct {
		n, d int64
		want string
	}{
		{7, 4, "1 3/4"}, {-7, 4, "-1 3/4"}, {3, 4, "3/4"}, {-3, 4, "-3/4"},
		{5, 1, "5"}, {-5, 1, "-5"}, {0, 1, "0"}, {5, 2, "2 1/2"},
	}
	for _, c := range cases {
		if got := r(t, c.n, c.d).ToMixedString(); got != c.want {
			t.Errorf("mixed(%d/%d) = %s, want %s", c.n, c.d, got, c.want)
		}
	}
}
