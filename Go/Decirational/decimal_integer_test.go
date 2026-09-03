package decirational

import "testing"

func mustD(t *testing.T, s string) DecimalInteger {
	t.Helper()
	v, err := ParseDecimalInteger(s)
	if err != nil {
		t.Fatalf("ParseDecimalInteger(%q): %v", s, err)
	}
	return v
}

func expectPanic(t *testing.T, desc string, fn func()) {
	t.Helper()
	defer func() {
		if recover() == nil {
			t.Errorf("%s: expected a panic", desc)
		}
	}()
	fn()
}

// Construction / String
func TestDecimalIntegerConstruction(t *testing.T) {
	cases := []struct{ input, want string }{
		{"123", "123"}, {"-123", "-123"}, {"+123", "123"}, {"000123", "123"},
		{"0", "0"}, {"-0", "0"}, {"123456789012345678901234567890", "123456789012345678901234567890"},
	}
	for _, c := range cases {
		if got := mustD(t, c.input).String(); got != c.want {
			t.Errorf("parse(%q) = %q, want %q", c.input, got, c.want)
		}
	}
	if NewDecimalIntegerFromInt64(0).String() != "0" {
		t.Error("int64 constructor zero")
	}
	if NewDecimalIntegerFromInt64(42).String() != "42" {
		t.Error("int64 constructor positive")
	}
	if NewDecimalIntegerFromInt64(-42).String() != "-42" {
		t.Error("int64 constructor negative")
	}
	if !DecimalZero.Equals(mustD(t, "0")) {
		t.Error("DecimalZero equals parsed zero")
	}
	if !DecimalOne.Equals(mustD(t, "1")) {
		t.Error("DecimalOne equals parsed one")
	}
	if seven, err := GetDecimalDigit(7); err != nil || !seven.Equals(mustD(t, "7")) {
		t.Error("GetDecimalDigit(7) matches parsed 7")
	}

	for _, bad := range []string{"", "-", "12a", "1.5"} {
		if _, err := ParseDecimalInteger(bad); err == nil {
			t.Errorf("expected error parsing %q", bad)
		}
	}
	if _, err := GetDecimalDigit(10); err == nil {
		t.Error("GetDecimalDigit(10) should be rejected")
	}
	if _, err := NewDecimalInteger(nil, false); err == nil {
		t.Error("NewDecimalInteger with empty digits should be rejected")
	}
	if _, err := NewDecimalInteger([]byte{1, 10, 2}, false); err == nil {
		t.Error("NewDecimalInteger with an out-of-range digit should be rejected")
	}
}

func TestDecimalIntegerPredicates(t *testing.T) {
	if !mustD(t, "0").IsZero() || mustD(t, "1").IsZero() {
		t.Error("IsZero")
	}
	if !mustD(t, "1").IsOne() || mustD(t, "-1").IsOne() || mustD(t, "2").IsOne() {
		t.Error("IsOne")
	}
	if !mustD(t, "1").IsUnitAbs() || !mustD(t, "-1").IsUnitAbs() || mustD(t, "2").IsUnitAbs() {
		t.Error("IsUnitAbs")
	}
	if !mustD(t, "5").IsPositive() || mustD(t, "0").IsPositive() || mustD(t, "-5").IsPositive() {
		t.Error("IsPositive")
	}
	if !mustD(t, "-5").IsNegative() || mustD(t, "0").IsNegative() || mustD(t, "5").IsNegative() {
		t.Error("IsNegative")
	}
}

func TestDecimalIntegerNegateAbs(t *testing.T) {
	if got := mustD(t, "5").Negate().String(); got != "-5" {
		t.Errorf("negate positive: %s", got)
	}
	if got := mustD(t, "-5").Negate().String(); got != "5" {
		t.Errorf("negate negative: %s", got)
	}
	if got := mustD(t, "0").Negate().String(); got != "0" {
		t.Errorf("negate zero stays zero: %s", got)
	}
	if got := mustD(t, "-5").Abs().String(); got != "5" {
		t.Errorf("abs of negative: %s", got)
	}
}

func TestDecimalIntegerCompare(t *testing.T) {
	cases := []struct {
		a, b string
		want int // -1, 0, or 1 (sign only compared)
	}{
		{"5", "3", 1}, {"3", "5", -1}, {"5", "5", 0},
		{"-5", "3", -1}, {"-5", "-3", -1}, {"-3", "-5", 1},
		{"100", "99", 1}, {"-100", "-99", -1},
	}
	for _, c := range cases {
		got := mustD(t, c.a).Compare(mustD(t, c.b))
		if sign(got) != c.want {
			t.Errorf("compare(%s,%s) = %d, want sign %d", c.a, c.b, got, c.want)
		}
	}
}

func sign(n int) int {
	if n > 0 {
		return 1
	} else if n < 0 {
		return -1
	}
	return 0
}

func TestDecimalIntegerEquals(t *testing.T) {
	if !mustD(t, "007").Equals(NewDecimalIntegerFromInt64(7)) {
		t.Error("equal values from different literals are equal")
	}
	if mustD(t, "5").Equals(mustD(t, "-5")) {
		t.Error("5 should not equal -5")
	}
}

func TestDecimalIntegerPlusMinus(t *testing.T) {
	cases := []struct{ a, b, want string }{
		{"123", "456", "579"}, {"-5", "3", "-2"}, {"5", "-3", "2"}, {"-5", "-3", "-8"},
		{"0", "7", "7"}, {"7", "0", "7"}, {"5", "-5", "0"},
	}
	for _, c := range cases {
		if got := mustD(t, c.a).Plus(mustD(t, c.b)).String(); got != c.want {
			t.Errorf("%s+%s = %s, want %s", c.a, c.b, got, c.want)
		}
	}
	minusCases := []struct{ a, b, want string }{
		{"5", "3", "2"}, {"3", "5", "-2"}, {"-5", "-3", "-2"}, {"5", "-3", "8"}, {"0", "7", "-7"}, {"7", "0", "7"},
	}
	for _, c := range minusCases {
		if got := mustD(t, c.a).Minus(mustD(t, c.b)).String(); got != c.want {
			t.Errorf("%s-%s = %s, want %s", c.a, c.b, got, c.want)
		}
	}
}

func TestDecimalIntegerMultiply(t *testing.T) {
	cases := []struct{ a, b, want string }{
		{"12", "12", "144"}, {"-3", "4", "-12"}, {"-3", "-4", "12"}, {"0", "999", "0"},
		{"1", "999", "999"}, {"-1", "999", "-999"}, {"999", "-1", "-999"},
	}
	for _, c := range cases {
		if got := mustD(t, c.a).Multiply(mustD(t, c.b)).String(); got != c.want {
			t.Errorf("%s*%s = %s, want %s", c.a, c.b, got, c.want)
		}
	}
}

func TestDecimalIntegerMultiplyDivideBase(t *testing.T) {
	if got := mustD(t, "12").MultiplyBase(2).String(); got != "1200" {
		t.Errorf("multiply_base by 2: %s", got)
	}
	if got := mustD(t, "12").MultiplyBase(0).String(); got != "12" {
		t.Errorf("multiply_base by 0 is identity: %s", got)
	}
	if got := mustD(t, "-12").MultiplyBase(2).String(); got != "-1200" {
		t.Errorf("multiply_base preserves sign: %s", got)
	}
	if got := mustD(t, "1234").DivideByBase(2).String(); got != "12" {
		t.Errorf("divide_by_base by 2: %s", got)
	}
	if got := mustD(t, "1234").DivideByBase(0).String(); got != "1234" {
		t.Errorf("divide_by_base by 0 is identity: %s", got)
	}
	if got := mustD(t, "1234").DivideByBase(10).String(); got != "0" {
		t.Errorf("divide_by_base beyond length is zero: %s", got)
	}
	expectPanic(t, "multiply_base rejects negative times", func() { mustD(t, "5").MultiplyBase(-1) })
	expectPanic(t, "divide_by_base rejects negative times", func() { mustD(t, "5").DivideByBase(-1) })
}

func TestDecimalIntegerDivideModulo(t *testing.T) {
	cases := []struct{ a, b, want string }{
		{"144", "12", "12"}, {"-144", "12", "-12"}, {"7", "2", "3"}, {"-7", "2", "-3"},
	}
	for _, c := range cases {
		if got := mustD(t, c.a).DivideBy(mustD(t, c.b)).String(); got != c.want {
			t.Errorf("%s/%s = %s, want %s", c.a, c.b, got, c.want)
		}
	}
	moduloCases := []struct{ a, b, want string }{{"7", "2", "1"}, {"-7", "2", "-1"}}
	for _, c := range moduloCases {
		if got := mustD(t, c.a).Modulo(mustD(t, c.b)).String(); got != c.want {
			t.Errorf("%s%%%s = %s, want %s", c.a, c.b, got, c.want)
		}
	}
	expectPanic(t, "divide by zero panics", func() { mustD(t, "5").DivideBy(DecimalZero) })
	expectPanic(t, "modulo by zero panics", func() { mustD(t, "5").Modulo(DecimalZero) })
}

func verifyDecimalDivisionIdentity(t *testing.T, aStr, bStr string) {
	t.Helper()
	a, b := mustD(t, aStr), mustD(t, bStr)
	qr := a.DivideByAndModulo(b)
	reconstructed := qr[0].Multiply(b).Plus(qr[1])
	if !reconstructed.Equals(a) {
		t.Errorf("(%s)/(%s): q*b+r = %s, want %s", aStr, bStr, reconstructed, a)
	}
	if !qr[0].Equals(a.DivideBy(b)) {
		t.Errorf("(%s)/(%s): quotient mismatch with DivideBy", aStr, bStr)
	}
	if !qr[1].Equals(a.Modulo(b)) {
		t.Errorf("(%s)/(%s): remainder mismatch with Modulo", aStr, bStr)
	}
}

func TestDecimalIntegerDivisionIdentity(t *testing.T) {
	pairs := [][2]string{{"17", "5"}, {"-17", "5"}, {"17", "-5"}, {"-17", "-5"}, {"100", "7"}, {"0", "5"}, {"999999999999999999999", "37"}, {"1", "1"}}
	for _, p := range pairs {
		verifyDecimalDivisionIdentity(t, p[0], p[1])
	}
}

func TestDecimalIntegerGcdLcm(t *testing.T) {
	cases := []struct{ a, b, want string }{
		{"48", "18", "6"}, {"17", "5", "1"}, {"100", "75", "25"}, {"0", "5", "5"}, {"5", "0", "5"}, {"-12", "18", "6"},
	}
	for _, c := range cases {
		if got := mustD(t, c.a).Gcd(mustD(t, c.b)).String(); got != c.want {
			t.Errorf("gcd(%s,%s) = %s, want %s", c.a, c.b, got, c.want)
		}
	}
	if got := mustD(t, "21").Lcm(mustD(t, "6")).String(); got != "42" {
		t.Errorf("lcm(21,6) = %s", got)
	}
	if got := mustD(t, "0").Lcm(mustD(t, "5")).String(); got != "0" {
		t.Errorf("lcm(0,x) = %s", got)
	}
}

func TestDecimalIntegerPow(t *testing.T) {
	cases := []struct {
		base string
		exp  int32
		want string
	}{
		{"2", 10, "1024"}, {"5", 0, "1"}, {"0", 0, "1"}, {"5", 1, "5"}, {"-2", 3, "-8"}, {"-2", 2, "4"},
	}
	for _, c := range cases {
		if got := mustD(t, c.base).Pow(c.exp).String(); got != c.want {
			t.Errorf("%s^%d = %s, want %s", c.base, c.exp, got, c.want)
		}
	}
	expectPanic(t, "negative exponent rejected", func() { mustD(t, "2").Pow(-1) })
}
